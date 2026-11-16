package br.com.acasadotenis.notificacao;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderEventListenerTest {

    @Test
    void montaMensagemComPrimeiroNomeEStatus() {
        var json = JsonMapper.builder().build();
        var event = json.readTree("""
                {"type":"STATUS_CHANGED","code":"NX-2026-000125","customerName":"Rafael Souza",
                 "customerPhone":"(11) 90000-0000","status":"READY"}""");
        Notification n = OrderEventListener.toNotification(event);
        assertEquals("NX-2026-000125", n.getOrderCode());
        assertEquals("Olá, Rafael. Seu pedido NX-2026-000125 está pronto para retirada/entrega. A Casa do Tênis",
                n.getMessage());
        assertEquals("PENDING", n.getDeliveryStatus());
    }
}
