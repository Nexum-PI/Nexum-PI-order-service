package br.com.acasadotenis.auth.domain;

/** Perfis de acesso: equipe (ADMIN, ATTENDANT, TECHNICIAN) e cliente (CLIENT). */
public enum Role {
    ADMIN,      // Administrador: gerencia funcionários
    ATTENDANT,  // Atendente: abre ordens e atende clientes
    TECHNICIAN, // Técnico: atualiza o andamento na bancada
    CLIENT      // Cliente: cria a própria conta e acompanha os próprios pedidos
}
