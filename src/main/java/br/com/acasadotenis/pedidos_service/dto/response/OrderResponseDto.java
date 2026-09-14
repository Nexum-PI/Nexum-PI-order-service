package br.com.acasadotenis.pedidos_service.dto.response;

import br.com.acasadotenis.pedidos_service.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderResponseDto(
        Long id,
        String customerName,
        String customerEmail,
        String customerPhone,
        String serviceType,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime estimatedDeliveryDate
) {}
