package br.com.acasadotenis.notificacao;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Notificação gerada para o cliente a partir de um evento de pedido.
 * Hoje fica registrada (status PENDING) e aparece no painel; o envio real
 * por WhatsApp/e-mail é o próximo passo.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, length = 20)
    private String orderCode;

    @Column(name = "customer_name", length = 120)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "order_status", length = 20)
    private String orderStatus;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, length = 20)
    private String channel = "WHATSAPP";

    @Column(name = "delivery_status", nullable = false, length = 20)
    private String deliveryStatus = "PENDING";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String v) { this.orderCode = v; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { this.customerName = v; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String v) { this.customerPhone = v; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String v) { this.orderStatus = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public String getChannel() { return channel; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public Instant getCreatedAt() { return createdAt; }
}
