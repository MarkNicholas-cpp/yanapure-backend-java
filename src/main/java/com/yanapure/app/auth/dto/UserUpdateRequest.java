package com.yanapure.app.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for updating user profile
 */
public class UserUpdateRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    // Constructors
    public UserUpdateRequest() {
    }

    public UserUpdateRequest(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "UserUpdateRequest{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
