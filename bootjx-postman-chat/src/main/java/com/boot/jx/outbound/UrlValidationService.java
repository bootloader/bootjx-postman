package com.boot.jx.outbound;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.boot.utils.ArgUtil;

/**
 * Service for validating remote file URLs without downloading the entire file.
 * Uses HEAD request for efficiency with GET Range fallback for compatibility.
 * 
 * This service is specifically designed for outbound message validation
 * and is located within the chat module for optimal architecture.
 */
@Service
public class UrlValidationService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UrlValidationService.class);
    
    // Timeout constants for efficient validation
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;  // 3 seconds
    private static final int DEFAULT_READ_TIMEOUT_MS = 5000;     // 5 seconds
    
    private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
    private int readTimeoutMs = DEFAULT_READ_TIMEOUT_MS;
    
    /**
     * Validates if a remote file exists and is accessible.
     * Uses HEAD request first (most efficient), falls back to GET with Range header.
     *
     * @param fileUrl The URL to validate
     * @return FileValidationResult containing validation status and error details
     */
    public FileValidationResult validateFileWithImmediateResponse(String fileUrl) {
        if (!ArgUtil.is(fileUrl)) {
            return FileValidationResult.invalid("URL is required");
        }

        // Basic URL format validation using Java's URL constructor
        if (!isValidUrlFormat(fileUrl)) {
            return FileValidationResult.invalid("Invalid URL format");
        }

        try {
            // First attempt: HEAD request (most efficient)
            FileValidationResult headRes = validateWithHead(fileUrl);
            if (headRes.isValid()) {
                LOGGER.debug("URL validation successful via HEAD: {} - Response time: {}ms",
                    fileUrl, headRes.getResponseTime());
                return headRes;
            }

            // Fallback: GET with Range header (downloads only first byte)
            FileValidationResult rangeRes = validateWithRange(fileUrl);
            if (rangeRes.isValid()) {
                LOGGER.debug("URL validation successful via Range GET: {} - Response time: {}ms",
                    fileUrl, rangeRes.getResponseTime());
                return rangeRes;
            }

            // Both methods failed - return user-friendly error
            LOGGER.warn("URL validation failed for both methods: {} - HEAD: {}, Range: {}",
                fileUrl, headRes.getErrorMessage(), rangeRes.getErrorMessage());
            
            // Return user-friendly message instead of technical details
            String userMsg = getUserFriendlyErrorMessage(headRes, rangeRes);
            return FileValidationResult.invalid(userMsg);

        } catch (Exception e) {
            LOGGER.error("Unexpected error during URL validation: {}", fileUrl, e);
            return FileValidationResult.invalid("Unable to validate URL");
        }
    }
    
    /**
     * Sets custom timeout values for URL validation.
     * 
     * @param connectTimeoutMs Connection timeout in milliseconds
     * @param readTimeoutMs Read timeout in milliseconds
     */
    public void setTimeouts(int connectTimeoutMs, int readTimeoutMs) {
        this.connectTimeoutMs = Math.max(1000, connectTimeoutMs);  // Minimum 1 second
        this.readTimeoutMs = Math.max(2000, readTimeoutMs);        // Minimum 2 seconds
    }
    
    /**
     * Resets timeout values to defaults.
     */
    public void resetTimeouts() {
        this.connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
        this.readTimeoutMs = DEFAULT_READ_TIMEOUT_MS;
    }
    
    /**
     * Basic URL format validation using Java's URL constructor.
     * More reliable than regex pattern matching.
     * 
     * @param url The URL to validate
     * @return true if URL format is valid, false otherwise
     */
    private boolean isValidUrlFormat(String url) {
        try {
            // Use Java's URL constructor for comprehensive validation
            new URL(url);
            return true;
        } catch (Exception e) {
            LOGGER.debug("URL format validation failed for: {} - {}", url, e.getMessage());
            return false;
        }
    }
    
    /**
     * Generic HTTP validation method to reduce code duplication.
     * 
     * @param fileUrl The URL to validate
     * @param method The HTTP method (HEAD, GET, etc.)
     * @param headers Additional headers to set
     * @param successCodes Array of HTTP response codes considered successful
     * @param methodName Human-readable method name for error messages
     * @return FileValidationResult containing validation status and response time
     */
    private FileValidationResult validateHttpRequest(String fileUrl, String method, 
            Map<String, String> headers, int[] successCodes, String methodName) {
        HttpURLConnection connection = null;
        long startTime = System.currentTimeMillis();
        
        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setInstanceFollowRedirects(true);
            
            // Set additional headers if provided
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            
            // Check if response code is in success codes array
            for (int successCode : successCodes) {
                if (responseCode == successCode) {
                    long responseTime = System.currentTimeMillis() - startTime;
                    return FileValidationResult.valid(responseTime);
                }
            }
            
            // If not successful, return error with response time
            long responseTime = System.currentTimeMillis() - startTime;
            return FileValidationResult.invalid(
                String.format("HTTP %d: %s", responseCode, responseMessage), responseTime
            );
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return FileValidationResult.invalid(methodName + " request failed: " + e.getMessage(), responseTime);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Validates URL using HEAD request (most efficient method).
     * Only retrieves headers, no file content is transferred.
     */
    private FileValidationResult validateWithHead(String fileUrl) {
        return validateHttpRequest(fileUrl, "HEAD", null, 
            new int[]{HttpURLConnection.HTTP_OK}, "HEAD");
    }
    
    /**
     * Validates URL using GET request with Range header (fallback method).
     * Downloads only the first byte to verify file accessibility.
     */
    private FileValidationResult validateWithRange(String fileUrl) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Range", "bytes=0-0");

        return validateHttpRequest(fileUrl, "GET", headers,
            new int[]{HttpURLConnection.HTTP_PARTIAL, HttpURLConnection.HTTP_OK}, "Range");
    }

    /**
     * Maps HTTP response codes to user-friendly error messages.
     * Provides specific guidance on what the user should do.
     *
     * @param headResult Result from HEAD request
     * @param rangeResult Result from Range GET request
     * @return User-friendly error message with actionable guidance
     */
    private String getUserFriendlyErrorMessage(FileValidationResult headResult, FileValidationResult rangeResult) {
        String headErr = headResult.getErrorMessage();
        String rangeErr = rangeResult.getErrorMessage();
        
        if (headErr.contains("HTTP 403") || rangeErr.contains("HTTP 403")) {
            return "Access denied - Please check if the file URL is public or update permissions";
        } else if (headErr.contains("HTTP 404") || rangeErr.contains("HTTP 404")) {
            return "File not found - Please verify the URL is correct and the file exists";
        } else if (headErr.contains("HTTP 401") || rangeErr.contains("HTTP 401")) {
            return "Authentication required - Please provide a publicly accessible URL";
        } else if (headErr.contains("HTTP 500") || rangeErr.contains("HTTP 500")) {
            return "Server error - Please try again later or contact the file provider";
        } else if (headErr.contains("HTTP 503") || rangeErr.contains("HTTP 503")) {
            return "Service unavailable - Please try again later";
        } else if (headErr.contains("HTTP 408") || rangeErr.contains("HTTP 408")) {
            return "Request timeout - Please check your internet connection and try again";
        } else if (headErr.contains("HTTP 429") || rangeErr.contains("HTTP 429")) {
            return "Too many requests - Please wait a moment and try again";
        } else if (headErr.contains("Connection refused") || rangeErr.contains("Connection refused")) {
            return "Cannot connect to server - Please check if the URL is correct";
        } else if (headErr.contains("UnknownHostException") || rangeErr.contains("UnknownHostException")) {
            return "Invalid domain name - Please check the URL format";
        } else {
            return "File not accessible - Please verify the URL is correct and publicly accessible";
        }
    }
    
    /**
     * Result of URL validation operation.
     * Contains validation status, error message, and response time.
     */
    public static class FileValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final long responseTime;
        
        private FileValidationResult(boolean valid, String errorMessage, long responseTime) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.responseTime = responseTime;
        }
        
        /**
         * Creates a valid validation result with measured response time.
         */
        public static FileValidationResult valid() {
            return new FileValidationResult(true, null, System.currentTimeMillis());
        }
        
        /**
         * Creates a valid validation result with specific response time.
         */
        public static FileValidationResult valid(long responseTime) {
            return new FileValidationResult(true, null, responseTime);
        }
        
        /**
         * Creates an invalid validation result with error message.
         */
        public static FileValidationResult invalid(String errorMessage) {
            return new FileValidationResult(false, errorMessage, System.currentTimeMillis());
        }
        
        /**
         * Creates an invalid validation result with error message and specific response time.
         */
        public static FileValidationResult invalid(String errorMessage, long responseTime) {
            return new FileValidationResult(false, errorMessage, responseTime);
        }
        
        // Getters
        public boolean isValid() { 
            return valid; 
        }
        
        public String getErrorMessage() { 
            return errorMessage; 
        }
        
        public long getResponseTime() { 
            return responseTime; 
        }
        
        @Override
        public String toString() {
            return String.format("FileValidationResult{valid=%s, errorMessage='%s', responseTime=%d}", 
                valid, errorMessage, responseTime);
        }
    }
}
