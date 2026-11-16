# pedidos-service · NEXUM (A Casa do Tênis)

API de pedidos do sistema NEXUM. Spring Boot 4 + JPA. Segue o dicionário de dados:
tabelas `orders`, `order_items` e `order_status_history`.

## Rodar

Faz parte do back-end em microsserviços: suba tudo pela raiz do repositório com
`docker compose up -d --build` (veja o README da raiz).

Sozinho, para desenvolvimento: `./mvnw spring-boot:run` (porta 8082, banco H2 em memória,
Kafka desligado). As rotas da equipe exigem o token JWT emitido pelo auth-service.

## Endpoints (pelo gateway, http://localhost:8080)

| Método | Rota | Acesso | O que faz |
|---|---|---|---|
| GET | `/api/orders/{code}/tracking` | **Público** | Consulta do cliente: primeiro nome, status, datas, serviços e histórico. Sem preço, telefone ou CEP. |
| GET | `/api/orders` | Equipe (JWT) | Lista completa (em andamento primeiro, por prazo), com valores. |
| GET | `/api/orders/{code}` | Equipe (JWT) | Detalhe de um pedido. |
| POST | `/api/orders` | Equipe (JWT) | Abre ordem de serviço. Gera o código `NX-AAAA-000123`. |
| PATCH | `/api/orders/{code}/status` | Equipe (JWT) | Muda o status, grava o histórico e publica evento no Kafka. |

Eventos no tópico `pedidos.eventos` (enviados só depois de gravar no banco):
`ORDER_CREATED` e `STATUS_CHANGED`, consumidos pelo notificacao-service.

## Testes

```bash
./mvnw test
```
