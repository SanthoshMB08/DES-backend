package com.evaluation.prototype.admin.dto;

public record ResetPasswordResponse(String username, String temporaryPassword, boolean forcePasswordChange) {
}
