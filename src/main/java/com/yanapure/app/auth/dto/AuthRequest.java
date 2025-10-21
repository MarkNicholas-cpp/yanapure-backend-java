package com.yanapure.app.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for authentication operations
 */
public class AuthRequest {

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("otp")
    private String otp;

    @JsonProperty("refresh_token")
    private String refreshToken;

    // Constructors
    public AuthRequest() {
    }

    public AuthRequest(String phone) {
        this.phone = phone;
    }

    public AuthRequest(String phone, String otp) {
        this.phone = phone;
        this.otp = otp;
    }

    // Getters and Setters
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String toString() {
        return "AuthRequest{" +
                "phone='" + (phone != null ? phone.substring(0, 2) + "****" : null) + '\'' +
                ", otp='" + (otp != null ? "****" : null) + '\'' +
                ", refreshToken='" + (refreshToken != null ? "****" : null) + '\'' +
                '}';
    }
}
