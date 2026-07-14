package com.dbwb.platform.common.dto;

/**
 * Uniform response envelope for all API endpoints. Keeping this consistent
 * lets the Next.js frontend use one shared response-handling helper instead
 * of parsing a different shape per endpoint.
 */
public record ApiResponse<T>(boolean success, T data, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
