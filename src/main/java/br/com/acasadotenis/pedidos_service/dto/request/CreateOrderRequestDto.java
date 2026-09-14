package br.com.acasadotenis.pedidos_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequestDto(
        @NotBlank(message = "Customer name is required")
        String customerName,

        @NotBlank(message = "Contact phone is required")
        String contactPhone,

        @NotBlank(message = "Contact email is required")
        String contactEmail,

        @NotBlank(message = "Service type is required")
        String serviceType
) {
}
