package br.com.acasadotenis.auth.service;

import org.springframework.http.HttpStatus;

/** Erro de regra com status HTTP e mensagem em português. */
public class AuthException extends RuntimeException {
    private final HttpStatus status;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
