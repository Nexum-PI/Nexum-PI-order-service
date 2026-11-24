package br.com.acasadotenis.auth.service;

import br.com.acasadotenis.auth.domain.Role;
import br.com.acasadotenis.auth.domain.StaffMember;
import br.com.acasadotenis.auth.domain.StaffRepository;
import br.com.acasadotenis.auth.web.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Contas de acesso: login, cadastro de clientes e gestão da equipe. */
@Service
public class StaffService {

    private final StaffRepository repository;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final ConfirmationMailer mailer;
    private final String frontUrl;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CONFIRM_TTL = Duration.ofHours(24);
    private static final Duration RESEND_WAIT = Duration.ofSeconds(60);

    public StaffService(StaffRepository repository, PasswordEncoder encoder, TokenService tokens,
                        ConfirmationMailer mailer,
                        @org.springframework.beans.factory.annotation.Value("${nexum.front-url:http://localhost:5173}") String frontUrl) {
        this.repository = repository;
        this.encoder = encoder;
        this.tokens = tokens;
        this.mailer = mailer;
        this.frontUrl = frontUrl.replaceAll("/+$", "");
    }

    // ── Login e conta própria ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        StaffMember m = repository.findByEmailIgnoreCase(req.email().trim())
                .filter(s -> encoder.matches(req.password(), s.getPasswordHash()))
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos."));
        if (!m.isActive()) {
            throw new AuthException(HttpStatus.FORBIDDEN, "Acesso desativado. Fale com a loja.");
        }
        if (m.isEmailPending()) {
            throw new AuthException(HttpStatus.FORBIDDEN,
                    "Confirme seu e-mail para entrar. Enviamos um link para " + m.getEmail() + ".");
        }
        return respond(m);
    }

    /**
     * Cadastro público: cria uma conta de CLIENTE pendente e envia o link de confirmação por
     * e-mail. A conta só entra depois de confirmar (o e-mail prova que a pessoa é a dona dele).
     */
    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new AuthException(HttpStatus.CONFLICT, "Já existe uma conta com esse e-mail. Tente entrar.");
        }
        StaffMember m = new StaffMember();
        m.setName(req.name().trim());
        m.setEmail(email);
        m.setPhone(req.phone().trim());
        m.setCep(formatCep(req.cep()));
        m.setRole(Role.CLIENT);
        m.setPasswordHash(encoder.encode(req.password()));
        m.setEmailPending(true);
        String link = issueConfirmation(m);
        repository.save(m);
        sendAfterCommit(m, link);
        return new RegisterResponse(email, "Enviamos um link de confirmação para " + email + ".");
    }

    /** Abre o link do e-mail: ativa a conta e já devolve o login. */
    @Transactional
    public LoginResponse confirm(String token) {
        StaffMember m = repository.findByConfirmTokenHash(hash(token == null ? "" : token.trim()))
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST,
                        "Link inválido ou já utilizado. Peça um novo e-mail de confirmação."));
        if (m.getConfirmExpiresAt() == null || m.getConfirmExpiresAt().isBefore(Instant.now())) {
            throw new AuthException(HttpStatus.BAD_REQUEST,
                    "Esse link expirou. Peça um novo e-mail de confirmação.");
        }
        m.setEmailPending(false);
        m.setConfirmTokenHash(null);
        m.setConfirmExpiresAt(null);
        return respond(m);
    }

    /**
     * Reenvia o e-mail de confirmação. Sempre responde igual, exista a conta ou não, para ninguém
     * descobrir quais e-mails estão cadastrados; e espera 60s entre envios.
     */
    @Transactional
    public void resendConfirmation(String email) {
        if (email == null || email.isBlank()) return;
        repository.findByEmailIgnoreCase(email.trim()).filter(StaffMember::isEmailPending).ifPresent(m -> {
            Instant last = m.getConfirmSentAt();
            if (last != null && last.plus(RESEND_WAIT).isAfter(Instant.now())) return;
            String link = issueConfirmation(m);
            sendAfterCommit(m, link);
        });
    }

    @Transactional(readOnly = true)
    public StaffView me(String email) {
        return StaffView.from(findByEmail(email));
    }

    @Transactional
    public StaffView updateProfile(String email, UpdateProfileRequest req) {
        StaffMember m = findByEmail(email);
        m.setName(req.name().trim());
        m.setPhone(req.phone().trim());
        m.setCep(formatCep(req.cep()));
        return StaffView.from(m);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        StaffMember m = findByEmail(email);
        if (!encoder.matches(req.currentPassword(), m.getPasswordHash())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "A senha atual está incorreta.");
        }
        m.setPasswordHash(encoder.encode(req.newPassword()));
    }

    // ── Equipe (só ADMIN) ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StaffView> list() {
        return repository.findAll().stream()
                .filter(m -> m.getRole() != Role.CLIENT)
                .sorted(Comparator.comparing(StaffMember::isActive).reversed().thenComparing(StaffMember::getName))
                .map(StaffView::from).toList();
    }

    @Transactional
    public StaffView create(CreateStaffRequest req) {
        requireStaffRole(req.role());
        String email = normalize(req.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new AuthException(HttpStatus.CONFLICT, "Já existe uma conta com esse e-mail.");
        }
        StaffMember m = new StaffMember();
        m.setName(req.name().trim());
        m.setEmail(email);
        m.setRole(req.role());
        m.setPasswordHash(encoder.encode(req.password()));
        return StaffView.from(repository.save(m));
    }

    @Transactional
    public StaffView update(Long id, UpdateStaffRequest req) {
        requireStaffRole(req.role());
        StaffMember m = findStaff(id);
        String email = normalize(req.email());
        repository.findByEmailIgnoreCase(email).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw new AuthException(HttpStatus.CONFLICT, "Já existe uma conta com esse e-mail.");
        });
        m.setName(req.name().trim());
        m.setEmail(email);
        m.setRole(req.role());
        if (req.password() != null && !req.password().isBlank()) {
            if (req.password().length() < 8) {
                throw new AuthException(HttpStatus.BAD_REQUEST, "A senha deve ter pelo menos 8 caracteres.");
            }
            m.setPasswordHash(encoder.encode(req.password()));
        }
        return StaffView.from(m);
    }

    @Transactional
    public StaffView setActive(Long id, boolean active, String currentEmail) {
        StaffMember m = findStaff(id);
        if (!active && m.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Você não pode desativar o próprio acesso.");
        }
        m.setActive(active);
        return StaffView.from(m);
    }

    // ── Apoio ──────────────────────────────────────────────────────────────

    /** Gera um link novo (guarda só o hash) e devolve o endereço completo para o e-mail. */
    private String issueConfirmation(StaffMember m) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        m.setConfirmTokenHash(hash(token));
        m.setConfirmExpiresAt(Instant.now().plus(CONFIRM_TTL));
        m.setConfirmSentAt(Instant.now());
        return frontUrl + "/confirmar-email?token=" + token;
    }

    /** Só envia depois que a conta foi gravada, para o link nunca apontar para algo que não existe. */
    private void sendAfterCommit(StaffMember m, String link) {
        String to = m.getEmail();
        String name = m.getName();
        Runnable send = () -> mailer.sendConfirmation(to, name, link);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private LoginResponse respond(StaffMember m) {
        TokenService.Token t = tokens.issue(m);
        return new LoginResponse(t.value(), t.expiresAt(), StaffView.from(m));
    }

    /** A gestão de equipe só enxerga e altera funcionários, nunca contas de cliente. */
    private StaffMember findStaff(Long id) {
        return repository.findById(id)
                .filter(m -> m.getRole() != Role.CLIENT)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
    }

    private StaffMember findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
    }

    private static void requireStaffRole(Role role) {
        if (role == Role.CLIENT) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Clientes criam a própria conta em /cadastro.");
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String formatCep(String cep) {
        String digits = cep.replaceAll("\\D", "");
        return digits.substring(0, 5) + "-" + digits.substring(5);
    }
}
