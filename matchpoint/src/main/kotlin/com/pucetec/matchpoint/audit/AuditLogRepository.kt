package com.pucetec.matchpoint.audit

import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogRepository : JpaRepository<AuditLog, Long> {
    fun findByEntityNameAndEntityIdOrderByIdDesc(entityName: String, entityId: Long): List<AuditLog>
}
