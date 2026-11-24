package br.com.acasadotenis.auth;

import br.com.acasadotenis.auth.service.SmtpConfirmationMailer;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Monta o e-mail de verdade (sem enviar) para pegar erros que só apareceriam com o SMTP ligado. */
class SmtpConfirmationMailerTest {

    @Test
    void montaEmailComTextoHtmlELinkParaOCliente() throws Exception {
        SmtpConfirmationMailer mailer = new SmtpConfirmationMailer(
                "smtp.exemplo.com", 587, "loja@exemplo.com", "x", "loja@exemplo.com");
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

        String link = "http://localhost:5173/confirmar-email?token=abc123";
        mailer.buildMessage(message, "cliente@email.com", "Artur Silva Vaz", link);
        message.saveChanges();

        String conteudo = textoDasPartes(message);
        String raw = message.getAllRecipients()[0].toString() + "\n" + conteudo;

        assertTrue(raw.contains("cliente@email.com"), "destinatário é o e-mail do cadastro");
        assertTrue(conteudo.contains("<a href=\"" + link + "\""), "tem a parte HTML com o botão");
        assertTrue(conteudo.contains("Olá, Artur!"), "tem o primeiro nome");
        assertTrue(conteudo.contains(link), "tem o link");
    }

    /** Junta o texto de todas as partes do e-mail, já decodificado. */
    private static String textoDasPartes(Part part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String text) return text + "\n";
        if (content instanceof Multipart multi) {
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < multi.getCount(); i++) all.append(textoDasPartes(multi.getBodyPart(i)));
            return all.toString();
        }
        return "";
    }
}
