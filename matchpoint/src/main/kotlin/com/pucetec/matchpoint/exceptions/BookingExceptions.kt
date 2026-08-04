package com.pucetec.matchpoint.exceptions

class CourtNotFoundException(message: String) : RuntimeException(message)
class ReservationNotFoundException(message: String) : RuntimeException(message)

class NotYourCourtException(message: String) : RuntimeException(message)
class NotYourReservationException(message: String) : RuntimeException(message)

class InvalidCourtException(message: String) : RuntimeException(message)
class InvalidReservationException(message: String) : RuntimeException(message)

class CourtNotAvailableException(message: String) : RuntimeException(message)
