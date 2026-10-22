package com.evaluation.prototype.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriBuilder;

import com.evaluation.prototype.common.exception.ApiException;

/**
 * Supabase Storage for scanned answer-booklet PDFs. Originals and evaluator
 * corrections are stored separately under their QP-code folders. The legacy
 * script-id object remains readable for papers uploaded before this layout.
 *
 * Requests are made with the service_role key, which bypasses RLS, so this service
 * is the only door to the bucket. Keep the bucket private: these are exam papers.
 * The objects are never handed out as public or signed URLs - every read goes
 * through the caller's existing role check in ValuationService.
 */
@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private static final byte[] PDF_MAGIC_BYTES = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    /** Mirrors spring.servlet.multipart.max-file-size so the bucket rejects nothing the API would accept. */
    private static final long MAX_FILE_BYTES = 30L * 1024 * 1024;

    private static final String NO_FILE_MESSAGE = "No scanned file has been uploaded for this script yet";

    private final RestClient client;
    private final String bucket;
    private final boolean configured;
    private final AtomicBoolean bucketReady = new AtomicBoolean();

    public SupabaseStorageService(
            RestClient.Builder restClientBuilder,
            @Value("${app.storage.supabase.url:}") String url,
            @Value("${app.storage.supabase.service-role-key:}") String serviceRoleKey,
            @Value("${app.storage.supabase.bucket:scripts}") String bucket) {
        this.bucket = bucket;
        this.configured = !url.isBlank() && !serviceRoleKey.isBlank();
        if (configured) {
            this.client = restClientBuilder
                    .baseUrl(stripTrailingSlash(url))
                    .defaultHeader("apikey", serviceRoleKey)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                    .build();
        } else {
            this.client = null;
            // Not fatal: the rest of the app is unaffected, but nothing can be scanned.
            log.warn("Supabase Storage is not configured - set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY. "
                    + "Script PDFs cannot be uploaded or read until then.");
        }

    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private java.net.URI storageUri(UriBuilder uriBuilder, String endpoint, String path) {
        return uriBuilder.path("/storage/v1/")
                .path(endpoint)
                .path("/")
                .path(bucket)
                .path("/")
                .path(path)
                .build();
    }

    private String legacyObjectPath(UUID scriptId) {
        return scriptId + ".pdf";
    }

    public static String objectPath(String qpCode, String folder, UUID scriptId) {
        String safeQpCode = qpCode == null ? "unassigned" : qpCode.trim()
                .replaceAll("[^A-Za-z0-9._-]", "_");
        if (safeQpCode.isBlank() || ".".equals(safeQpCode) || "..".equals(safeQpCode)) {
            safeQpCode = "unassigned";
        }
        return safeQpCode + "/" + folder + "/" + scriptId + ".pdf";
    }

    private String originalObjectPath(String qpCode, UUID scriptId) {
        return objectPath(qpCode, "qp-original", scriptId);
    }

    private String correctedObjectPath(String qpCode, UUID scriptId) {
        return objectPath(qpCode, "qp-after-correction", scriptId);
    }

    /** Keeps the former storage API available for legacy callers. */
    public void store(UUID scriptId, MultipartFile file) {
        storeObject(legacyObjectPath(scriptId), scriptId, file);
    }

    public void storeOriginal(UUID scriptId, String qpCode, MultipartFile file) {
        storeObject(originalObjectPath(qpCode, scriptId), scriptId, file);
    }

    public void storeCorrected(UUID scriptId, String qpCode, MultipartFile file) {
        storeObject(correctedObjectPath(qpCode, scriptId), scriptId, file);
    }

    private void storeObject(String path, UUID scriptId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No file was uploaded");
        }
        if (!looksLikePdf(file)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF files are accepted for answer scripts");
        }
        requireConfigured();
        ensureBucket();

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }

        try {
            client.post()
                    .uri(uriBuilder -> storageUri(uriBuilder, "object", path))
                    .contentType(MediaType.APPLICATION_PDF)
                    // Without this the Storage API answers 400 "Asset Already Exists" when the
                    // evaluator re-uploads their annotated copy over the scanner's original.
                    .header("x-upsert", "true")
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("Supabase upload rejected for script {} ({}): {}", scriptId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save the uploaded file");
        } catch (RestClientException e) {
            log.error("Supabase Storage unreachable while uploading script {}", scriptId, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save the uploaded file");
        }
    }

    /**
     * Returns the currently stored PDF bytes. Deliberately sends no Accept header:
     * Supabase Storage answers application/json requests with object metadata
     * instead of the file itself.
     */
    public byte[] read(UUID scriptId) {
        return readObject(legacyObjectPath(scriptId), scriptId);
    }

    public byte[] readOriginal(UUID scriptId, String qpCode) {
        try {
            return readObject(originalObjectPath(qpCode, scriptId), scriptId);
        } catch (ApiException e) {
            if (e.getStatus() != HttpStatus.NOT_FOUND) {
                throw e;
            }
            return readObject(legacyObjectPath(scriptId), scriptId);
        }
    }

    public byte[] readCorrected(UUID scriptId, String qpCode) {
        return readObject(correctedObjectPath(qpCode, scriptId), scriptId);
    }

    private byte[] readObject(String path, UUID scriptId) {
        requireConfigured();
        byte[] bytes;
        try {
            bytes = client.get()
                    .uri(uriBuilder -> storageUri(uriBuilder, "object", path))
                    .retrieve()
                    .body(byte[].class);
        } catch (HttpStatusCodeException e) {
            if (isMissingObject(e)) {
                throw new ApiException(HttpStatus.NOT_FOUND, NO_FILE_MESSAGE);
            }
            log.error("Supabase download rejected for script {} ({}): {}", scriptId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read the stored file");
        } catch (RestClientException e) {
            log.error("Supabase Storage unreachable while reading script {}", scriptId, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read the stored file");
        }
        if (bytes == null || bytes.length == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, NO_FILE_MESSAGE);
        }
        return bytes;
    }

    public boolean exists(UUID scriptId) {
        return existsObject(legacyObjectPath(scriptId), scriptId);
    }

    public boolean originalExists(UUID scriptId, String qpCode) {
        return existsObject(originalObjectPath(qpCode, scriptId), scriptId)
                || existsObject(legacyObjectPath(scriptId), scriptId);
    }

    public boolean correctedExists(UUID scriptId, String qpCode) {
        return existsObject(correctedObjectPath(qpCode, scriptId), scriptId);
    }

    private boolean existsObject(String path, UUID scriptId) {
        requireConfigured();
        try {
            client.get()
                    .uri(uriBuilder -> storageUri(uriBuilder, "object/info", path))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpStatusCodeException e) {
            if (isMissingObject(e)) {
                return false;
            }
            log.error("Supabase metadata lookup failed for script {} ({}): {}", scriptId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read the stored file");
        } catch (RestClientException e) {
            log.error("Supabase Storage unreachable while checking script {}", scriptId, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read the stored file");
        }
    }

    /**
     * Checks the file actually starts with the PDF magic bytes ("%PDF-"), rather than
     * trusting the client-supplied filename/content-type header, both of which are
     * trivially spoofable (e.g. renaming a .exe to "script.pdf").
     */
    private boolean looksLikePdf(MultipartFile file) {
        try (var in = file.getInputStream()) {
            byte[] header = in.readNBytes(PDF_MAGIC_BYTES.length);
            return Arrays.equals(header, PDF_MAGIC_BYTES);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Storage reports a missing object as 404, but some deployments answer 400 with
     * an "Object not found" body instead - both mean "never uploaded".
     */
    private static boolean isMissingObject(HttpStatusCodeException e) {
        if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
            return true;
        }
        String body = e.getResponseBodyAsString();
        return body != null && (body.contains("\"error\":\"not_found\"") || body.contains("\"statusCode\":\"404\""));
    }

    private void requireConfigured() {
        if (!configured) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Script file storage is not configured on this server (SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY)");
        }
    }

    /**
     * Creates the bucket on first write so a fresh project works without a manual
     * dashboard step. The result is cached for the life of the process, and the
     * bucket is always private and PDF-only.
     */
    private void ensureBucket() {
        if (bucketReady.get()) {
            return;
        }
        boolean present;
        try {
            present = Boolean.TRUE.equals(client.get()
                    .uri("/storage/v1/bucket/{bucket}", bucket)
                    .exchange((request, response) -> response.getStatusCode().is2xxSuccessful()));
        } catch (RestClientException e) {
            log.error("Could not reach Supabase Storage to check bucket '{}'", bucket, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Supabase Storage is unreachable");
        }
        if (!present) {
            log.info("Supabase Storage bucket '{}' not found - creating it as private", bucket);
            try {
                client.post()
                        .uri("/storage/v1/bucket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "id", bucket,
                                "name", bucket,
                                "public", false,
                                "file_size_limit", MAX_FILE_BYTES,
                                "allowed_mime_types", List.of(MediaType.APPLICATION_PDF_VALUE)))
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientException e) {
                log.error("Could not create Supabase Storage bucket '{}'", bucket, e);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Supabase Storage bucket '" + bucket + "' does not exist and could not be created");
            }
        }
        bucketReady.set(true);
    }
}
