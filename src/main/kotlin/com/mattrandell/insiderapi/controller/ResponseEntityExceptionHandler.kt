package com.mattrandell.insiderapi.controller

import com.mattrandell.insiderapi.dto.ExceptionBody
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest


@ControllerAdvice
class ResponseEntityExceptionHandler : org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler() {

  @ExceptionHandler(value = [RuntimeException::class])
  protected fun handleConflict(ex: RuntimeException, request: WebRequest): ResponseEntity<Any?>? {
    val bodyOfResponse = ExceptionBody(ex.message?:"Something went wrong.")
    return handleExceptionInternal(ex, bodyOfResponse, HttpHeaders(), HttpStatus.BAD_REQUEST, request)
  }
}