package br.com.acasadotenis.pedidos_service.web;

import br.com.acasadotenis.pedidos_service.dto.TrackingResponse;
import br.com.acasadotenis.pedidos_service.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Área do cliente (só perfil CLIENT). O cliente nunca usa /api/orders, que é da equipe.
 * GET  /api/me/orders  lista os pedidos da conta dele
 * POST /api/me/orders  adiciona um pedido pelo código
 */
@RestController
@RequestMapping("/api/me/orders")
public class MyOrdersController {

    private final OrderService service;

    public MyOrdersController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<TrackingResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.listForAccount(jwt.getSubject());
    }

    public record ClaimRequest(@NotBlank(message = "Informe o código do pedido") String code) {}

    @PostMapping
    public TrackingResponse claim(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ClaimRequest request) {
        return service.claim(jwt.getSubject(), request.code());
    }
}
