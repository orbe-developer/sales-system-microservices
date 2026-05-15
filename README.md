# Sales System — Microservices Architecture

A sales platform built as a full microservices system using Spring Boot 3 and Spring Cloud. Designed as a working sandbox for production patterns: event-driven communication, distributed tracing, circuit breaking, OAuth2 security, and a complete observability stack.

**Stack:** Java 17 · Spring Boot 3.1.3 · Spring Cloud 2022.0.4 · Kafka · Keycloak · Docker

---

## Architecture

```
                        ┌─────────────────┐
                        │    Keycloak     │
                        │  (OAuth2 IdP)   │
                        └────────┬────────┘
                                 │ JWT validation
                        ┌────────▼────────┐
           Client ────► │   API Gateway   │ :8080
                        │ Spring Cloud GW │
                        └──┬──────┬───┬──┘
                           │      │   │
              ┌────────────┘      │   └──────────────┐
              │                   │                  │
     ┌────────▼───────┐  ┌────────▼───────┐  ┌──────▼──────────┐
     │ Orders Service │  │ Inventory Svc  │  │ Products Service │
     │    (MySQL)     │  │  (PostgreSQL)  │  │   (PostgreSQL)   │
     └────────┬───────┘  └────────────────┘  └─────────────────┘
              │ REST (stock check)
              └──────────────────────────────────────►
              │ Kafka (orders-topic)
     ┌────────▼───────┐
     │  Notification  │
     │    Service     │
     └────────────────┘

     All services register with Eureka (Discovery Server :8761)
     All services emit metrics → Prometheus → Grafana
     All services send traces → Zipkin
```

---

## Services

| Service | Port | Database | Responsibility |
|---------|------|----------|----------------|
| `discovery-server` | 8761 | — | Eureka service registry |
| `api-gateway` | 8080 | — | Request routing, OAuth2 authentication |
| `products-service` | dynamic | PostgreSQL | Product catalogue (CRUD) |
| `inventory-service` | dynamic | PostgreSQL | Stock availability checks |
| `orders-service` | dynamic | MySQL | Order creation, publishes events to Kafka |
| `notification-service` | dynamic | — | Consumes order events from Kafka |

---

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Framework | Spring Boot | 3.1.3 |
| Cloud | Spring Cloud | 2022.0.4 |
| Language | Java | 17 |
| Service discovery | Netflix Eureka | — |
| API gateway | Spring Cloud Gateway | — |
| Message broker | Apache Kafka (Confluent) | 7.5.0 |
| Identity provider | Keycloak | 22.0.3 |
| Resilience | Resilience4j | — |
| Distributed tracing | Zipkin + Micrometer Brave | 2.24.3 |
| Metrics | Prometheus | 2.47.0 |
| Dashboards | Grafana | 10.1.2 |
| Databases | MySQL 8.0 / PostgreSQL 15 | — |
| Build | Maven | — |

---

## Communication Patterns

**Synchronous (REST):**
- API Gateway routes to all services via load-balanced URIs (`lb://service-name`)
- Orders Service calls Inventory Service (stock validation before order creation)

**Asynchronous (Kafka):**
- Orders Service publishes `OrderEvent` to topic `orders-topic`
- Notification Service consumes from `orders-topic` (consumer group: `notification-service`)

**Resilience:**
- Circuit breaker on Orders Service (Resilience4j)
  - Sliding window: 5 requests
  - Failure threshold: 50%
  - Wait in open state: 10s
  - Half-open permitted calls: 3

---

## API Endpoints

All requests go through the API Gateway on port `8080`. Authentication requires a Bearer token issued by Keycloak.

### Products
| Method | Path | Role required |
|--------|------|---------------|
| `GET` | `/api/v1/products` | `ROLE_USER` |
| `POST` | `/api/v1/products` | `ROLE_ADMIN` |

### Orders
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/orders` | Create order (validates stock, publishes event) |
| `GET` | `/api/v1/orders` | List all orders |

### Inventory
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/inventory/{sku}` | Check single product stock |
| `POST` | `/api/v1/inventory/in-stock` | Check multiple products stock |

---

## Observability

| Tool | URL | Credentials |
|------|-----|-------------|
| Eureka Dashboard | http://localhost:8761 | eureka / password |
| Keycloak Admin | http://localhost:8181 | admin / admin |
| Zipkin | http://localhost:9411 | — |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | admin / admin |

Distributed tracing is enabled across all services with 100% sampling. Each service exposes Prometheus metrics at `/actuator/{service}/prometheus`.

---

## Running Locally

### Prerequisites
- Docker and Docker Compose
- Java 17
- Maven

### 1. Start infrastructure

```bash
docker compose up -d
```

This starts: PostgreSQL (×3), MySQL, Keycloak, Kafka, Zookeeper, Zipkin, Prometheus, Grafana.

### 2. Configure Keycloak

- Go to http://localhost:8181 (admin / admin)
- Create realm: `microservices-realm`
- Create client: `microservices_client`
- Create roles: `ROLE_USER`, `ROLE_ADMIN`
- Create a test user and assign roles

### 3. Start services

Run each in a separate terminal or from your IDE. Start in this order:

```bash
mvn -pl discovery-server spring-boot:run
mvn -pl api-gateway spring-boot:run
mvn -pl products-service spring-boot:run
mvn -pl inventory-service spring-boot:run
mvn -pl orders-service spring-boot:run
mvn -pl notification-service spring-boot:run
```

### 4. Verify registration

Open http://localhost:8761 — all services should appear in the Eureka registry before sending requests.

---

## Database Configuration

| Service | Engine | Port | Database |
|---------|--------|------|----------|
| Orders | MySQL 8.0 | 3366 | ms_orders |
| Inventory | PostgreSQL 15 | 5431 | ms_inventory |
| Products | PostgreSQL 15 | 5432 | ms_products |
| Keycloak | PostgreSQL 15 | 5433 | dbkeycloak |

Schemas are auto-generated by Hibernate on first run (`ddl-auto=update`).

---

## Project Structure

```
sales-system-microservices/
├── api-gateway/
├── discovery-server/
├── inventory-service/
├── notification-service/
├── orders-service/
├── products-service/
├── files/
│   ├── prometheus.yml
│   └── grafana/
├── docker-compose.yml
└── pom.xml                 ← parent POM
```
