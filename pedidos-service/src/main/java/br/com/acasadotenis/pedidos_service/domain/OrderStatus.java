package br.com.acasadotenis.pedidos_service.domain;

/**
 * Status do pedido (enum orders.status do dicionário de dados).
 * "Atrasado" e "Prazo próximo" NÃO são status: são calculados pela previsão de entrega.
 */
public enum OrderStatus {
    RECEIVED,   // Recebido
    CLEANING,   // Em higienização
    RESTORING,  // Em restauração
    READY,      // Pronto para retirada/entrega
    DELIVERED,  // Entregue
    CANCELED    // Cancelado
}
