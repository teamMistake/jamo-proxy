package io.teammistake.chatjamo.controllers

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.teammistake.chatjamo.dto.JamoAPIError
import io.teammistake.chatjamo.exceptions.APIErrorException
import io.teammistake.chatjamo.exceptions.NotFoundException
import io.teammistake.chatjamo.exceptions.PermissionDeniedException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice
class ExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class, MissingKotlinParameterException::class, DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun onIllegalArgument(e: Exception): JamoAPIError {
        return JamoAPIError(e.message);
    }


    @ExceptionHandler(APIErrorException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun onWeirdResponse(e: APIErrorException): JamoAPIError {
        e.printStackTrace()
        return JamoAPIError(e.message,e.err)
    }




    @ExceptionHandler(PermissionDeniedException::class, BadCredentialsException::class, AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun onWeirdResponse(e: Exception): JamoAPIError {
        e.printStackTrace()
        return JamoAPIError(e.message)
    }

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun onWeirdResponse(e: NotFoundException) {}

    @ExceptionHandler(AuthenticationCredentialsNotFoundException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun onAuthNotFound(e: AuthenticationCredentialsNotFoundException) {}
}