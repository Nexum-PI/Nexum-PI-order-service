package br.com.acasadotenis.pedidos_service.web;

import br.com.acasadotenis.pedidos_service.dto.CreateOrderRequest;
import br.com.acasadotenis.pedidos_service.dto.OrderResponse;
import br.com.acasadotenis.pedidos_service.dto.TrackingResponse;
import br.com.acasadotenis.pedidos_service.dto.UpdateStatusRequest;
import br.com.acasadotenis.pedidos_service.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de pedidos.
 * Público:  GET /api/orders/{code}/tracking (consulta do cliente pelo código)
 * Equipe:   GET /api/orders, GET /api/orders/{code}, POST /api/orders, PATCH /api/orders/{code}/status
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrderResponse> list() {
        return service.list();
    }

    @GetMapping("/{code}")
    public OrderResponse get(@PathVariable String code) {
        return service.get(code);
    }

    @GetMapping("/{code}/tracking")
    public TrackingResponse track(@PathVariable String code) {
        return service.track(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{code}/status")
    public OrderResponse changeStatus(@PathVariable String code, @Valid @RequestBody UpdateStatusRequest request) {
        return service.changeStatus(code, request);
    }
}
