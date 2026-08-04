package com.pucetec.matchpoint.repositories

import com.pucetec.matchpoint.entities.Tournament
import org.springframework.data.jpa.repository.JpaRepository

interface TournamentRepository : JpaRepository<Tournament, Long>
