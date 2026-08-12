package com.pucetec.matchpoint.repositories

import com.pucetec.matchpoint.entities.Reservation
import com.pucetec.matchpoint.enums.ReservationStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ReservationRepository : JpaRepository<Reservation, Long> {
    fun findByOwnerUserOrderByStartsAtDesc(ownerUser: String): List<Reservation>

    fun findByCourtIdAndStatus(courtId: Long, status: ReservationStatus): List<Reservation>
}
