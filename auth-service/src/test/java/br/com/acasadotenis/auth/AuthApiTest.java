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

    private static final String REGISTER = """
            {"name":"Mariana Albuquerque","email":"Mariana@Email.com","phone":"(11) 90000-0000",
             "cep":"01310100","password":"senha-forte-1","acceptTerms":true}""";

    @Test
    void clienteCriaContaEJaEntraComPerfilClient() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.role").value("CLIENT"))
                .andExpect(jsonPath("$.user.email").value("mariana@email.com"))
                .andExpect(jsonPath("$.user.cep").value("01310-100"));
        login("mariana@email.com", "senha-forte-1");
    }

    @Test
    void cadastroNaoAceitaEscolherPerfilNemEmailRepetido() throws Exception {
        // o corpo pode até trazer "role", mas ele é ignorado: a conta é sempre CLIENT
        String comRole = REGISTER.replace("Mariana@Email.com", "bruno@email.com")
                .replace("\"acceptTerms\":true", "\"acceptTerms\":true,\"role\":\"ADMIN\"");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(comRole))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CLIENT"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(comRole))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Já existe uma conta com esse e-mail. Tente entrar."));
    }

    @Test
    void cadastroValidaCamposETermos() throws Exception {
        String invalido = """
                {"name":"","email":"nao-e-email","phone":"123","cep":"12","password":"curta","acceptTerms":false}""";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.cep").value("CEP deve ter 8 números"))
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.acceptTerms").value("É preciso aceitar a política de privacidade"));
    }

    @Test
    void clienteNaoAcessaEquipeEEquipeNaoMostraClientes() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER.replace("Mariana@Email.com", "carla@email.com")))
                .andReturn().getResponse().getContentAsString();
        String cliente = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mvc.perform(get("/api/staff").header("Authorization", "Bearer " + cliente))
                .andExpect(status().isForbidden());

        String admin = login("admin@acasadotenis.com.br", "nexum-admin");
        mvc.perform(get("/api/staff").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", not(hasItem("carla@email.com"))));
        // a equipe também não cria conta de cliente por esse caminho
        mvc.perform(post("/api/staff").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"x@loja.com\",\"role\":\"CLIENT\",\"password\":\"senha-forte-1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clienteEditaOsProprioDadosETrocaASenha() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER.replace("Mariana@Email.com", "dora@email.com")))
                .andReturn().getResponse().getContentAsString();
        String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mvc.perform(put("/api/auth/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dora Nova\",\"phone\":\"11988887777\",\"cep\":\"04538-132\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dora Nova"))
                .andExpect(jsonPath("$.cep").value("04538-132"));

        mvc.perform(patch("/api/auth/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"errada\",\"newPassword\":\"outra-senha-9\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/auth/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"senha-forte-1\",\"newPassword\":\"outra-senha-9\"}"))
                .andExpect(status().isNoContent());
        login("dora@email.com", "outra-senha-9");
    }
}
