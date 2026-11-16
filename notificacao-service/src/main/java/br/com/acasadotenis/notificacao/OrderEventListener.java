package br.com.acasadotenis.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/** Consome o tópico pedidos.eventos (publicado pelo pedidos-service). */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    static final Map<String, String> STATUS_LABEL = Map.of(
            "RECEIVED", "foi recebido na loja",
            "CLEANING", "está em higienização",
            "RESTORING", "está em restauração",
            "READY", "está pronto para retirada/entrega",
            "DELIVERED", "foi entregue. Obrigado pela confiança",
            "CANCELED", "foi cancelado. Fale com a loja se tiver dúvidas");

    private final NotificationRepository repository;
    private final JsonMapper json;

    public OrderEventListener(NotificationRepository repository, JsonMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @KafkaListener(topics = "${nexum.kafka.topic:pedidos.eventos}", groupId = "notificacao-service")
    public void onEvent(String payload) {
        JsonNode e = json.readTree(payload);
        repository.save(toNotification(e));
        log.info("Notificação registrada para o pedido {} ({})", e.path("code").asString(), e.path("status").asString());
    }

    static Notification toNotification(JsonNode e) {
        String code = e.path("code").asString();
        String status = e.path("status").asString();
        String fullName = e.path("customerName").asString("");
        String firstName = fullName.isBlank() ? "" : fullName.trim().split("\\s+")[0];
        Notification n = new Notification();
        n.setOrderCode(code);
        n.setCustomerName(fullName);
        n.setCustomerPhone(e.path("customerPhone").isNull() ? null : e.path("customerPhone").asString(null));
        n.setOrderStatus(status);
        n.setMessage("Olá" + (firstName.isEmpty() ? "" : ", " + firstName) + ". Seu pedido " + code + " "
                + STATUS_LABEL.getOrDefault(status, "foi atualizado") + ". A Casa do Tênis");
        return n;
    }
}
