package com.ingsisbry.permission.adapter.web

import com.ingsisbry.permission.adapter.web.dto.ErrorResponse
import com.ingsisbry.permission.application.common.exception.PermissionAlreadyExistsException
import com.ingsisbry.permission.application.common.exception.PermissionDeniedException
import com.ingsisbry.permission.application.common.exception.PermissionNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class PermissionExceptionHandler {
    @ExceptionHandler(PermissionDeniedException::class)
    fun handleDenied(exception: PermissionDeniedException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.FORBIDDEN, exception)

    @ExceptionHandler(PermissionNotFoundException::class)
    fun handleNotFound(exception: PermissionNotFoundException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.NOT_FOUND, exception)

    @ExceptionHandler(PermissionAlreadyExistsException::class)
    fun handleConflict(
        exception: PermissionAlreadyExistsException,
    ): ResponseEntity<ErrorResponse> = respond(HttpStatus.CONFLICT, exception)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(exception: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, exception)

    private fun respond(
        status: HttpStatus,
        exception: RuntimeException,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(status)
            .body(ErrorResponse(exception.message.orEmpty()))
}
