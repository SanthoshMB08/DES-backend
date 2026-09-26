package com.evaluation.prototype.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.evaluation.prototype.common.exception.ApiException;

/**
 * Local-disk storage for scanned answer-booklet PDFs. Each script has exactly one
 * file, keyed by its id, that gets overwritten in place whenever a new version is
 * uploaded (e.g. the scanner's original, later replaced by the evaluator's
 * annotated copy). Nothing is ever served as a forced download - callers get the
 * raw bytes back to render inline.
 */
@Service
public class FileStorageService {

    private final Path baseDir;

    public FileStorageService(@Value("${app.storage.dir:./data/booklets}") String dir) {
        this.baseDir = Path.of(dir);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create storage directory: " + dir, e);
        }
    }

    private Path pathFor(UUID scriptId) {
        return baseDir.resolve(scriptId + ".pdf");
    }

    private static final byte[] PDF_MAGIC_BYTES = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    public void store(UUID scriptId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No file was uploaded");
        }
        if (!looksLikePdf(file)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF files are accepted for answer scripts");
        }
        try {
            file.transferTo(pathFor(scriptId));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save the uploaded file");
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
            return java.util.Arrays.equals(header, PDF_MAGIC_BYTES);
        } catch (IOException e) {
            return false;
        }
    }

    public byte[] read(UUID scriptId) {
        Path path = pathFor(scriptId);
        if (!Files.exists(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No scanned file has been uploaded for this script yet");
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read the stored file");
        }
    }

    public boolean exists(UUID scriptId) {
        return Files.exists(pathFor(scriptId));
    }
}
