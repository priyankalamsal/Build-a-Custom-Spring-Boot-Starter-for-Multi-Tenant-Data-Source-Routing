
# Multi-Tenancy Spring Boot Project

This repository contains a reusable `multitenancy-spring-boot-starter` that wires header-driven tenant routing plus a `demo-application` that consumes the starter and demonstrates tenant-scoped REST APIs and health reporting.

## Repository Layout

- `multitenancy-spring-boot-starter`: Auto-configures a `TenantAwareRoutingDataSource`, a `TenantInterceptor` that extracts `X-Tenant-ID`, and a registry of tenant `DataSource`s exposed to downstream consumers.
- `demo-application`: A Spring Boot web application that depends on the starter, exposes `POST/GET /api/users`, and adds an actuator health indicator for every configured tenant datasource.
- `db-init`: Initialization scripts executed by the PostgreSQL container to provision `tenant1_db`, `tenant2_db`, and `tenant3_db`.
- `Dockerfile`, `docker-compose.yml`, `.env.example`: Containerization layers that build the demo app, provision the database, and wire the required environment.

## Build the Starter Locally

```bash
mvn clean install
```

This builds both modules and installs the starter in the local Maven repository so that the demo module can resolve it via the multi-module build.

## Running the Application via Docker Compose

1. Copy the example environment into a live file:

   ```bash
   cp .env.example .env
   ```

2. Start the database and application together (the first run builds the demo image):

   ```bash
   docker-compose up --build
   ```

   - The PostgreSQL container runs `db-init/init-tenant-dbs.sh` to create three tenant databases.
   - The application container waits until the database is healthy, starts the Spring Boot app, and exposes `localhost:8080`.

3. When you are done, stop the stack with `docker-compose down -v`.

## Environment Variables

The `.env.example` documents all required values. At minimum:

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: Credentials consumed by Postgres and by the starter.
- `SPRING_DATASOURCE_URL_BASE`: Base JDBC URL used by every tenant (e.g., `jdbc:postgresql://db:5432/`). The tenant-specific part is appended in `demo-application/src/main/resources/application.yml`.

## API Usage

All tenant-aware API requests must include `X-Tenant-ID`. Missing headers result in `400 Bad Request`; unknown tenants return `404 Not Found`.

- **Create a User**
  ```bash
  curl -X POST http://localhost:8080/api/users \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: tenant1" \
    -d '{"name":"Alice","email":"alice@example.com"}'
  ```

- **List Users for a Tenant**
  ```bash
  curl http://localhost:8080/api/users -H "X-Tenant-ID: tenant1"
  ```

- **Fetch a Specific User**
  ```bash
  curl http://localhost:8080/api/users/1 -H "X-Tenant-ID: tenant1"
  ```

## Health Endpoints

- `GET /actuator/health`: Standard actuator health aggregated results.
- `GET /actuator/health/datasources`: Custom health indicator that checks every tenant datasource created by the starter and reports `UP` or `DOWN` per tenant.

## Multi-Tenancy Implementation Highlights

1. **Tenant Context:** `TenantContext` uses a `ThreadLocal` to store `X-Tenant-ID` for each request.
2. **Tenant Interceptor:** `TenantInterceptor` extracts and validates `X-Tenant-ID`, rejects bad requests, and clears the context after completion.
3. **Tenant Routing:** `TenantAwareRoutingDataSource` extends `AbstractRoutingDataSource` and switches between datasources registered via `TenantDataSourceRegistry`.
4. **Auto-Configuration:** `MultiTenancyAutoConfiguration` builds one `DataSource` per tenant, exposes a registry, and registers the interceptor via `WebMvcConfigurer`.

## Database Isolation

After hitting `POST /api/users` for `tenant1`, verify that only `tenant1_db` contains the new row by connecting directly to the database:

```bash
docker exec -it tenant_postgres psql -U user -d tenant1_db
```

Repeat the check against `tenant2_db` and `tenant3_db` to confirm the user is not present there.
