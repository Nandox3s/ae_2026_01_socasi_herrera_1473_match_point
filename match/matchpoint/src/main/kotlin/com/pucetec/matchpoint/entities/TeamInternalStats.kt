package com.pucetec.matchpoint.entities

import jakarta.persistence.Embeddable

@Embeddable
class TeamInternalStats(
    var eliminated: Boolean = false,
    var matchesPlayed: Int = 0,
    var matchesWon: Int = 0,
    var matchesLost: Int = 0,
    var pointsFor: Int = 0,
    var pointsAgainst: Int = 0,
    var currentRound: Int = 0
)
