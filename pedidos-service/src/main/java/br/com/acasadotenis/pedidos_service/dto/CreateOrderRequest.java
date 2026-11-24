package br.com.acasadotenis.pedidos_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Abertura de ordem de serviço (tela "Nova ordem" do painel). */
public record CreateOrderRequest(
        @NotBlank(message = "Informe o nome do cliente") @Size(max = 120) String customerName,
        @Size(max = 20) String customerPhone,
        @Email(message = "E-mail inválido") @Size(max = 160) String customerEmail,
        @NotBlank(message = "Informe o CEP do cliente")
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP deve ter 8 números") String customerCep,
        @NotNull(message = "Informe a previsão de entrega") LocalDate expectedAt,
        @Size(max = 1000) String notes,
        @NotEmpty(message = "Inclua pelo menos um item") List<@Valid Item> items
) {
    public record Item(
            @NotBlank(message = "Informe o modelo do tênis") @Size(max = 120) String shoe,
            @NotBlank(message = "Informe o serviço") @Size(max = 60) String service,
            @NotNull(message = "Informe o valor") @Positive(message = "O valor deve ser maior que zero")
            @Digits(integer = 8, fraction = 2) BigDecimal price,
            @Size(max = 120) String detail
    ) {}
}
