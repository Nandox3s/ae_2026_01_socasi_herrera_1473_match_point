package com.pucetec.matchpoint.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Auditoria de las entidades principales (Criterio 2, punto e2): quien, que, cuando y
 * valores anteriores/nuevos. Vive en la base del propio microservicio.
 */
@Entity
@Table(name = "audit_log")
class AuditLog(

    @Column(name = "entity_name", nullable = false)
    val entityName: String,

    @Column(name = "entity_id", nullable = false)
    val entityId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val action: AuditAction,

    @Column(name = "user_sub", nullable = false)
    val userSub: String,

    @Column(name = "user_name", nullable = false)
    val userName: String,

    @Column(name = "old_values", length = 2000)
    val oldValues: String? = null,

    @Column(name = "new_values", length = 2000)
    val newValues: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
)
