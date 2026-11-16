package br.com.acasadotenis.notificacao;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /api/notifications: últimas notificações (só equipe). */
@RestController
public class NotificationController {

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    public record NotificationView(Long id, String orderCode, String customerName, String orderStatus,
                                   String message, String channel, String deliveryStatus, java.time.Instant createdAt) {}

    @GetMapping("/api/notifications")
    public List<NotificationView> latest() {
        return repository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(n -> new NotificationView(n.getId(), n.getOrderCode(), n.getCustomerName(), n.getOrderStatus(),
                        n.getMessage(), n.getChannel(), n.getDeliveryStatus(), n.getCreatedAt()))
                .toList();
    }
}
