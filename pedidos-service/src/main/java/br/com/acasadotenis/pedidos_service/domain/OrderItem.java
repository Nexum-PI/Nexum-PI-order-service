package br.com.acasadotenis.pedidos_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Item do pedido (tabela order_items): um tênis e o serviço feito nele. */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 120)
    private String shoe;

    @Column(nullable = false, length = 60)
    private String service;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Detalhe do serviço (material, cor desejada, tipo de solado...). */
    @Column(length = 120)
    private String detail;
}
