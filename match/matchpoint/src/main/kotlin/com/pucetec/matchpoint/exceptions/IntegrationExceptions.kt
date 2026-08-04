package com.pucetec.matchpoint.exceptions

/** El usuario todavia no registro su perfil en el microservicio `users`. */
class ProfileNotRegisteredException(message: String) : RuntimeException(message)

/** El microservicio `users` no responde. Es recuperable: se reintenta y listo. */
class UsersServiceUnavailableException(message: String) : RuntimeException(message)
