package br.com.acasadotenis.pedidos_service.dto;

import br.com.acasadotenis.pedidos_service.domain.Order;
import br.com.acasadotenis.pedidos_service.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Visão completa do pedido, só para a equipe (inclui contato, CEP e preços). */
public record OrderResponse(
        String code,
        String customerName,
        String customerPhone,
        String customerCep,
        OrderStatus status,
        Instant receivedAt,
        LocalDate expectedAt,
        Instant deliveredAt,
        String notes,
        BigDecimal total,
        List<Item> items,
        List<HistoryEntry> history
) {
    public record Item(String shoe, String service, BigDecimal price, String detail) {}

    public record HistoryEntry(OrderStatus status, Instant changedAt, String note) {}

    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getCode(), o.getCustomerName(), o.getCustomerPhone(), o.getCustomerCep(),
                o.getStatus(), o.getReceivedAt(), o.getExpectedAt(), o.getDeliveredAt(), o.getNotes(),
                o.getTotal(),
                o.getItems().stream().map(i -> new Item(i.getShoe(), i.getService(), i.getPrice(), i.getDetail())).toList(),
                o.getHistory().stream().map(h -> new HistoryEntry(h.getStatus(), h.getChangedAt(), h.getNote())).toList());
    }
}
