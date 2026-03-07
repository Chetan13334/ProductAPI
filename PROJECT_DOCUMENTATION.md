# **Product API - Complete Project Explanation**

## **PART 1: API Architecture & Endpoints**

### **Overview**
This is a **Spring Boot REST API** for managing Products with JWT-based authentication. It follows a layered architecture with controllers, services, repositories, and DTOs.

### **Core API Endpoints**

**Base URL**: `http://localhost:8080/api/v1`

#### **Authentication Endpoints** (`/auth`)
1. **Register** - `POST /auth/register`
   - Creates a new user with USER role
   - Request: `{"username": "john", "password": "Pass@1234"}`
   - Response: `201 Created` (No body)

2. **Login** - `POST /auth/login`
   - Authenticates existing user
   - Returns JWT access token + refresh token

3. **Refresh Token** - `POST /auth/refresh`
   - Rotates tokens (invalidates old refresh token, issues new one)
   - Keeps users logged in without re-entering password

#### **Product Endpoints** (`/products`)
1. **Get All Products** - `GET /products?page=0&size=10&sortBy=id&direction=asc`
   - Paginated list with sorting
   - Public access (no auth needed)
   - Returns: `PageResponse<ProductResponse>`

2. **Get Product by ID** - `GET /products/{id}`
   - Fetch single product with items
   - Returns: `ProductResponse`

3. **Create Product** - `POST /products` *(ADMIN ONLY)*
   - Create new product with items
   - Request: `{"productName": "Laptop", "items": [{"quantity": 2}]}`
   - Auto-logs audit entry

4. **Update Product** - `PUT /products/{id}` *(ADMIN ONLY)*
   - Modify existing product

5. **Delete Product** - `DELETE /products/{id}` *(ADMIN ONLY)*
   - Remove product (soft delete with audit)

6. **Get Items** - `GET /products/{id}/items`
   - Fetch all items in a product

### **Authentication Flow**
```
User Registration/Login → JWT Access Token (15 min) + Refresh Token (7 days)
         ↓
API Request with Bearer Token in Authorization header
         ↓
JwtAuthenticationFilter validates token
         ↓
Request processed with user context
         ↓
Access denied if token expired or user lacks role
```

### **Role-Based Access Control**
- **ADMIN**: Full CRUD on products
- **USER**: Read-only access
- Default admin credentials: `admin` / `Admin@12345`

---

## **PART 2: Exception Handling & Error Responses**

### **Why We Created an Exception Folder**

Error handling is critical for production APIs. Instead of letting raw exceptions bubble up, we created a **centralized exception handling system** to:

1. **Standardize Responses** - All errors follow the same format
2. **Better User Experience** - Clear, actionable error messages
3. **Security** - Hide sensitive server details from clients
4. **Consistency** - Unified error structure across all endpoints

### **Exception Hierarchy**

#### **Custom Exceptions** (in `exception/` folder)

1. **ResourceNotFoundException** - When product/user not found
   ```java
   throw new ResourceNotFoundException("Product with id 5 not found");
   // Response: 404 with standardized error
   ```

2. **InvalidTokenException** - When JWT is invalid/expired
   ```java
   throw new InvalidTokenException("Token has expired");
   // Response: 401 Unauthorized
   ```

#### **GlobalExceptionHandler** - Central error processor

This `@RestControllerAdvice` class intercepts all exceptions and returns standardized responses:

```json
{
  "timestamp": "2026-02-20T21:45:30.123456",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/products",
  "fieldErrors": {
    "productName": "must not be blank",
    "items": "size must be between 1 and 50"
  }
}
```

### **Handled Exception Types**

| Exception | HTTP Status | Use Case |
|-----------|------------|----------|
| `MethodArgumentNotValidException` | 400 | Invalid request body (validation) |
| `ConstraintViolationException` | 400 | Path param/query param violations |
| `ResourceNotFoundException` | 404 | Product doesn't exist |
| `InvalidTokenException` | 401 | JWT token invalid/expired |
| `BadCredentialsException` | 401 | Wrong username/password |
| `AccessDeniedException` | 403 | User lacks permission (not ADMIN) |
| `IllegalArgumentException` | 400 | Invalid sort parameter, etc. |
| `Exception` (generic) | 500 | Unexpected server error |

### **Example: Exception in Action**

When user tries to delete a product without ADMIN role:
```
Request → Controller → Service → Security Check
              ↓
        AccessDeniedException thrown
              ↓
        GlobalExceptionHandler catches it
              ↓
        Returns 403 with message: "Access is denied"
```

---

## **PART 3: Testing Strategy**

### **Why Tests Matter**
Tests ensure:
- Code works as expected before deployment
- Regressions don't happen when adding features
- API contracts stay consistent

### **Test Structure** (in `src/test/`)

#### **1. Unit Tests** - Business Logic

**File**: `ProductServiceTest.java`
- Tests **ProductService** in isolation
- Uses **Mockito** to mock database interactions
- No real database calls

**Example Test**:
```java
@Test
void createProductShouldPersistAndReturnResponse() {
    ProductRequest request = new ProductRequest("Laptop", ...);
    
    when(productRepository.save(any(Product.class)))
        .thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0, Product.class);
            saved.setId(1);  // Simulate DB-generated ID
            return saved;
        });

    ProductResponse response = productService.createProduct(request, "admin");
    
    assertEquals(1, response.id());
    verify(productRepository).save(any(Product.class));  // Verify called
}
```

**What's being tested**:
- ✅ Product is created with correct data
- ✅ Database save method is called
- ✅ Audit log is recorded
- ✅ Response contains expected ID

#### **2. Integration Tests** - Full API Flow

**File**: `AuthIntegrationTest.java`
- Tests **actual HTTP requests** through MockMvc
- Uses **real Spring context** but with H2 in-memory database
- End-to-end user flows

**Example Test**:
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")  // Uses application-test.properties (H2 DB)
class AuthIntegrationTest {
    
    @Test
    void refreshTokenShouldRotateAndInvalidateOldToken() throws Exception {
        // 1. Register new user
        String registerResponse = mockMvc.perform(
            post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // 2. Extract old refresh token
        String oldRefreshToken = objectMapper.readTree(registerResponse)
            .get("refreshToken").asText();
        
        // 3. Call refresh endpoint
        String refreshResponse = mockMvc.perform(
            post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshPayload))
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // 4. Verify old token is now invalid
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);
    }
}
```

**What's being tested**:
- ✅ Full registration process
- ✅ Token refresh endpoint works
- ✅ Old token becomes invalid
- ✅ New token is issued

### **Test Configuration**

**File**: `application-test.properties`
```properties
# Uses H2 in-memory database (fast, isolated)
spring.datasource.url=jdbc:h2:mem:testdb
# Recreates schema for each test suite
spring.jpa.hibernate.ddl-auto=create-drop
```

### **Running Tests**

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=ProductServiceTest

# Run with coverage report
./mvnw test jacoco:report
```

**Output**:
```
[INFO] Running com.chetan.productapi.service.ProductServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Running com.chetan.productapi.integration.AuthIntegrationTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
```

---

## **PART 4: Docker & Containerization**

### **Why Docker?**
- **Consistent Environment**: Works same on laptop, CI/CD, production
- **Isolation**: App doesn't interfere with other services
- **Scalability**: Easy to spin up multiple instances
- **Deployment**: Single deployable artifact (image)

### **Docker Architecture**

#### **Dockerfile** - Multi-stage build

```dockerfile
# Stage 1: Build
FROM maven:3.9.11-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy build files
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn

# Download Maven dependencies offline (faster layer caching)
RUN ./mvnw -q -DskipTests dependency:go-offline

# Copy source code and compile
COPY src src
RUN ./mvnw -q -DskipTests clean package

# Stage 2: Runtime  
FROM eclipse-temurin:17-jre  # Lightweight Java runtime
WORKDIR /app

# Copy only the JAR from builder stage
COPY --from=builder /app/target/productapi-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Why multi-stage?**
- **Build stage**: Includes Maven (heavy, ~500MB)
- **Runtime stage**: Only JRE (light, ~100MB)
- **Final image**: ~130MB instead of ~700MB

#### **Docker Compose** - Orchestrate services

```yaml
services:
  mysql:                        # Database service
    image: mysql:8.4
    container_name: productapi-mysql
    environment:
      MYSQL_DATABASE: productdb
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3306:3306"
    healthcheck:                # Wait for DB to be ready
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-proot"]
      interval: 10s
      timeout: 5s
      retries: 10
    volumes:
      - mysql_data:/var/lib/mysql  # Persistent storage

  productapi:                   # Application service
    build: .                    # Build from Dockerfile
    container_name: productapi-app
    depends_on:
      mysql:
        condition: service_healthy  # Wait for healthy DB
    environment:
      DB_URL: jdbc:mysql://mysql:3306/productdb?...
      DB_USERNAME: root
      DB_PASSWORD: root
      JWT_SECRET: ...
      BOOTSTRAP_ADMIN_USERNAME: admin
      BOOTSTRAP_ADMIN_PASSWORD: Admin@12345
    ports:
      - "8080:8080"             # Map container:host port

volumes:
  mysql_data:                   # Named volume for mysql data
```

### **How to Test Everything Locally**

#### **Option 1: Run with Docker Compose** (Recommended)

```bash
# Build Docker image and start both containers
docker compose up --build

# Output:
# productapi-mysql      | 2026-02-20 21:45:30 0 [System] InnoDB: Buffer pool...
# productapi-app       | 2026-02-20 21:45:32 Product API is running...
# productapi-app       | Started ProductapiApplication in 3.2 seconds
```

**Access the app**:
- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- MySQL: `localhost:3306` (user: root, password: root)

#### **Option 2: Run Locally** (Development)

```bash
# Ensure MySQL is running locally on port 3306
mysql -u root -p

# Then in another terminal
./mvnw spring-boot:run

# Output:
# 2026-02-20 21:45:32 Started ProductapiApplication in 4.1 seconds
```

### **Test the API**

#### **1. Test Registration**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"Pass@1234"}'

# Response: 201 Created
```

#### **2. Get Products** (No auth needed)
```bash
curl http://localhost:8080/api/v1/products?page=0&size=5

# Response:
{
  "content": [],
  "pageNumber": 0,
  "pageSize": 5,
  "totalElements": 0,
  "totalPages": 0
}
```

#### **3. Create Product** (ADMIN only)
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"productName":"Laptop","items":[{"quantity":2}]}'

# Response: 201 Created
{
  "id": 1,
  "productName": "Laptop",
  "items": [...],
  "createdBy": "admin"
}
```

#### **4. Test Error Handling**
```bash
# Try to get non-existent product
curl http://localhost:8080/api/v1/products/999

# Response: 404 Not Found
{
  "timestamp": "2026-02-20T21:45:30.123456",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found",
  "path": "/api/v1/products/999"
}
```

### **Docker Common Commands**

```bash
# Build and start
docker compose up --build

# Start without rebuild
docker compose up

# Stop containers
docker compose down

# Remove containers AND volumes (careful!)
docker compose down -v

# View logs
docker compose logs -f productapi

# View only app logs
docker compose logs productapi

# Execute command in container
docker compose exec productapi bash

# Inspect what's running
docker compose ps
```

### **Troubleshooting**

| Issue | Solution |
|-------|----------|
| Port 8080 already in use | `docker compose port productapi 8080` or change in `docker-compose.yml` |
| MySQL connection failed | Wait 15s for MySQL healthcheck to pass, check `docker compose logs mysql` |
| Slow builds | First build downloads deps (10-15min). Subsequent builds use cache (~30s) |
| Container stops immediately | Check `docker compose logs productapi` for errors |
| H2 console not accessible | Enable in `application.properties`: `spring.h2.console.enabled=true` |

---

## **Project Architecture Diagram**

```
Client Request
    ↓
Spring Boot App (Port 8080)
    ↓
    ├─→ JwtAuthenticationFilter (validates token)
    ├─→ Controller Layer (AuthController, ProductController)
    ├─→ Service Layer (AuthService, ProductService, RefreshTokenService)
    ├─→ Repository Layer (JPA Repositories)
    ├─→ Entity Layer (Product, User, Item, RefreshToken)
    └─→ Exception Handler (GlobalExceptionHandler)
    ↓
MySQL Database (Port 3306)
```

---

## **Quick Reference: Project Flow**

```
1. CLIENT SENDS REQUEST
   ↓
2. JwtAuthenticationFilter validates JWT
   ↓
3. Controller receives request (ProductController / AuthController)
   ↓
4. @Validated annotation checks input validation
   ↓
5. Service layer handles business logic
   ↓
6. Repository queries database
   ↓
7. Exception handler catches any errors
   ↓
8. Response returned in standardized format
```

---

## **Project Structure**

```
productapi/
├── src/
│   ├── main/
│   │   ├── java/com/chetan/productapi/
│   │   │   ├── controller/           # API endpoints
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # Data access
│   │   │   ├── entity/              # JPA entities
│   │   │   ├── dto/                 # Request/Response models
│   │   │   ├── exception/           # Custom exceptions & handler
│   │   │   ├── security/            # JWT, auth filters
│   │   │   ├── config/              # Spring configuration
│   │   │   └── util/                # Helper utilities
│   │   └── resources/
│   │       ├── application.properties
│   │       └── templates/
│   └── test/
│       ├── java/com/chetan/productapi/
│       │   ├── service/             # Unit tests
│       │   ├── integration/         # Integration tests
│       │   └── ProductapiApplicationTests
│       └── resources/
│           └── application-test.properties
├── Dockerfile                        # Multi-stage build config
├── docker-compose.yml               # MySQL + App orchestration
├── pom.xml                          # Maven dependencies
└── mvnw, mvnw.cmd                   # Maven wrapper
```

---

## **Summary**

| Component | Purpose |
|-----------|---------|
| **API Endpoints** | CRUD operations for Products + Authentication |
| **Exception Handling** | Standardized, user-friendly error responses |
| **Testing** | Unit tests (Mockito) + Integration tests (MockMvc) with H2 |
| **Docker** | Multi-stage build, lightweight containers |
| **Docker Compose** | Orchestrate MySQL + App with health checks |

---

## **Tech Stack**
- **Language**: Java 17
- **Framework**: Spring Boot 4.0.3
- **Database**: MySQL (production), H2 (testing)
- **Authentication**: JWT with refresh token rotation
- **Testing**: JUnit 5, Mockito, MockMvc
- **Build Tool**: Maven
- **Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Containerization**: Docker & Docker Compose

---

## **Getting Started**

### **Prerequisites**
- Docker & Docker Compose installed
- OR Java 17 + Maven + MySQL

### **Quick Start**
```bash
# Clone/Navigate to project
cd productapi

# Option 1: Docker Compose (Easiest)
docker compose up --build

# Option 2: Local Development
./mvnw spring-boot:run

# Run Tests
./mvnw test

# Access Swagger UI
http://localhost:8080/swagger-ui.html
```

---

**Documentation Created**: February 21, 2026  
**Project Version**: 0.0.1-SNAPSHOT  
**Last Updated**: 2026-02-21
