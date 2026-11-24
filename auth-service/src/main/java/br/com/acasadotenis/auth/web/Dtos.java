package br.com.acasadotenis.auth.web;

import br.com.acasadotenis.auth.domain.Role;
import br.com.acasadotenis.auth.domain.StaffMember;
import jakarta.validation.constraints.*;

import java.time.Instant;

/** Contratos da API de autenticação, cadastro de clientes e funcionários. */
public final class Dtos {
    private Dtos() {}

    private static final String PHONE_RE = "^\\D*(\\d\\D*){10,11}$";

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail") String email,
            @NotBlank(message = "Informe a senha") String password) {}

    public record LoginResponse(String token, Instant expiresAt, StaffView user) {}

    /** Cadastro público: sempre cria uma conta de CLIENTE (o perfil não vem do corpo). */
    public record RegisterRequest(
            @NotBlank(message = "Informe o seu nome") @Size(max = 120) String name,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 160) String email,
            @NotBlank(message = "Informe o telefone")
            @Pattern(regexp = PHONE_RE, message = "Telefone com DDD, ex.: (11) 90000-0000") String phone,
            @NotBlank(message = "Informe o CEP")
            @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP deve ter 8 números") String cep,
            @NotBlank(message = "Defina uma senha")
            @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String password,
            @AssertTrue(message = "É preciso aceitar a política de privacidade") boolean acceptTerms) {}

    /** O próprio cliente (ou funcionário) edita nome, telefone e CEP. O e-mail não muda. */
    public record UpdateProfileRequest(
            @NotBlank(message = "Informe o seu nome") @Size(max = 120) String name,
            @NotBlank(message = "Informe o telefone")
            @Pattern(regexp = PHONE_RE, message = "Telefone com DDD, ex.: (11) 90000-0000") String phone,
            @NotBlank(message = "Informe o CEP")
            @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP deve ter 8 números") String cep) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Informe a senha atual") String currentPassword,
            @NotBlank(message = "Defina a nova senha")
            @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String newPassword) {}

    public record CreateStaffRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120) String name,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 160) String email,
            @NotNull(message = "Informe o perfil") Role role,
            @NotBlank(message = "Defina uma senha inicial")
            @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String password) {}

    /** Edição: senha é opcional (em branco = mantém a atual). */
    public record UpdateStaffRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120) String name,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 160) String email,
            @NotNull(message = "Informe o perfil") Role role,
            @Size(max = 72) String password) {}

    public record StaffView(Long id, String name, String email, Role role, boolean active, String phone, String cep) {
        public static StaffView from(StaffMember m) {
            return new StaffView(m.getId(), m.getName(), m.getEmail(), m.getRole(), m.isActive(), m.getPhone(), m.getCep());
        }
    }
}
