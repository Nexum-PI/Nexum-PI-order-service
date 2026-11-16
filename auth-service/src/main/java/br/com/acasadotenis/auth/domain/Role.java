package br.com.acasadotenis.auth.domain;

/** Perfis da equipe. */
public enum Role {
    ADMIN,      // Administrador: gerencia funcionários
    ATTENDANT,  // Atendente: abre ordens e atende clientes
    TECHNICIAN  // Técnico: atualiza o andamento na bancada
}
