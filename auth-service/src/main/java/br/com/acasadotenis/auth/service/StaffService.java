package br.com.acasadotenis.auth.service;

import br.com.acasadotenis.auth.domain.Role;
import br.com.acasadotenis.auth.domain.StaffMember;
import br.com.acasadotenis.auth.domain.StaffRepository;
import br.com.acasadotenis.auth.web.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Contas de acesso: login, cadastro de clientes e gestão da equipe. */
@Service
public class StaffService {

    private final StaffRepository repository;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public StaffService(StaffRepository repository, PasswordEncoder encoder, TokenService tokens) {
        this.repository = repository;
        this.encoder = encoder;
        this.tokens = tokens;
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
        return respond(m);
    }

    /** Cadastro público: cria sempre uma conta de CLIENTE e já devolve o login. */
    @Transactional
    public LoginResponse register(RegisterRequest req) {
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
        return respond(repository.save(m));
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
