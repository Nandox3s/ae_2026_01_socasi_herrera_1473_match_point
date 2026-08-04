package com.pucetec.matchpoint.repositories

import com.pucetec.matchpoint.entities.Team
import org.springframework.data.jpa.repository.JpaRepository

interface TeamRepository : JpaRepository<Team, Long> {

    fun findByTournamentId(tournamentId: Long): List<Team>

    fun countByTournamentId(tournamentId: Long): Long

    fun existsByTournamentIdAndExternalInfoName(tournamentId: Long, name: String): Boolean
}
