package br.com.acasadotenis.pedidos_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiTest {

    @Autowired
    MockMvc mvc;

    private static final String NEW_ORDER = """
            {"customerName":"Joana Prado Lima","customerPhone":"(11) 90000-0000","customerCep":"01310100",
             "expectedAt":"2030-01-10",
             "items":[{"shoe":"Cano baixo de couro","service":"Lavagem Premium","price":150.00,"detail":"Couro"}]}
            """;

    @Test
    void consultaDoClienteEPublicaEMostraSoOPrimeiroNomeSemPreco() throws Exception {
        mvc.perform(get("/api/orders/nx-2026-000123/tracking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NX-2026-000123"))
                .andExpect(jsonPath("$.firstName").value("Carlos"))
                .andExpect(jsonPath("$.items[0].price").doesNotExist())
                .andExpect(jsonPath("$.customerCep").doesNotExist())
                .andExpect(jsonPath("$.history[0].status").value("RECEIVED"));
    }

    @Test
    void pedidoInexistenteDevolve404() throws Exception {
        mvc.perform(get("/api/orders/NX-0000/tracking"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Pedido não encontrado"));
    }

    @Test
    void listarCriarEMudarStatusExigemLoginDaEquipe() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(NEW_ORDER))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/orders/NX-2026-000124/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLEANING\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders").with(httpBasic("equipe", "senha-errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void equipeListaComEmAndamentoPrimeiroEComValores() throws Exception {
        mvc.perform(get("/api/orders").with(httpBasic("equipe", "nexum-dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$[0].status", is(in(new String[] {"RECEIVED", "CLEANING", "RESTORING"}))))
                .andExpect(jsonPath("$[0].total").exists());
    }

    @Test
    void equipeCriaPedidoComCodigoCepFormatadoEHistorico() throws Exception {
        mvc.perform(post("/api/orders").with(httpBasic("equipe", "nexum-dev"))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_ORDER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", matchesPattern("NX-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.customerCep").value("01310-100"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.total").value(150.0))
                .andExpect(jsonPath("$.history", hasSize(1)));
    }

    @Test
    void validaCamposObrigatoriosValorECep() throws Exception {
        String invalido = """
                {"customerName":"","customerCep":"123","expectedAt":"2030-01-10",
                 "items":[{"shoe":"X","service":"Lavagem Premium","price":0}]}
                """;
        mvc.perform(post("/api/orders").with(httpBasic("equipe", "nexum-dev"))
                        .contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerName").exists())
                .andExpect(jsonPath("$.errors.customerCep").value("CEP deve ter 8 números"))
                .andExpect(jsonPath("$.errors['items[0].price']").exists());
    }

    @Test
    void mudarStatusGravaHistoricoEReflecteNaConsultaDoCliente() throws Exception {
        mvc.perform(patch("/api/orders/NX-2026-000126/status").with(httpBasic("equipe", "nexum-dev"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.deliveredAt").exists())
                .andExpect(jsonPath("$.history[-1].note").value("Status atualizado pela equipe"));

        mvc.perform(get("/api/orders/NX-2026-000126/tracking"))
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }
}
