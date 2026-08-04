package com.pucetec.matchpoint.exceptions

class TournamentNotFoundException(message: String) : RuntimeException(message)
class TeamNotFoundException(message: String) : RuntimeException(message)
class MatchNotFoundException(message: String) : RuntimeException(message)

class NotYourTournamentException(message: String) : RuntimeException(message)
class NotYourTeamException(message: String) : RuntimeException(message)

class InvalidTournamentException(message: String) : RuntimeException(message)
class InvalidTeamException(message: String) : RuntimeException(message)
class TieNotAllowedException(message: String) : RuntimeException(message)

class TournamentFullException(message: String) : RuntimeException(message)
class DuplicateTeamNameException(message: String) : RuntimeException(message)
class RegistrationClosedException(message: String) : RuntimeException(message)
class TournamentNotReadyException(message: String) : RuntimeException(message)
class MatchNotReadyException(message: String) : RuntimeException(message)
class MatchAlreadyPlayedException(message: String) : RuntimeException(message)
