package br.com.acasadotenis.auth.web;

import br.com.acasadotenis.auth.service.StaffService;
import br.com.acasadotenis.auth.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * POST  /api/auth/login        público: devolve o JWT
 * POST  /api/auth/register     público: cliente cria a própria conta (perfil CLIENT) e já entra
 * GET   /api/auth/me           logado: dados de quem está logado
 * PUT   /api/auth/me           logado: edita nome, telefone e CEP
 * PATCH /api/auth/me/password  logado: troca a senha
 * GET/POST/PUT/PATCH /api/staff  só ADMIN: cadastro de funcionários
 */
@RestController
public class AuthController {

    private final StaffService service;

    public AuthController(StaffService service) {
        this.service = service;
    }

    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/api/auth/confirm")
    public LoginResponse confirm(@Valid @RequestBody ConfirmRequest request) {
        return service.confirm(request.token());
    }

    @PostMapping("/api/auth/resend-confirmation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resend(@Valid @RequestBody ResendRequest request) {
        service.resendConfirmation(request.email());
    }

    @GetMapping("/api/auth/me")
    public StaffView me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(jwt.getSubject());
    }

    @PutMapping("/api/auth/me")
    public StaffView updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return service.updateProfile(jwt.getSubject(), request);
    }

    @PatchMapping("/api/auth/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(jwt.getSubject(), request);
    }

    @GetMapping("/api/staff")
    public List<StaffView> list() {
        return service.list();
    }

    @PostMapping("/api/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffView create(@Valid @RequestBody CreateStaffRequest request) {
        return service.create(request);
    }

    @PutMapping("/api/staff/{id}")
    public StaffView update(@PathVariable Long id, @Valid @RequestBody UpdateStaffRequest request) {
        return service.update(id, request);
    }

    public record ActiveRequest(boolean active) {}

    @PatchMapping("/api/staff/{id}/active")
    public StaffView setActive(@PathVariable Long id, @RequestBody ActiveRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.setActive(id, request.active(), jwt.getSubject());
    }
}
