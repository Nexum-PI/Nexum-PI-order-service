package br.com.acasadotenis.auth.service;

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

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        StaffMember m = repository.findByEmailIgnoreCase(req.email().trim())
                .filter(s -> encoder.matches(req.password(), s.getPasswordHash()))
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos."));
        if (!m.isActive()) {
            throw new AuthException(HttpStatus.FORBIDDEN, "Acesso desativado. Fale com o administrador.");
        }
        TokenService.Token t = tokens.issue(m);
        return new LoginResponse(t.value(), t.expiresAt(), StaffView.from(m));
    }

    @Transactional(readOnly = true)
    public StaffView me(String email) {
        return repository.findByEmailIgnoreCase(email).map(StaffView::from)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<StaffView> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(StaffMember::isActive).reversed().thenComparing(StaffMember::getName))
                .map(StaffView::from).toList();
    }

    @Transactional
    public StaffView create(CreateStaffRequest req) {
        String email = normalize(req.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new AuthException(HttpStatus.CONFLICT, "Já existe um funcionário com esse e-mail.");
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
        StaffMember m = find(id);
        String email = normalize(req.email());
        repository.findByEmailIgnoreCase(email).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw new AuthException(HttpStatus.CONFLICT, "Já existe um funcionário com esse e-mail.");
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
        StaffMember m = find(id);
        if (!active && m.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Você não pode desativar o próprio acesso.");
        }
        m.setActive(active);
        return StaffView.from(m);
    }

    private StaffMember find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
