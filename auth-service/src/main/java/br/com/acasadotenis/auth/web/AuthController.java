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
 * POST /api/auth/login        público: devolve o JWT
 * GET  /api/auth/me           equipe: dados de quem está logado
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

    @GetMapping("/api/auth/me")
    public StaffView me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(jwt.getSubject());
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
