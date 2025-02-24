package com.bank.appbank.exceptions.handler;

import com.bank.appbank.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ExceptionHandlers {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlers.class);

    @ExceptionHandler({
            ResourceNotFoundException.class,
            CreditInvalid.class,
            LimitAccountsException.class,
            IneligibleClientException.class,
            ClientNotFoundException.class,
            LimitMovementsExceeded.class,
            InsufficientBalance.class,
            UnsupportedMovementException.class,
            InvalidPayException.class,
            ConsumeNotValidException.class,
            RuntimeException.class
    })
    public Mono<ResponseEntity<Map<String, String>>> handleExceptions(RuntimeException exception) {
        HttpStatus status = getStatus(exception);
        return Mono.just(ResponseEntity.status(status).body(
                Map.of("message", exception.getMessage())
        ));
    }

    private HttpStatus getStatus(RuntimeException exception) {
        if (exception instanceof ResourceNotFoundException || exception instanceof ClientNotFoundException) {
            log.warn(exception.getMessage());
            return HttpStatus.NOT_FOUND;
        } else if (exception instanceof CreditInvalid || exception instanceof LimitAccountsException) {
            log.warn(exception.getMessage());
            return HttpStatus.CONFLICT;
        } else if (exception instanceof IneligibleClientException || exception instanceof CanNotDeleteEntity) {
            log.warn(exception.getMessage());
            return HttpStatus.FORBIDDEN;
        } else if (exception instanceof LimitMovementsExceeded || exception instanceof InsufficientBalance ||
                exception instanceof UnsupportedMovementException || exception instanceof InvalidPayException ||
                exception instanceof ConsumeNotValidException || exception instanceof ClientAlreadyExist ||
                exception instanceof InconsistentClientException) {
            log.warn(exception.getMessage());
            return HttpStatus.BAD_REQUEST;
        } else {
            log.error(exception.getMessage());
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<Map<String, String>> HandleValidationException(WebExchangeBindException exception) {
        Map<String, String> errorsResponse = new HashMap<>();
        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errorsResponse.put(error.getField(), error.getDefaultMessage()));
        log.warn(exception.getMessage());
        return Mono.just(errorsResponse);
    }

}
