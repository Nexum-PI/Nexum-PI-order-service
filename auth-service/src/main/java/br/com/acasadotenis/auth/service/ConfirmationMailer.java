package br.com.acasadotenis.auth.service;

/** Envia ao cliente o e-mail com o link para confirmar o cadastro. */
public interface ConfirmationMailer {
    void sendConfirmation(String to, String name, String link);
}
