package com.pucetec.matchpoint.audit

import com.pucetec.matchpoint.logging.ANONYMOUS_SUB
import com.pucetec.matchpoint.logging.MDC_SUB
import com.pucetec.matchpoint.logging.logLine
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository
) {
    private val logger = LoggerFactory.getLogger(AuditService::class.java)

    fun record(
        entityName: String,
        entityId: Long,
        action: AuditAction,
        oldValues: String? = null,
        newValues: String? = null
    ) {
        val entry = AuditLog(
            entityName = entityName,
            entityId = entityId,
            action = action,
            userSub = currentSub(),
            userName = currentUsername(),
            oldValues = oldValues?.take(2000),
            newValues = newValues?.take(2000)
        )
        auditLogRepository.save(entry)
        logger.debug(
            logLine(
                "audit.recorded",
                "Audit entry stored",
                "entity" to entityName,
                "entityId" to entityId,
                "action" to action
            )
        )
    }

    private fun currentSub(): String = MDC.get(MDC_SUB) ?: currentJwt()?.subject ?: ANONYMOUS_SUB

    private fun currentUsername(): String =
        currentJwt()?.getClaimAsString("username") ?: ANONYMOUS_SUB

    private fun currentJwt(): Jwt? =
        SecurityContextHolder.getContext().authentication?.principal as? Jwt
}
