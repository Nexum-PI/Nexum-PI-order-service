# NEXUM · back-end (A Casa do Tênis)

Back-end em **microsserviços** com **Spring Boot 4**, **Kafka**, **Docker** e **MySQL online**.

```
                         ┌──────────────────────┐
  Front (React) ───────▶ │ gateway (nginx) :8080│
                         └──────────┬───────────┘
          ┌─────────────────────────┼──────────────────────────┐
          ▼                         ▼                          ▼
   auth-service              pedidos-service ──Kafka──▶ notificacao-service
   login JWT, equipe         pedidos, status           mensagens ao cliente
          │                         │       pedidos.eventos    │
          ▼                         ▼                          ▼
     nexum_auth              nexum_pedidos            nexum_notificacoes
                    (MySQL online: um banco por serviço)
```

| Serviço | O que faz | Rotas (pelo gateway) |
|---|---|---|
| **gateway** (nginx) | Porta única para o front, CORS | `http://localhost:8080` |
| **auth-service** | Login com JWT; cadastro de funcionários (Administrador, Atendente, Técnico) | `/api/auth/login`, `/api/auth/me`, `/api/staff` |
| **pedidos-service** | Pedidos, itens e histórico de status; publica eventos no Kafka | `/api/orders`, `/api/orders/{codigo}/tracking` (público) |
| **notificacao-service** | Consome os eventos do Kafka e registra a mensagem ao cliente | `/api/notifications` |
| **kafka** | Mensageria entre serviços (tópico `pedidos.eventos`) | interno |

## Como rodar (só isto)

Pré-requisito: **Docker Desktop** instalado e aberto.

1. Copie o arquivo de configuração e preencha (banco online, segredo do token e senha do admin):

   ```powershell
   Copy-Item .env.example .env
   ```

2. Suba tudo:

   ```bash
   docker compose up -d --build
   ```

   Na primeira vez demora alguns minutos (baixa e compila tudo). A API fica em
   **http://localhost:8080**. Depois é só rodar o front (`npm run dev`).

3. Para parar:

   ```bash
   docker compose down
   ```

**Primeiro acesso:** entre no painel com `ADMIN_EMAIL` / `ADMIN_PASSWORD` do `.env`
e cadastre a equipe na tela **Equipe**.

### Sem banco online?

Dá para usar um MySQL local em Docker. No `.env`, use `DB_HOST=mysql`, `DB_PORT=3306`,
`DB_USER=root`, `DB_SSL_MODE=DISABLED` e rode:

```bash
docker compose --profile local-db up -d --build
```

## Regras principais

- Status do pedido: `RECEIVED`, `CLEANING`, `RESTORING`, `READY`, `DELIVERED`, `CANCELED`.
  "Atrasado" e "Prazo próximo" são calculados pela previsão de entrega, não gravados.
- Toda mudança de status gera histórico **e** um evento no Kafka (enviado só depois de salvo no banco).
- Consulta do cliente é pública e mostra só o primeiro nome, sem preço, telefone ou CEP.
- Pedidos exigem login de qualquer perfil da equipe; cadastro de funcionários só **Administrador**.
- Senhas guardadas com BCrypt; tokens JWT assinados com `JWT_SECRET` (mesmo valor em todos os serviços).

## Desenvolvimento sem Docker

Cada serviço roda sozinho com banco em memória (H2): `cd pedidos-service && ./mvnw spring-boot:run`
(portas 8081 auth, 8082 pedidos, 8083 notificação). Testes: `./mvnw test` em cada pasta.

## Segurança

O `.env` está no `.gitignore`. **Nunca commite senhas.** Se uma senha já foi para o Git,
troque-a no provedor (ex.: painel da Aiven).
