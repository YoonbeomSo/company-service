package com.yoonbeom.companyservice.support.error

class CoreException(
    val errorType: ErrorType,
    override val message: String = errorType.message,
) : RuntimeException(message)
