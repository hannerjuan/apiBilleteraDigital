# BillPay - Core Banking & Financial Transfer API

API RESTful bancaria de alto rendimiento desarrollada con **Spring Boot 3.3.4** y **Java 21**, diseñada bajo estándares corporativos y financieros para la gestión de clientes, cuentas de ahorro de 10 dígitos, depósitos, retiros y transferencias monetarias entre cuentas (P2P) con **garantía ACID estricta**, bloqueo pesimista contra **doble gasto (double spending)** y respuestas de error bajo el estándar **RFC 7807 (Problem Details)**.

---

## 🛠️ Stack Tecnológico y Arquitectura

* **Lenguaje:** Java 21 LTS
* **Framework:** Spring Boot 3.3.4
* **Persistencia:** Spring Data JPA / Hibernate 6
* **Bases de Datos:**
  * **Producción:** PostgreSQL 16
  * **Desarrollo / Testing:** H2 en modo compatibilidad PostgreSQL
* **Seguridad:** Spring Security 6 + JJWT (JSON Web Token) stateless con algoritmo HMAC-SHA256 y hashing BCrypt
* **Documentación Interactiva:** SpringDoc OpenAPI 3 / Swagger UI (`/swagger-ui.html`)
* **Validación de Entradas:** Jakarta Bean Validation (`@NotNull`, `@Positive`, `@Digits`, `@NotBlank`, `@Email`, `@Pattern`)
* **Pruebas Automatizadas:** JUnit 5, Mockito, Spring Boot Test y pruebas de concurrencia multihilo

---

## 🏛️ Modelo de Datos (PostgreSQL)

```text
+-------------------+       +-----------------------+       +------------------------+
|      USUARIOS     |       |        CUENTAS        |       |     TRANSACCIONES      |
+-------------------+       +-----------------------+       +------------------------+
| id (PK)           | 1   1 | id (PK)               | 1   * | id (PK)                |
| documento_identidad|<----->| numero_cuenta (UNIQUE)|<----->| id_cuenta_origen (FK)  |
| nombre_completo   |       | saldo (NUMERIC 15,2)  |       | id_cuenta_destino (FK) |
| email (UNIQUE)    |       | estado (ENUM)         |       | monto (NUMERIC 15,2)   |
| password_hash     |       | fecha_creacion        |       | tipo (ENUM)            |
| fecha_registro    |       +-----------------------+       | estado (ENUM)          |
+-------------------+                                       | fecha_creacion         |
                                                            +------------------------+
```

---

## ⚡ Concurrencia, ACID y Prevención de Doble Gasto

Las operaciones financieras críticas implementan:
1. **Transaccionalidad Atómica (`@Transactional(isolation = Isolation.READ_COMMITTED)`):** Toda transferencia debita la cuenta origen y acredita la cuenta destino en una sola unidad indivisible de trabajo.
2. **Bloqueo Pesimista en Base de Datos (`PESSIMISTIC_WRITE`):** Emplea `SELECT ... FOR UPDATE` a nivel de base de datos para impedir que transacciones concurrentes lean saldos desactualizados.
3. **Estrategia Anti-Deadlock:** En transferencias bidireccionales concurrentes entre las cuentas A y B, las cuentas se bloquean siempre en un orden determinista y consistente por su identificador primario (`min(idA, idB)` primero, luego `max(idA, idB)`), evitando interbloqueos cíclicos en el motor de base de datos.
4. **Validación de Invariantes:** El saldo de ninguna cuenta puede ser inferior a 0 (`CHECK (saldo >= 0)` en DDL y validación previa en dominio).

---

## 📋 Catálogo de Endpoints Principales

| Método | Endpoint | Descripción | Códigos HTTP |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Registro de nuevo cliente y apertura automática de cuenta de 10 dígitos | `201 Created`, `400 Bad Request` |
| `POST` | `/api/v1/auth/login` | Autenticación y retorno de token JWT | `200 OK`, `401 Unauthorized` |
| `GET` | `/api/v1/accounts/me` | Consulta de saldo en tiempo real y estado de la cuenta del usuario | `200 OK`, `401 Unauthorized`, `403 Forbidden` |
| `POST` | `/api/v1/accounts/deposit` | Depósito / fondeo simulado en corresponsal bancario | `200 OK`, `400 Bad Request`, `422 Unprocessable` |
| `POST` | `/api/v1/accounts/withdraw` | Extracción de saldo con límites diarios simulando cajero automático | `200 OK`, `400 Bad Request`, `422 Unprocessable` |
| `POST` | `/api/v1/transfers` | Ejecución atómica de transferencia monetaria entre dos cuentas (P2P) | `200 OK`, `400 Bad Request`, `422 Unprocessable` |
| `GET` | `/api/v1/transfers/history` | Listado paginado de movimientos con filtros por fecha y tipo | `200 OK`, `401 Unauthorized` |

---

## 📑 Manejo de Errores RFC 7807 (Problem Details)

Todas las excepciones de negocio y de validación devuelven el estándar RFC 7807 con el tipo `application/problem+json`:

```json
{
  "type": "https://api.billpay.com/errors/insufficientfundsexception",
  "title": "Fondos insuficientes",
  "status": 422,
  "detail": "Fondos insuficientes. Su saldo disponible es de $100.00 y el monto solicitado es $350.00",
  "instance": "/api/v1/transfers",
  "timestamp": "2026-09-27T16:15:30Z"
}
```

---

## 🚀 Guía de Ejecución

### 1. Requisitos Previos
* Java 21 LTS instalado (o mediante Android Studio JBR configurado en `JAVA_HOME`)
* Maven 3.9+ o el wrapper incluido (`mvnw.cmd` / `./mvnw`)
* Docker y Docker Compose (opcional, para PostgreSQL)

### 2. Ejecutar Pruebas Automatizadas
Para correr las 30 pruebas unitarias y de integración (incluyendo la prueba multihilo contra doble gasto):
```bash
./mvnw clean test
```

### 3. Iniciar la Aplicación en Modo Desarrollo (H2 In-Memory)
```bash
./mvnw spring-boot:run
```
La aplicación iniciará en `http://localhost:8080`.
* **Swagger UI interactivo:** `http://localhost:8080/swagger-ui.html`
* **Especificación OpenAPI (JSON):** `http://localhost:8080/api-docs`
* **Consola H2:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:billpaydb`, User: `sa`, Password: *(vacío)*)

### 4. Iniciar con Base de Datos PostgreSQL Real
1. Levantar el contenedor PostgreSQL:
   ```bash
   docker compose up -d
   ```
2. Ejecutar la aplicación con el perfil `prod`:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
   ```

---

## 🧪 Ejemplos de Peticiones cURL

### 1. Registro de Usuario
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "documentoIdentidad": "1098765432",
    "nombreCompleto": "Carlos Andres Perez",
    "email": "carlos.perez@example.com",
    "password": "Password123!",
    "saldoInicial": 500.00
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "carlos.perez@example.com",
    "password": "Password123!"
  }'
```

### 3. Consultar Saldo de la Cuenta
```bash
curl -X GET http://localhost:8080/api/v1/accounts/me \
  -H "Authorization: Bearer <TOKEN_JWT>"
```

### 4. Realizar Transferencia P2P
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Authorization: Bearer <TOKEN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "cuentaDestino": "9876543210",
    "monto": 150.00,
    "descripcion": "Pago de servicios"
  }'
```

### 5. Consultar Historial Paginado
```bash
curl -X GET "http://localhost:8080/api/v1/transfers/history?page=0&size=10&tipo=TRANSFERENCIA" \
  -H "Authorization: Bearer <TOKEN_JWT>"
```
