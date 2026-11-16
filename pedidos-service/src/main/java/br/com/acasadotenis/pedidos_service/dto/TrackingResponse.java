package br.com.acasadotenis.pedidos_service.dto;

import br.com.acasadotenis.pedidos_service.domain.Order;
import br.com.acasadotenis.pedidos_service.domain.OrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Consulta pública do cliente (sem login). Expõe o mínimo:
 * só o primeiro nome, sem telefone, CEP ou preços.
 */
public record TrackingResponse(
        String code,
        String firstName,
        OrderStatus status,
        Instant receivedAt,
        LocalDate expectedAt,
        Instant deliveredAt,
        List<Item> items,
        List<OrderResponse.HistoryEntry> history
) {
    public record Item(String shoe, String service) {}

    public static TrackingResponse from(Order o) {
        String firstName = o.getCustomerName().trim().split("\\s+")[0];
        return new TrackingResponse(
                o.getCode(), firstName, o.getStatus(), o.getReceivedAt(), o.getExpectedAt(), o.getDeliveredAt(),
                o.getItems().stream().map(i -> new Item(i.getShoe(), i.getService())).toList(),
                o.getHistory().stream()
                        .map(h -> new OrderResponse.HistoryEntry(h.getStatus(), h.getChangedAt(), h.getNote()))
                        .toList());
    }
}
