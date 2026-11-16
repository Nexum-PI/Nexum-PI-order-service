package br.com.acasadotenis.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    MockMvc mvc;

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void loginDoAdminDevolveTokenEMe() throws Exception {
        String token = login("admin@acasadotenis.com.br", "nexum-admin");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void senhaErradaDevolve401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@acasadotenis.com.br\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("E-mail ou senha incorretos."));
    }

    @Test
    void adminCadastraFuncionarioQueConsegueEntrarMasNaoGerenciaEquipe() throws Exception {
        String admin = login("admin@acasadotenis.com.br", "nexum-admin");
        mvc.perform(post("/api/staff").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bia Técnica\",\"email\":\"Bia@Loja.com\",\"role\":\"TECHNICIAN\",\"password\":\"senha-forte-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("bia@loja.com"));

        String tecnico = login("bia@loja.com", "senha-forte-1");
        mvc.perform(get("/api/staff").header("Authorization", "Bearer " + tecnico))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/staff").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("bia@loja.com")));
    }

    @Test
    void semTokenNaoAcessa() throws Exception {
        mvc.perform(get("/api/staff")).andExpect(status().isUnauthorized());
    }
}
