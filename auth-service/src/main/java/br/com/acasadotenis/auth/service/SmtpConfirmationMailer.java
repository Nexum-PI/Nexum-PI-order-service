package br.com.acasadotenis.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.Properties;

/**
 * Envio por SMTP (Gmail, Brevo, Resend etc.), configurado por MAIL_HOST, MAIL_PORT, MAIL_USER,
 * MAIL_PASSWORD e MAIL_FROM. Sem MAIL_HOST o e-mail não sai: o link aparece no log do serviço,
 * o que basta para desenvolver sem configurar um provedor.
 * Falha no envio nunca derruba o cadastro: a pessoa pode pedir o reenvio.
 */
@Component
public class SmtpConfirmationMailer implements ConfirmationMailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpConfirmationMailer.class);

    private final JavaMailSenderImpl sender;
    private final String from;

    public SmtpConfirmationMailer(
            @Value("${nexum.mail.host:}") String host,
            @Value("${nexum.mail.port:587}") int port,
            @Value("${nexum.mail.user:}") String user,
            @Value("${nexum.mail.password:}") String password,
            @Value("${nexum.mail.from:}") String from) {
        this.from = from.isBlank() ? user : from;
        if (host.isBlank()) {
            this.sender = null;
            log.warn("MAIL_HOST não configurado: os links de confirmação aparecem só neste log.");
            return;
        }
        JavaMailSenderImpl s = new JavaMailSenderImpl();
        s.setHost(host);
        s.setPort(port);
        s.setUsername(user);
        s.setPassword(password);
        s.setDefaultEncoding("UTF-8");
        Properties props = s.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");
        props.put("mail.smtp.writetimeout", "8000");
        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        this.sender = s;
    }

    @Override
    public void sendConfirmation(String to, String name, String link) {
        if (sender == null) {
            log.info("Link de confirmação para {}: {}", to, link);
            return;
        }
        try {
            sender.send(buildMessage(sender.createMimeMessage(), to, name, link));
            log.info("E-mail de confirmação enviado para {}", to);
        } catch (Exception e) {
            log.error("Não foi possível enviar o e-mail de confirmação para {}: {}", to, e.getMessage());
        }
    }

    /** Monta o e-mail (texto + HTML). Separado do envio para poder ser testado sem SMTP. */
    public MimeMessage buildMessage(MimeMessage message, String to, String name, String link) throws Exception {
        // multipart: é o que permite mandar o mesmo e-mail em texto e em HTML
        MimeMessageHelper helper =
                new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
        helper.setFrom(from.isBlank() ? "no-reply@acasadotenis.com.br" : from, "A Casa do Tênis");
        helper.setTo(to);
        helper.setSubject("Confirme seu cadastro · A Casa do Tênis");
        helper.setText(plain(name, link), html(name, link));
        return message;
    }

    private static String first(String name) {
        String[] parts = name.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : name;
    }

    private static String plain(String name, String link) {
        return "Olá, " + first(name) + "!\n\n"
                + "Para ativar sua conta na A Casa do Tênis, abra o link abaixo (vale por 24 horas):\n"
                + link + "\n\n"
                + "Se você não criou essa conta, é só ignorar este e-mail.";
    }

    private static String html(String name, String link) {
        String safeName = HtmlUtils.htmlEscape(first(name));
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;background:#faf6e8;padding:24px">
                  <div style="max-width:480px;margin:auto;background:#fff;border-radius:12px;overflow:hidden;border:1px solid #e6dfc4">
                    <div style="background:#14283d;color:#f3e9c6;padding:20px 24px;font-size:18px;font-weight:bold;letter-spacing:.04em">A CASA DO TÊNIS</div>
                    <div style="padding:24px;color:#14283d;line-height:1.5">
                      <p style="font-size:18px;margin:0 0 12px"><b>Olá, %s!</b></p>
                      <p style="margin:0 0 20px">Falta só um passo para ativar sua conta e acompanhar seus pedidos.</p>
                      <p style="margin:0 0 24px"><a href="%s" style="background:#c9a24a;color:#14283d;text-decoration:none;font-weight:bold;padding:14px 24px;border-radius:8px;display:inline-block">Confirmar meu e-mail</a></p>
                      <p style="font-size:13px;color:#3d5268;margin:0 0 8px">O link vale por 24 horas. Se o botão não abrir, copie e cole no navegador:</p>
                      <p style="font-size:12px;color:#3d5268;word-break:break-all;margin:0 0 16px">%s</p>
                      <p style="font-size:13px;color:#3d5268;margin:0">Se você não criou essa conta, é só ignorar este e-mail.</p>
                    </div>
                  </div>
                </div>
                """.formatted(safeName, HtmlUtils.htmlEscape(link), HtmlUtils.htmlEscape(link));
    }
}
