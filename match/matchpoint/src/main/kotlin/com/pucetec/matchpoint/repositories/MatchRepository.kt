package com.pucetec.matchpoint.repositories

import com.pucetec.matchpoint.entities.Match
import org.springframework.data.jpa.repository.JpaRepository

interface MatchRepository : JpaRepository<Match, Long> {

    fun findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(tournamentId: Long): List<Match>

    fun findByTournamentIdAndRoundNumberAndPositionInRound(
        tournamentId: Long,
        roundNumber: Int,
        positionInRound: Int
    ): Match?
}
