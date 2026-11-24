package br.com.acasadotenis.auth;

import br.com.acasadotenis.auth.service.ConfirmationMailer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Nos testes o "e-mail" não sai: guardamos o último link enviado a cada endereço. */
@TestConfiguration
class RecordingMailer {

    static final Map<String, String> LINKS = new ConcurrentHashMap<>();

    static String tokenFor(String email) {
        String link = LINKS.get(email);
        return link == null ? null : link.substring(link.indexOf("token=") + 6);
    }

    @Bean
    @Primary
    ConfirmationMailer recordingMailer() {
        return (to, name, link) -> LINKS.put(to, link);
    }
}
