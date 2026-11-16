package br.com.acasadotenis.pedidos_service.dto;

import br.com.acasadotenis.pedidos_service.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(
        @NotNull(message = "Informe o status") OrderStatus status,
        @Size(max = 255) String note
) {}
