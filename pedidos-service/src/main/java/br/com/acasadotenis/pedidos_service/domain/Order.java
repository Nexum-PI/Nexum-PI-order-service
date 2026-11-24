package br.com.acasadotenis.pedidos_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pedido (tabela orders). O cliente consulta pelo código, ex.: NX-2026-000123. */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código informado ao cliente. Gerado a partir do id ao salvar. */
    @Column(unique = true, length = 20)
    private String code;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    /** E-mail da conta do cliente (opcional). Com ele, o pedido aparece em "Meus pedidos". */
    @Column(name = "customer_email", length = 160)
    private String customerEmail;

    @Column(name = "customer_cep", length = 9)
    private String customerCep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.RECEIVED;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    /** Previsão de entrega (só o dia). */
    @Column(name = "expected_at", nullable = false)
    private LocalDate expectedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(length = 1000)
    private String notes;

    /** Contas de clientes que adicionaram o pedido pelo código (tabela order_followers). */
    @ElementCollection
    @CollectionTable(name = "order_followers", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "follower_email", length = 160)
    private Set<String> followers = new HashSet<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC, id ASC")
    private List<OrderStatusHistory> history = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    /** Toda mudança de status gera uma linha em order_status_history. */
    public void changeStatus(OrderStatus newStatus, String note, Instant when) {
        this.status = newStatus;
        this.deliveredAt = newStatus == OrderStatus.DELIVERED ? when : null;
        OrderStatusHistory entry = new OrderStatusHistory();
        entry.setOrder(this);
        entry.setStatus(newStatus);
        entry.setChangedAt(when);
        entry.setNote(note);
        history.add(entry);
    }

    public BigDecimal getTotal() {
        return items.stream().map(OrderItem::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
