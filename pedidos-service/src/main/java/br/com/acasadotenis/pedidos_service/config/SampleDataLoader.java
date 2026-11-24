package br.com.acasadotenis.pedidos_service.config;

import br.com.acasadotenis.pedidos_service.domain.Order;
import br.com.acasadotenis.pedidos_service.domain.OrderItem;
import br.com.acasadotenis.pedidos_service.domain.OrderStatus;
import br.com.acasadotenis.pedidos_service.repository.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Pedidos de exemplo (os mesmos do front), com datas relativas a hoje para a
 * demonstração sempre ter pedidos atrasados, com prazo próximo e no prazo.
 * Só roda com nexum.sample-data=true e com o banco vazio.
 */
@Component
@ConditionalOnProperty(name = "nexum.sample-data", havingValue = "true")
public class SampleDataLoader implements CommandLineRunner {

    private final OrderRepository repository;
    private final Clock clock;

    public SampleDataLoader(OrderRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (repository.count() > 0) return;
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("America/Sao_Paulo")));

        save("NX-2026-000123", "Carlos Eduardo", "carlos.eduardo@email.com", "01310-100", now.minus(3, ChronoUnit.DAYS), today.plusDays(1),
                item("Nike Air Force 1", "Lavagem Premium", "90"),
                step(OrderStatus.CLEANING, now.minus(1, ChronoUnit.DAYS), "Higienização iniciada"));
        save("NX-2026-000124", "Mariana Costa", "mariana.costa@email.com", "04538-132", now.minus(2, ChronoUnit.HOURS), today.plusDays(5),
                item("Yeezy Boost 350", "Lavagem Premium", "120"));
        save("NX-2026-000125", "Rafael Souza", null, "05422-030", now.minus(8, ChronoUnit.DAYS), today.minusDays(1),
                item("Air Jordan 1", "Pintura & Restauração", "280"),
                step(OrderStatus.RESTORING, now.minus(6, ChronoUnit.DAYS), "Pintura iniciada"),
                step(OrderStatus.READY, now.minus(1, ChronoUnit.DAYS), "Aguardando retirada"));
        save("NX-2026-000126", "Ana Lima", null, "01415-000", now.minus(9, ChronoUnit.DAYS), today.minusDays(2),
                item("Vans Old Skool", "Customização", "350"),
                step(OrderStatus.RESTORING, now.minus(7, ChronoUnit.DAYS), "Aguardando material para customização"));
        save("NX-2026-000127", "Lucas Silva", null, "09015-000", now.minus(12, ChronoUnit.DAYS), today.minusDays(4),
                item("New Balance 550", "Troca de Solado", "220"),
                step(OrderStatus.RESTORING, now.minus(10, ChronoUnit.DAYS), "Troca de solado iniciada"),
                step(OrderStatus.READY, now.minus(5, ChronoUnit.DAYS), "Serviço finalizado"),
                step(OrderStatus.DELIVERED, now.minus(4, ChronoUnit.DAYS), "Retirado na loja"));
    }

    private record Step(OrderStatus status, Instant at, String note) {}

    private static Step step(OrderStatus status, Instant at, String note) {
        return new Step(status, at, note);
    }

    private static OrderItem item(String shoe, String service, String price) {
        OrderItem item = new OrderItem();
        item.setShoe(shoe);
        item.setService(service);
        item.setPrice(new BigDecimal(price));
        return item;
    }

    private void save(String code, String name, String email, String cep, Instant receivedAt, LocalDate expectedAt,
                      OrderItem item, Step... steps) {
        Order o = new Order();
        o.setCode(code);
        o.setCustomerName(name);
        o.setCustomerEmail(email);
        o.setCustomerCep(cep);
        o.setReceivedAt(receivedAt);
        o.setExpectedAt(expectedAt);
        o.addItem(item);
        o.changeStatus(OrderStatus.RECEIVED, "Tênis recebido na loja", receivedAt);
        for (Step s : steps) o.changeStatus(s.status(), s.note(), s.at());
        repository.save(o);
    }
}
