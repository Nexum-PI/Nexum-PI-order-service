package br.com.acasadotenis.pedidos_service.service;

import br.com.acasadotenis.pedidos_service.domain.Order;
import br.com.acasadotenis.pedidos_service.domain.OrderItem;
import br.com.acasadotenis.pedidos_service.domain.OrderStatus;
import br.com.acasadotenis.pedidos_service.dto.CreateOrderRequest;
import br.com.acasadotenis.pedidos_service.dto.OrderResponse;
import br.com.acasadotenis.pedidos_service.dto.TrackingResponse;
import br.com.acasadotenis.pedidos_service.dto.UpdateStatusRequest;
import br.com.acasadotenis.pedidos_service.events.OrderEvent;
import br.com.acasadotenis.pedidos_service.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository repository, Clock clock, ApplicationEventPublisher events) {
        this.repository = repository;
        this.clock = clock;
        this.events = events;
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

    /** "Meus pedidos" do cliente: mais recentes primeiro. Mostra a visão pública (sem preço). */
    @Transactional(readOnly = true)
    public List<TrackingResponse> listForAccount(String email) {
        return repository.findForAccount(email.trim().toLowerCase(Locale.ROOT)).stream()
                .sorted(Comparator.comparing(Order::getReceivedAt).reversed())
                .map(TrackingResponse::from)
                .toList();
    }

    /**
     * O cliente adiciona um pedido à própria conta pelo código. Isso só acrescenta o pedido à
     * lista dele (visão pública, sem preço); o dono do pedido no sistema não muda.
     */
    @Transactional
    public TrackingResponse claim(String email, String code) {
        Order order = find(code);
        String mine = email.trim().toLowerCase(Locale.ROOT);
        if (!mine.equalsIgnoreCase(order.getCustomerEmail())) {
            order.getFollowers().add(mine);
        }
        return TrackingResponse.from(order);
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest req) {
        Instant now = clock.instant();
        Order order = new Order();
        order.setCustomerName(req.customerName().trim());
        order.setCustomerPhone(req.customerPhone());
        if (req.customerEmail() != null && !req.customerEmail().isBlank()) {
            order.setCustomerEmail(req.customerEmail().trim().toLowerCase(Locale.ROOT));
        }
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
        events.publishEvent(OrderEvent.of("ORDER_CREATED", saved, "Tênis recebido na loja", now));
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse changeStatus(String code, UpdateStatusRequest req) {
        Order order = find(code);
        if (order.getStatus() != req.status()) {
            String note = req.note() == null || req.note().isBlank() ? "Status atualizado pela equipe" : req.note();
            Instant now = clock.instant();
            order.changeStatus(req.status(), note, now);
            events.publishEvent(OrderEvent.of("STATUS_CHANGED", order, note, now));
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
