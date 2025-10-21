package com.yanapure.app.users;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "users",
    indexes = {
      @Index(name = "idx_users_phone", columnList = "phone"),
      @Index(name = "idx_users_email", columnList = "email")
    })
public class User {
  @Id @GeneratedValue private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, unique = true, length = 16)
  private String phone;

  @Column(unique = true, length = 255)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role = Role.USER;

  private Instant lastLoginAt;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  public boolean isAdmin() {
    return role == Role.ADMIN;
  }

  public boolean hasEmail() {
    return email != null && !email.isBlank();
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(Instant lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  /** Check if user is regular user */
  public boolean isUser() {
    return this.role == Role.USER;
  }
}
