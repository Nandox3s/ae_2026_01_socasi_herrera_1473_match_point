package com.pucetec.matchpoint.repositories

import com.pucetec.matchpoint.entities.Court
import org.springframework.data.jpa.repository.JpaRepository

interface CourtRepository : JpaRepository<Court, Long> {

    fun findByActiveTrue(): List<Court>
}
