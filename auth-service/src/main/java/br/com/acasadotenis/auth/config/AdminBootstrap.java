package br.com.acasadotenis.auth.config;

import br.com.acasadotenis.auth.domain.Role;
import br.com.acasadotenis.auth.domain.StaffMember;
import br.com.acasadotenis.auth.domain.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro administrador quando não há nenhum funcionário cadastrado.
 * E-mail e senha vêm de ADMIN_EMAIL / ADMIN_PASSWORD. Depois do primeiro login,
 * o administrador cadastra o resto da equipe pelo painel.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final StaffRepository repository;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminBootstrap(StaffRepository repository, PasswordEncoder encoder,
                          @Value("${nexum.admin.email}") String email,
                          @Value("${nexum.admin.password}") String password) {
        this.repository = repository;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;
        StaffMember admin = new StaffMember();
        admin.setName("Administrador");
        admin.setEmail(email.trim().toLowerCase());
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(encoder.encode(password));
        repository.save(admin);
        log.info("Primeiro administrador criado: {}", admin.getEmail());
    }
}
