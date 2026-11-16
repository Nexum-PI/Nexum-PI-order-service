package br.com.acasadotenis.pedidos_service.service;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String code) {
        super("Pedido não encontrado: " + code);
    }
}
