package com.pucetec.users.exceptions

class BlankNameException(message: String) : RuntimeException(message)

class UserNotFoundException(message: String) : RuntimeException(message)

class DuplicateCognitoIdException(message: String) : RuntimeException(message)
