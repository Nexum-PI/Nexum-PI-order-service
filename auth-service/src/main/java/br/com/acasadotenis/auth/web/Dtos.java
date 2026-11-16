package br.com.acasadotenis.auth.web;

import br.com.acasadotenis.auth.domain.Role;
import br.com.acasadotenis.auth.domain.StaffMember;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Contratos da API de autenticação e funcionários. */
public final class Dtos {
    private Dtos() {}

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail") String email,
            @NotBlank(message = "Informe a senha") String password) {}

    public record LoginResponse(String token, Instant expiresAt, StaffView user) {}

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

    public record StaffView(Long id, String name, String email, Role role, boolean active) {
        public static StaffView from(StaffMember m) {
            return new StaffView(m.getId(), m.getName(), m.getEmail(), m.getRole(), m.isActive());
        }
    }
}
