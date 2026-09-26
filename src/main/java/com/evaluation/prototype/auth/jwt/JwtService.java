package com.evaluation.prototype.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Pattern SUBJECT_PATTERN = Pattern.compile("\"sub\":\"([^\"]+)\"");
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("\"exp\":(\\d+)");

    private final String secret;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.secret = secret;
        this.expirationSeconds = expirationMinutes * 60;
    }

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        String roles = roleJson(userDetails.getAuthorities());
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = """
                {"sub":"%s","roles":%s,"iat":%d,"exp":%d}
                """.formatted(
                userDetails.getUsername(),
                roles,
                now.getEpochSecond(),
                now.plusSeconds(expirationSeconds).getEpochSecond()).trim();

        String encodedHeader = base64Url(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        String unsignedToken = encodedHeader + "." + encodedPayload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    public String extractUsername(String token) {
        String payload = decodePayload(token);
        Matcher matcher = SUBJECT_PATTERN.matcher(payload);
        return matcher.find() ? matcher.group(1) : null;
    }

    public boolean isValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username != null
                && username.equals(userDetails.getUsername())
                && !isExpired(token)
                && signatureMatches(token);
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private boolean isExpired(String token) {
        String payload = decodePayload(token);
        Matcher matcher = EXPIRY_PATTERN.matcher(payload);
        if (!matcher.find()) {
            return true;
        }
        return Instant.now().getEpochSecond() >= Long.parseLong(matcher.group(1));
    }

    private boolean signatureMatches(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        // MessageDigest.isEqual runs in constant time regardless of where the first
        // mismatched byte is - String.equals() short-circuits on the first difference,
        // which leaks timing information an attacker could use to guess the signature
        // one byte at a time. This matters here because the signature is exactly the
        // secret we're trying to protect.
        byte[] expected = sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8);
        byte[] provided = parts[2].getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(expected, provided);
    }

    private String decodePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return "";
        }
        try {
            return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException malformedBase64) {
            // Not valid base64 - treat exactly like "couldn't parse this token" rather
            // than letting the exception propagate out of the security filter chain,
            // which would otherwise surface as an ugly unhandled 500 instead of a
            // clean "not authenticated".
            return "";
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String roleJson(Collection<? extends GrantedAuthority> authorities) {
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> "\"" + role + "\"")
                .toList();
        return "[" + String.join(",", roles) + "]";
    }
}
