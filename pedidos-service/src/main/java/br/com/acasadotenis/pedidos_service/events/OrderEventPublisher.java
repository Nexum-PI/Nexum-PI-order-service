package br.com.acasadotenis.pedidos_service.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

/**
 * Envia os eventos de pedido para o Kafka só depois que a transação foi gravada
 * no banco (assim nunca se avisa o cliente de algo que não foi salvo).
 * Com nexum.kafka.enabled=false (testes e execução sem Kafka), apenas registra no log.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final ObjectProvider<KafkaTemplate<String, String>> kafka;
    private final JsonMapper json;
    private final boolean enabled;
    private final String topic;

    public OrderEventPublisher(ObjectProvider<KafkaTemplate<String, String>> kafka, JsonMapper json,
                               @Value("${nexum.kafka.enabled:false}") boolean enabled,
                               @Value("${nexum.kafka.topic:pedidos.eventos}") String topic) {
        this.kafka = kafka;
        this.json = json;
        this.enabled = enabled;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderEvent event) {
        String payload = json.writeValueAsString(event);
        if (!enabled) {
            log.info("Kafka desligado; evento não enviado: {}", payload);
            return;
        }
        kafka.getObject().send(topic, event.code(), payload)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Falha ao publicar evento do pedido {}", event.code(), ex);
                    else log.info("Evento publicado em {}: {} {}", topic, event.type(), event.code());
                });
    }
}
