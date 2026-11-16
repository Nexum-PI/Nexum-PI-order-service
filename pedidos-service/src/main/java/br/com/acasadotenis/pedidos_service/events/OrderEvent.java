package br.com.acasadotenis.pedidos_service.events;

import br.com.acasadotenis.pedidos_service.domain.Order;
import br.com.acasadotenis.pedidos_service.domain.OrderStatus;

import java.time.Instant;

/**
 * Evento publicado no Kafka (tópico pedidos.eventos) quando um pedido é criado
 * ou muda de status. O notificacao-service consome para avisar o cliente.
 */
public record OrderEvent(
        String type,          // ORDER_CREATED | STATUS_CHANGED
        String code,
        String customerName,
        String customerPhone,
        OrderStatus status,
        String note,
        Instant occurredAt
) {
    public static OrderEvent of(String type, Order o, String note, Instant at) {
        return new OrderEvent(type, o.getCode(), o.getCustomerName(), o.getCustomerPhone(), o.getStatus(), note, at);
    }
}
