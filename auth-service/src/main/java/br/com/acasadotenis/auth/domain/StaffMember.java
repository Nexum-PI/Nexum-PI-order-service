package br.com.acasadotenis.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Conta de acesso (tabela staff_members): funcionários e clientes, separados pelo
 * perfil (role). O nome da tabela vem da primeira versão, só com equipe.
 */
@Entity
@Table(name = "staff_members")
public class StaffMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    /** Senha guardada só como hash (BCrypt). */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Contato e CEP: preenchidos pelo cliente no cadastro (podem ficar vazios para a equipe). */
    @Column(length = 20)
    private String phone;

    @Column(length = 9)
    private String cep;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Cadastro de cliente ainda sem o e-mail confirmado. Fica "falso" por padrão para que contas
     * antigas e as criadas pelo administrador continuem entrando normalmente.
     */
    @Column(name = "email_pending", nullable = false, columnDefinition = "boolean default false")
    private boolean emailPending = false;

    /** Hash (SHA-256) do link de confirmação; o texto do link nunca é guardado. */
    @Column(name = "confirm_token_hash", length = 64)
    private String confirmTokenHash;

    @Column(name = "confirm_expires_at")
    private Instant confirmExpiresAt;

    @Column(name = "confirm_sent_at")
    private Instant confirmSentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isEmailPending() { return emailPending; }
    public void setEmailPending(boolean emailPending) { this.emailPending = emailPending; }
    public String getConfirmTokenHash() { return confirmTokenHash; }
    public void setConfirmTokenHash(String confirmTokenHash) { this.confirmTokenHash = confirmTokenHash; }
    public Instant getConfirmExpiresAt() { return confirmExpiresAt; }
    public void setConfirmExpiresAt(Instant confirmExpiresAt) { this.confirmExpiresAt = confirmExpiresAt; }
    public Instant getConfirmSentAt() { return confirmSentAt; }
    public void setConfirmSentAt(Instant confirmSentAt) { this.confirmSentAt = confirmSentAt; }
    public Instant getCreatedAt() { return createdAt; }
}
