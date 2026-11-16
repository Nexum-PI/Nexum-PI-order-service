# pedidos-service · NEXUM (A Casa do Tênis)

API de pedidos do sistema NEXUM. Spring Boot 4 + JPA. Segue o dicionário de dados:
tabelas `orders`, `order_items` e `order_status_history`.

## Rodar localmente

Não precisa instalar banco: por padrão usa **H2 em memória**, com 5 pedidos de exemplo.

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Login da equipe em desenvolvimento:
usuário `equipe`, senha `nexum-dev` (troque com `NEXUM_STAFF_USER` / `NEXUM_STAFF_PASSWORD`).

### Com o MySQL (Aiven)

Copie `.env.example` para `.env`, preencha e rode com o perfil `mysql`.
O `.env` está no `.gitignore`: **nunca commite senhas**.

## Endpoints

| Método | Rota | Acesso | O que faz |
|---|---|---|---|
| GET | `/api/orders/{code}/tracking` | **Público** | Consulta do cliente: primeiro nome, status, datas, serviços e histórico. Sem preço, telefone ou CEP. |
| GET | `/api/orders` | Equipe | Lista completa (em andamento primeiro, por prazo), com valores. |
| GET | `/api/orders/{code}` | Equipe | Detalhe de um pedido. |
| POST | `/api/orders` | Equipe | Abre ordem de serviço. Gera o código `NX-AAAA-000123`. |
| PATCH | `/api/orders/{code}/status` | Equipe | Muda o status e grava o histórico. |

Status: `RECEIVED`, `CLEANING`, `RESTORING`, `READY`, `DELIVERED`, `CANCELED`.
"Atrasado" e "Prazo próximo" não são status: são calculados pela previsão de entrega.

Exemplo de criação:

```bash
curl -u equipe:nexum-dev -H "Content-Type: application/json" -X POST localhost:8080/api/orders -d '{
  "customerName": "Joana Prado", "customerPhone": "(11) 90000-0000", "customerCep": "01310-100",
  "expectedAt": "2026-12-01",
  "items": [{ "shoe": "Cano baixo de couro", "service": "Lavagem Premium", "price": 150.00, "detail": "Couro" }]
}'
```

Erros seguem o formato Problem Details, com mensagens em português
(ex.: `400` com `errors.customerCep = "CEP deve ter 8 números"`).

## Segurança (etapa atual)

Login HTTP Basic com um usuário da equipe. Próximo passo: **auth-service com JWT** e perfis
`ADMIN`, `ATTENDANT` e `TECHNICIAN`, sem mudar as rotas.

## Testes

```bash
./mvnw test
```
