package br.com.acasadotenis.pedidos_service.service;

import br.com.acasadotenis.pedidos_service.domain.Order;
import br.com.acasadotenis.pedidos_service.domain.OrderItem;
import br.com.acasadotenis.pedidos_service.domain.OrderStatus;
import br.com.acasadotenis.pedidos_service.dto.CreateOrderRequest;
import br.com.acasadotenis.pedidos_service.dto.OrderResponse;
import br.com.acasadotenis.pedidos_service.dto.TrackingResponse;
import br.com.acasadotenis.pedidos_service.dto.UpdateStatusRequest;
import br.com.acasadotenis.pedidos_service.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class OrderService {

    /** Pedidos que ainda dependem do trabalho da loja. */
    public static final Set<OrderStatus> ACTIVE = EnumSet.of(OrderStatus.RECEIVED, OrderStatus.CLEANING, OrderStatus.RESTORING);

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private final OrderRepository repository;
    private final Clock clock;

    public OrderService(OrderRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Lista do painel: em andamento primeiro, pelos prazos mais curtos. */
    @Transactional(readOnly = true)
    public List<OrderResponse> list() {
        return repository.findAll().stream()
                .sorted(Comparator
                        .comparing((Order o) -> !ACTIVE.contains(o.getStatus()))
                        .thenComparing(Order::getExpectedAt))
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(String code) {
        return OrderResponse.from(find(code));
    }

    /** Consulta pública do cliente. */
    @Transactional(readOnly = true)
    public TrackingResponse track(String code) {
        return TrackingResponse.from(find(code));
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest req) {
        Instant now = clock.instant();
        Order order = new Order();
        order.setCustomerName(req.customerName().trim());
        order.setCustomerPhone(req.customerPhone());
        order.setCustomerCep(formatCep(req.customerCep()));
        order.setExpectedAt(req.expectedAt());
        order.setNotes(req.notes());
        order.setReceivedAt(now);
        req.items().forEach(i -> {
            OrderItem item = new OrderItem();
            item.setShoe(i.shoe().trim());
            item.setService(i.service().trim());
            item.setPrice(i.price());
            item.setDetail(i.detail());
            order.addItem(item);
        });
        order.changeStatus(OrderStatus.RECEIVED, "Tênis recebido na loja", now);

        Order saved = repository.save(order);
        // Código no formato NX-AAAA-000123, a partir do id gerado pelo banco
        saved.setCode(String.format(Locale.ROOT, "NX-%d-%06d", now.atZone(SAO_PAULO).getYear(), saved.getId()));
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse changeStatus(String code, UpdateStatusRequest req) {
        Order order = find(code);
        if (order.getStatus() != req.status()) {
            String note = req.note() == null || req.note().isBlank() ? "Status atualizado pela equipe" : req.note();
            order.changeStatus(req.status(), note, clock.instant());
        }
        return OrderResponse.from(order);
    }

    private Order find(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return repository.findByCode(normalized).orElseThrow(() -> new OrderNotFoundException(normalized));
    }

    private static String formatCep(String cep) {
        String digits = cep.replaceAll("\\D", "");
        return digits.substring(0, 5) + "-" + digits.substring(5);
    }
}
