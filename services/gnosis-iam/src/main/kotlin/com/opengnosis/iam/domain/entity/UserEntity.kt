package com.opengnosis.iam.domain.entity

import com.opengnosis.domain.Role
import com.opengnosis.domain.UserStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users", schema = "iam")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, unique = true)
    var email: String,
    
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,
    
    @Column(name = "first_name", nullable = false)
    var firstName: String,
    
    @Column(name = "last_name", nullable = false)
    var lastName: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserStatus = UserStatus.ACTIVE,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        schema = "iam",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var roles: MutableSet<RoleEntity> = mutableSetOf()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
    
    fun getRoleNames(): Set<Role> = roles.map { Role.valueOf(it.name) }.toSet()
}
