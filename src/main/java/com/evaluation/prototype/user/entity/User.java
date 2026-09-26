package com.evaluation.prototype.user.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.evaluation.prototype.common.enums.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "force_password_change", nullable = false)
    private boolean forcePasswordChange;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Chain of command: RE created this BO, BO created this Reviewer, Reviewer created this Evaluator.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    // Only meaningful for evaluators: manually toggled, auto-locked while a bundle is assigned to them.
    @Column(nullable = false)
    private boolean available = true;

    // Guards against the read-then-write race in ValuationService.assignBundle(): without
    // this, two concurrent assignment requests for the same evaluator could both read
    // available=true before either transaction commits, double-booking that evaluator.
    // With it, the second transaction to commit fails with an OptimisticLockException
    // instead of silently succeeding.
    @Version
    @Column(nullable = false)
    private long version;

    // The subject this BO/Reviewer/Evaluator is scoped to (inherited down the chain from the BO's subject).
    // Stored as a plain id (not a JPA relation) to keep the user package independent of the valuation package.
    @Column(name = "subject_id")
    private UUID subjectId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isEnabled() {
        return enabled && deletedAt == null && status == UserStatus.ACTIVE;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
        this.enabled = status == UserStatus.ACTIVE;
        if (status != UserStatus.LOCKED) {
            this.lockedAt = null;
        }
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }

    public void setForcePasswordChange(boolean forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void lock() {
        this.status = UserStatus.LOCKED;
        this.enabled = false;
        this.lockedAt = Instant.now();
    }

    public void unlock() {
        this.status = UserStatus.ACTIVE;
        this.enabled = true;
        this.failedLoginAttempts = 0;
        this.lockedAt = null;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public long getVersion() {
        return version;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(UUID subjectId) {
        this.subjectId = subjectId;
    }

    public void softDelete(User deletedBy) {
        this.deletedBy = deletedBy;
        this.deletedAt = Instant.now();
        this.status = UserStatus.INACTIVE;
        this.enabled = false;
    }
}
