# 📝 Cuestionario de Evaluación - ToolRent

## Preguntas Técnicas para la Presentación del Proyecto

---

## 🏗️ ARQUITECTURA GENERAL

### 1. ¿Qué tipo de arquitectura usa el proyecto?

**Respuesta:**
El proyecto usa una **arquitectura de 3 capas (monolítica)** con los siguientes componentes:

- **Capa de Presentación (Frontend)**: React + Vite
- **Capa de Negocio (Backend)**: Spring Boot con arquitectura MVC
- **Capa de Datos**: MySQL con JPA/Hibernate

Además, implementa **microservicios auxiliares**:
- **Keycloak** para autenticación y autorización OAuth2
- **Nginx** como proxy inverso para el frontend

---

### 2. ¿Cómo se comunican los diferentes componentes del sistema?

**Respuesta:**

```
┌─────────────┐
│   Usuario   │
└──────┬──────┘
       │ HTTP
┌──────▼──────────┐
│ Nginx (puerto 80)│
└──────┬──────────┘
       │
┌──────▼──────────────┐      ┌─────────────────┐
│   React Frontend    │◄─────┤ Keycloak        │
│   (puerto 3000)     │      │ (puerto 9090)   │
└──────┬──────────────┘      └────────▲────────┘
       │ Axios HTTP               │ OAuth2/JWT
       │ + JWT Token              │
┌──────▼──────────────┐          │
│  Spring Boot        │──────────┘
│  Backend API        │
│  (puerto 8090)      │
└──────┬──────────────┘
       │ JDBC
┌──────▼──────────────┐
│   MySQL             │
│   (puerto 3306)     │
└─────────────────────┘
```

**Protocolo de comunicación:**
1. Frontend hace peticiones HTTP/HTTPS usando **Axios**
2. Axios intercepta las peticiones y agrega el **JWT token** de Keycloak
3. Backend valida el token con Keycloak
4. Backend procesa la petición y consulta MySQL
5. Backend retorna respuesta JSON al frontend

---

## 🔗 COMUNICACIÓN FRONTEND-BACKEND

### 3. ¿Cómo y dónde se comunica el frontend con el backend?

**Respuesta:**

**Archivo clave:** `toolrent-frontend/src/http-common.js`

```javascript
// Configuración base de Axios
const SERVER = import.meta.env.VITE_PAYROLL_BACKEND_SERVER || "localhost";
const PORT = import.meta.env.VITE_PAYROLL_BACKEND_PORT || "8090";
export const API_BASE = `http://${SERVER}:${PORT}`;

const http = axios.create({
  baseURL: API_BASE,  // http://localhost:8090
  headers: { "Content-Type": "application/json" }
});
```

**Interceptor de peticiones** (agrega el token JWT automáticamente):
```javascript
http.interceptors.request.use(async (config) => {
  if (keycloak?.authenticated) {
    await keycloak.updateToken(30);
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
});
```

**Ejemplo de uso en un servicio:**

**Archivo:** `toolrent-frontend/src/services/tool.service.js`
```javascript
import http from '../http-common'

const getAll = async () => {
  const { data } = await http.get('/api/v1/tools/')
  return data
}

const create = (body) => {
  return http.post('/api/v1/tools/', body)
}
```

**Flujo completo:**
1. Componente React llama a `toolService.getAll()`
2. El servicio usa `http.get('/api/v1/tools/')`
3. Axios intercepta y agrega token JWT
4. Petición va a `http://localhost:8090/api/v1/tools/`
5. Backend (Spring Boot) recibe la petición en `ToolController`

---

### 4. ¿Qué endpoints expone el backend?

**Respuesta:**

El backend expone una API REST con los siguientes endpoints:

**TOOLS (Herramientas)**
- `GET /api/v1/tools/` - Listar herramientas (USER, ADMIN)
- `POST /api/v1/tools/` - Crear herramienta (ADMIN)
- `PUT /api/v1/tools/{id}/decommission` - Dar de baja (ADMIN)

**CLIENTS (Clientes)**
- `GET /api/v1/clients/` - Listar clientes (USER, ADMIN)
- `POST /api/v1/clients/` - Crear cliente (ADMIN)

**LOANS (Préstamos)**
- `GET /api/v1/loans/` - Listar préstamos (USER, ADMIN)
- `POST /api/v1/loans/` - Crear préstamo (ADMIN)
- `PUT /api/v1/loans/{id}/return` - Devolver préstamo (ADMIN)
- `PUT /api/v1/loans/{id}/pay-fine` - Pagar multa (ADMIN)

**KARDEX (Historial)**
- `GET /api/v1/kardex/` - Ver historial completo (USER, ADMIN)

**REPORTS (Reportes)**
- `GET /api/v1/reports/` - Generar reportes (USER, ADMIN)

**CONFIG (Configuración)**
- `GET /api/v1/config/` - Obtener configuración (USER, ADMIN)
- `PUT /api/v1/config/{id}` - Actualizar configuración (ADMIN)

---

### 5. ¿Cómo está estructurado el backend?

**Respuesta:**

El backend sigue el patrón **MVC (Model-View-Controller)** con arquitectura en capas:

```
backend-toolrent/src/main/java/com/example/demo/
│
├── Controller/          # Capa de Presentación (REST Controllers)
│   ├── ToolController.java
│   ├── ClientController.java
│   ├── LoanController.java
│   ├── KardexController.java
│   └── ReportController.java
│
├── Service/            # Capa de Lógica de Negocio
│   ├── ToolService.java
│   ├── ClientService.java
│   ├── LoanService.java
│   └── KardexService.java
│
├── Repository/         # Capa de Acceso a Datos (JPA)
│   ├── ToolRepository.java
│   ├── ClientRepository.java
│   ├── LoanRepository.java
│   └── KardexRepository.java
│
├── Entity/            # Modelos de Datos (JPA Entities)
│   ├── ToolEntity.java
│   ├── ClientEntity.java
│   ├── LoanEntity.java
│   └── KardexEntity.java
│
├── Config/            # Configuración de Spring
│   └── SecurityConfig.java
│
└── Utils/             # Utilidades
    └── KeycloakUtils.java
```

**Flujo de una petición:**
1. **Controller** recibe la petición HTTP
2. **Controller** valida permisos con `@PreAuthorize`
3. **Controller** llama al **Service**
4. **Service** ejecuta lógica de negocio
5. **Service** llama al **Repository** para acceder a la BD
6. **Repository** usa JPA/Hibernate para ejecutar queries
7. Respuesta fluye de vuelta: Repository → Service → Controller → Cliente

---

## 🔐 SEGURIDAD Y AUTENTICACIÓN

### 6. ¿Cómo funciona la autenticación en el proyecto?

**Respuesta:**

El proyecto usa **OAuth 2.0** con **Keycloak** como servidor de autenticación:

**Flujo de autenticación:**

```
1. Usuario ingresa a la aplicación
   ↓
2. Frontend (React) redirige a Keycloak
   ↓
3. Usuario ingresa credenciales en Keycloak
   ↓
4. Keycloak valida y genera JWT token
   ↓
5. Frontend recibe el token y lo almacena
   ↓
6. Cada petición al backend incluye: Authorization: Bearer <token>
   ↓
7. Backend valida el token con Keycloak
   ↓
8. Backend autoriza según roles (ADMIN, USER)
```

**Archivo de configuración frontend:** `toolrent-frontend/src/services/keycloak.js`
```javascript
import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
  url: 'http://localhost:9090',
  realm: 'sisgr-realm',
  clientId: 'sisgr-frontend'
})
```

**Archivo de configuración backend:** `application.properties`
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9090/realms/sisgr-realm
```

**Interceptor en frontend** agrega el token automáticamente:
```javascript
http.interceptors.request.use(async (config) => {
  if (keycloak?.authenticated) {
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
});
```

---

### 7. ¿Cómo se controlan los roles y permisos?

**Respuesta:**

Se usan **anotaciones de Spring Security** en los controllers:

```java
@PreAuthorize("hasAnyRole('USER','ADMIN')")  // Ambos roles
@GetMapping("/")
public List<ToolEntity> getAllTools() {
    return toolService.getAllTools();
}

@PreAuthorize("hasRole('ADMIN')")  // Solo ADMIN
@PostMapping("/")
public ResponseEntity<ToolEntity> create(@RequestBody ToolEntity body) {
    return toolService.create(body);
}
```

**Roles disponibles:**
- **ADMIN**: Puede crear, modificar, eliminar y ver todo
- **USER**: Solo puede ver información (lectura)

**Configuración en Keycloak:**
1. Se crean los roles en Keycloak Admin Console
2. Se asignan roles a usuarios
3. El JWT token incluye los roles del usuario
4. Spring Security valida los roles en cada petición

---

## 💾 BASE DE DATOS

### 8. ¿Cómo está configurada la base de datos?

**Respuesta:**

**Motor:** MySQL 8.0

**Configuración en** `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/toolrent?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci
spring.datasource.username=toolrent
spring.datasource.password=toolrent123
spring.jpa.hibernate.ddl-auto=update
```

**Parámetros importantes:**
- `useUnicode=true` - Soporte UTF-8
- `characterEncoding=UTF-8` - Encoding UTF-8
- `connectionCollation=utf8mb4_unicode_ci` - Collation para acentos
- `hibernate.ddl-auto=update` - Crea/actualiza tablas automáticamente

**Tablas principales:**
1. **tools** - Herramientas disponibles
2. **clients** - Clientes registrados
3. **loans** - Préstamos activos/históricos
4. **kardex** - Historial de movimientos
5. **config** - Configuración del sistema (multas, etc.)

---

### 9. ¿Cómo se manejan las relaciones entre tablas?

**Respuesta:**

Se usan **anotaciones JPA** para definir relaciones:

**Ejemplo en LoanEntity:**
```java
@Entity
@Table(name = "loans")
public class LoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Many-to-One con Cliente
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    // Relación Many-to-One con Herramienta
    @ManyToOne
    @JoinColumn(name = "tool_id", nullable = false)
    private ToolEntity tool;

    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private Double fine;
    private String status;
}
```

**Relaciones:**
- **Loan → Client**: Many-to-One (muchos préstamos, un cliente)
- **Loan → Tool**: Many-to-One (muchos préstamos, una herramienta)
- **Kardex → Client**: Many-to-One
- **Kardex → Tool**: Many-to-One

JPA/Hibernate maneja las **foreign keys** automáticamente.

---

## 🧪 TESTING Y CALIDAD

### 10. ¿Qué tipo de tests tiene el proyecto?

**Respuesta:**

**1. Tests Unitarios** con JUnit 5 y Mockito

**Ejemplo:** `ToolServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock
    private ToolRepository toolRepository;

    @InjectMocks
    private ToolService toolService;

    @Test
    void getAllTools_shouldReturnAllTools() {
        List<ToolEntity> tools = Arrays.asList(new ToolEntity(), new ToolEntity());
        when(toolRepository.findAll()).thenReturn(tools);

        List<ToolEntity> result = toolService.getAllTools();

        assertEquals(2, result.size());
        verify(toolRepository).findAll();
    }
}
```

**2. Tests de Integración**

**Ejemplo:** `DemoApplicationTests.java`
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring carga correctamente
    }
}
```

**Cobertura de código:**
- **Herramienta:** JaCoCo
- **Cobertura alcanzada:** 92.25% en métodos (cumple ≥90%)
- **Reporte:** `backend-toolrent/target/site/jacoco/index.html`

---

### 11. ¿Cómo se ejecutan los tests?

**Respuesta:**

**Localmente:**
```bash
cd backend-toolrent
./mvnw test
./mvnw jacoco:report
```

**En Jenkins (Automatizado):**
```groovy
stage('Test Backend') {
    steps {
        dir('backend-toolrent') {
            sh './mvnw clean test'
            sh './mvnw jacoco:report'
        }
    }
    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            jacoco execPattern: '**/target/jacoco.exec'
        }
    }
}
```

**Configuración de tests para evitar conflictos de puertos:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

    @Test
    void mainMethodExecutes() {
        assertThatCode(() -> {
            DemoApplication.main(new String[] {"--server.port=0"});
        }).doesNotThrowAnyException();
    }
}
```

Esto permite que múltiples tests corran en paralelo sin conflictos de puerto.

---

## 🐳 DOCKER Y CONTENEDORES

### 12. ¿Cómo está configurado Docker en el proyecto?

**Respuesta:**

El proyecto usa **Docker Compose** para orquestar múltiples contenedores:

**Archivo:** `docker-compose.yml`

```yaml
services:
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: toolrent
      MYSQL_USER: toolrent
      MYSQL_PASSWORD: toolrent123
    volumes:
      - mysql_data:/var/lib/mysql

  keycloak:
    image: quay.io/keycloak/keycloak:26.0.2
    ports:
      - "9090:8080"
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_HOSTNAME: localhost
      KC_HTTP_ENABLED: "true"

  backend-1:
    build:
      context: ./backend-toolrent
    image: fergusone/toolrent-backend:latest
    ports:
      - "8090:8090"
    depends_on:
      - mysql
      - keycloak
    environment:
      DB_HOST: mysql

  frontend:
    build:
      context: ./toolrent-frontend
    image: fergusone/toolrent-frontend:latest
    ports:
      - "80:80"
    depends_on:
      - backend-1
```

**Contenedores:**
1. **mysql** - Base de datos (puerto 3306)
2. **keycloak** - Autenticación OAuth2 (puerto 9090)
3. **backend-1** - API Spring Boot (puerto 8090)
4. **frontend** - React + Nginx (puerto 80)

**Comandos:**
```bash
docker-compose up -d        # Iniciar todos los servicios
docker-compose ps           # Ver estado
docker-compose logs -f      # Ver logs
docker-compose down         # Detener todo
```

---

### 13. ¿Cómo se construyen las imágenes Docker?

**Respuesta:**

**Backend Dockerfile:**
```dockerfile
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Frontend Dockerfile:**
```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**Construcción:**
```bash
# Manual
docker build -t fergusone/toolrent-backend:latest ./backend-toolrent
docker build -t fergusone/toolrent-frontend:latest ./toolrent-frontend

# Con Docker Compose
docker-compose build
```

---

## 🔄 CI/CD Y JENKINS

### 14. ¿Cómo funciona el pipeline de Jenkins?

**Respuesta:**

El pipeline tiene **5 etapas** definidas en el `Jenkinsfile`:

```groovy
pipeline {
    agent any

    stages {
        stage('Checkout') {
            // Descarga código del repositorio
        }

        stage('Test Backend') {
            // Ejecuta tests con Maven
            // Genera reporte JaCoCo
            // Publica resultados en Jenkins
        }

        stage('Build Docker Images') {
            parallel {
                stage('Build Backend') {
                    // docker build backend
                }
                stage('Build Frontend') {
                    // docker build frontend
                }
            }
        }

        stage('Push to DockerHub') {
            parallel {
                stage('Push Backend') {
                    // docker push backend
                }
                stage('Push Frontend') {
                    // docker push frontend
                }
            }
        }

        stage('Cleanup') {
            // Elimina imágenes locales
        }
    }
}
```

**Flujo completo:**
1. **Trigger:** Push a GitHub
2. **Checkout:** Jenkins descarga el código
3. **Test:** Ejecuta 340 tests (92.25% cobertura)
4. **Build:** Construye imágenes Docker en paralelo
5. **Push:** Sube imágenes a Docker Hub
6. **Cleanup:** Limpia imágenes locales
7. **Resultado:** Imágenes listas para deployment

**Tiempo de ejecución:** ~5-7 minutos

---

### 15. ¿Qué plugins usa Jenkins?

**Respuesta:**

**Plugins instalados:**
1. **Docker Pipeline** - Para ejecutar comandos Docker
2. **JaCoCo Plugin** - Para visualizar cobertura de código
3. **JUnit Plugin** - Para mostrar resultados de tests
4. **Git Plugin** - Para integración con GitHub
5. **Pipeline Plugin** - Para ejecutar Jenkinsfiles

**Configuración:**
- **Puerto:** 8081 (para no conflictuar con backend en 8080)
- **Docker CLI:** Instalado dentro del contenedor Jenkins
- **Credenciales:** Docker Hub configuradas como `dockerhub-credentials`

---

## 💼 LÓGICA DE NEGOCIO

### 16. ¿Cómo se calculan las multas por atraso?

**Respuesta:**

**Archivo:** `LoanService.java`

```java
@Scheduled(cron = "0 0 0 * * ?")  // Ejecuta a medianoche
public void updateOverdueLoansDaily() {
    LocalDate today = LocalDate.now();
    List<LoanEntity> activeLoans = loanRepository.findByReturnDateIsNull();

    for (LoanEntity loan : activeLoans) {
        if (loan.getDueDate().isBefore(today)) {
            long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), today);
            Double dailyFine = configService.getDailyFineAmount();
            Double totalFine = daysOverdue * dailyFine;

            loan.setFine(totalFine);
            loan.setStatus("Atrasado");
            loanRepository.save(loan);
        }
    }
}
```

**Reglas:**
1. **Tarea programada:** Se ejecuta todos los días a medianoche
2. **Cálculo:** `multa = días_atraso × multa_diaria`
3. **Multa diaria:** Configurable (default: $1000 por día)
4. **Estado:** Cambia a "Atrasado" automáticamente
5. **Cliente:** Estado cambia a "Con deuda" si tiene multas pendientes

---

### 17. ¿Qué validaciones se aplican al crear un préstamo?

**Respuesta:**

**Archivo:** `LoanService.java` - método `createLoan()`

**Validaciones:**

1. **Cliente debe estar Activo:**
```java
if (!"Activo".equalsIgnoreCase(client.getState())) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Solo se pueden realizar préstamos a clientes en estado 'Activo'.");
}
```

2. **Cliente no debe tener préstamos atrasados:**
```java
if (loanRepository.hasOverduesOrFines(clientId)) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "El cliente tiene préstamos atrasados o multas pendientes.");
}
```

3. **Cliente no puede tener más de 3 préstamos activos:**
```java
long activeLoansCount = allLoans.stream()
    .filter(l -> Objects.equals(l.getClient().getId(), clientId))
    .filter(l -> l.getReturnDate() == null)
    .count();

if (activeLoansCount >= 3) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "El cliente ya tiene 3 préstamos activos.");
}
```

4. **Herramienta debe estar disponible:**
```java
if (!"Disponible".equalsIgnoreCase(tool.getStatus())) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "La herramienta no está disponible para préstamo.");
}
```

5. **Fecha de devolución debe ser futura:**
```java
if (dueDate.isBefore(today) || dueDate.isEqual(today)) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "La fecha de devolución debe ser posterior a hoy.");
}
```

---

### 18. ¿Cómo se actualiza el estado de los clientes automáticamente?

**Respuesta:**

**Archivo:** `ClientService.java`

```java
@Transactional
public void updateClientStateBasedOnLoans(Long clientId) {
    ClientEntity client = clientRepository.findById(clientId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Cliente no encontrado"));

    boolean hasOverdues = loanRepository.hasOverduesOrFines(clientId);
    boolean hasActiveLoans = loanRepository.existsByClientIdAndReturnDateIsNull(clientId);

    if (hasOverdues) {
        client.setState("Con deuda");
    } else if (hasActiveLoans) {
        client.setState("Activo con préstamo");
    } else {
        client.setState("Activo");
    }

    clientRepository.save(client);
}
```

**Estados posibles:**
1. **Activo** - Sin préstamos activos ni deudas
2. **Activo con préstamo** - Tiene préstamos activos al día
3. **Con deuda** - Tiene préstamos atrasados o multas pendientes
4. **Inactivo** - Dado de baja manualmente

**Actualización automática:**
- Se ejecuta **antes de crear un préstamo**
- Se ejecuta **al devolver un préstamo**
- Se ejecuta **al pagar una multa**
- Se ejecuta **diariamente a medianoche** (scheduled task)

---

## 📊 REPORTES Y KARDEX

### 19. ¿Qué es el Kardex y cómo funciona?

**Respuesta:**

El **Kardex** es un **historial de todos los movimientos** de herramientas en el sistema.

**Archivo:** `KardexEntity.java`
```java
@Entity
@Table(name = "kardex")
public class KardexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private ToolEntity tool;

    @ManyToOne
    private ClientEntity client;

    private LocalDateTime timestamp;
    private String movementType;  // "PRESTAMO", "DEVOLUCION", "ALTA", "BAJA"
    private String performedBy;
    private String details;
}
```

**Movimientos registrados:**
1. **PRESTAMO** - Cuando se crea un préstamo
2. **DEVOLUCION** - Cuando se devuelve una herramienta
3. **ALTA** - Cuando se da de alta una herramienta
4. **BAJA** - Cuando se da de baja una herramienta

**Registro automático:**
```java
@Transactional
public LoanEntity createLoan(...) {
    // ... crear préstamo ...

    // Registrar en Kardex
    kardexService.registerMovement(
        tool,
        client,
        "PRESTAMO",
        username,
        "Préstamo creado"
    );

    return loan;
}
```

**Utilidad:**
- Auditoría completa del sistema
- Trazabilidad de cada movimiento
- Reportes históricos
- Análisis de uso de herramientas

---

### 20. ¿Qué reportes genera el sistema?

**Respuesta:**

**Archivo:** `ReportController.java`

**Endpoint:** `GET /api/v1/reports/`

**Reportes disponibles:**

1. **Herramientas más prestadas:**
```json
{
  "toolName": "Taladro Bosch",
  "totalLoans": 45,
  "currentlyOnLoan": 3
}
```

2. **Clientes con más préstamos:**
```json
{
  "clientName": "María González",
  "totalLoans": 12,
  "activeLoans": 2,
  "totalFines": 5000.0
}
```

3. **Préstamos por estado:**
```json
{
  "Activo": 15,
  "Devuelto": 120,
  "Atrasado": 3
}
```

4. **Multas pendientes:**
```json
{
  "totalFines": 25000.0,
  "clientsWithFines": 5,
  "averageFinePerClient": 5000.0
}
```

**Implementación:**
```java
@GetMapping("/")
public ReportDTO generateReport() {
    List<LoanEntity> allLoans = loanRepository.findAll();

    // Calcular estadísticas
    Map<String, Long> loansByStatus = allLoans.stream()
        .collect(Collectors.groupingBy(
            LoanEntity::getStatus,
            Collectors.counting()
        ));

    return new ReportDTO(loansByStatus, ...);
}
```

---

## 🚀 DEPLOYMENT Y OPERACIONES

### 21. ¿Cómo se despliega el sistema completo?

**Respuesta:**

**Script automatizado:** `deploy-complete.ps1`

```powershell
# 1. Detener servicios existentes
docker-compose down

# 2. Construir imágenes
docker-compose build

# 3. Iniciar servicios
docker-compose up -d

# 4. Esperar a que MySQL esté listo
Wait-For-MySQL

# 5. Importar datos iniciales
docker cp seed-data.sql toolrent-mysql:/tmp/
docker exec toolrent-mysql mysql -u root -proot123 toolrent < /tmp/seed-data.sql

# 6. Verificar salud de servicios
docker-compose ps
```

**Orden de inicio:**
1. MySQL (primero)
2. Keycloak (segundo)
3. Backend (depende de MySQL y Keycloak)
4. Frontend (depende de Backend)

**Verificación:**
```bash
curl http://localhost:8090/actuator/health  # Backend
curl http://localhost:80                     # Frontend
```

---

### 22. ¿Cómo se monitorea el sistema en producción?

**Respuesta:**

**1. Health Checks de Spring Boot:**

**Configuración:** `application.properties`
```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=never
```

**Endpoint:** `GET http://localhost:8090/actuator/health`

**Respuesta:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

**2. Logs de Docker:**
```bash
docker-compose logs -f backend-1    # Backend logs
docker-compose logs -f frontend     # Frontend logs
docker-compose logs -f mysql        # Database logs
```

**3. Métricas de JaCoCo:**
- Cobertura de código en cada build
- Histórico de cobertura en Jenkins
- Alertas si la cobertura baja de 90%

**4. Monitoreo de contenedores:**
```bash
docker stats                        # Uso de CPU/memoria
docker-compose ps                   # Estado de servicios
```

---

## 🔧 CONFIGURACIÓN Y VARIABLES DE ENTORNO

### 23. ¿Qué variables de entorno se usan?

**Respuesta:**

**Backend (application.properties):**
```properties
# Base de datos
spring.datasource.url=jdbc:mysql://${DB_HOST}:3306/toolrent
DB_HOST=localhost  # En Docker: mysql

# Puerto
server.port=8090

# Keycloak
spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:9090/realms/sisgr-realm
```

**Frontend (.env):**
```bash
VITE_PAYROLL_BACKEND_SERVER=localhost
VITE_PAYROLL_BACKEND_PORT=8090
VITE_KEYCLOAK_URL=http://localhost:9090
VITE_KEYCLOAK_REALM=sisgr-realm
VITE_KEYCLOAK_CLIENT_ID=sisgr-frontend
```

**Docker Compose:**
```yaml
backend-1:
  environment:
    DB_HOST: mysql              # Nombre del servicio MySQL
    SPRING_PROFILES_ACTIVE: prod
```

**Cambio de ambiente:**
```bash
# Desarrollo (local)
DB_HOST=localhost

# Producción (Docker)
DB_HOST=mysql
```

---

## 🐛 MANEJO DE ERRORES

### 24. ¿Cómo se manejan los errores en el sistema?

**Respuesta:**

**Backend:**

1. **Excepciones de negocio:**
```java
if (!"Activo".equalsIgnoreCase(client.getState())) {
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "El cliente debe estar en estado 'Activo'"
    );
}
```

2. **Configuración global:**
```properties
server.error.include-message=always
```

3. **Respuesta de error:**
```json
{
  "timestamp": "2025-12-03T15:35:12.661Z",
  "status": 400,
  "error": "Bad Request",
  "message": "El cliente debe estar en estado 'Activo'",
  "path": "/api/v1/loans/"
}
```

**Frontend:**

1. **Interceptor de respuestas:**
```javascript
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || 'Error desconocido';
    showErrorCallback(message);  // Muestra Snackbar
    return Promise.reject(error);
  }
);
```

2. **Contexto de Snackbar:**
```jsx
<SnackbarContext.Provider value={{ showError, showSuccess }}>
  {children}
</SnackbarContext.Provider>
```

3. **Manejo en componentes:**
```jsx
try {
  await toolService.create(newTool);
  showSuccess('Herramienta creada exitosamente');
} catch (error) {
  // El interceptor ya mostró el error
  console.error(error);
}
```

---

## 📚 TECNOLOGÍAS Y FRAMEWORKS

### 25. ¿Qué tecnologías se usan en el proyecto?

**Respuesta:**

**Frontend:**
- **React 18** - Framework UI
- **Vite** - Build tool y dev server
- **Material-UI (MUI)** - Componentes UI
- **Axios** - Cliente HTTP
- **React Router** - Navegación
- **Keycloak.js** - Cliente OAuth2

**Backend:**
- **Spring Boot 3.4** - Framework Java
- **Spring Security** - Autenticación y autorización
- **Spring Data JPA** - ORM
- **Hibernate** - Implementación JPA
- **MySQL Driver** - Conector JDBC
- **Maven** - Gestor de dependencias

**Base de Datos:**
- **MySQL 8.0** - Motor de base de datos
- **UTF-8 MB4** - Charset para soportar emojis y acentos

**Autenticación:**
- **Keycloak 26.0** - Identity Provider
- **OAuth 2.0** - Protocolo de autorización
- **JWT** - JSON Web Tokens

**Infraestructura:**
- **Docker** - Contenedores
- **Docker Compose** - Orquestación
- **Nginx** - Servidor web / Proxy inverso
- **Jenkins** - CI/CD

**Testing:**
- **JUnit 5** - Framework de tests
- **Mockito** - Mocking
- **JaCoCo** - Cobertura de código
- **Maven Surefire** - Ejecución de tests

---

## 🎯 CUMPLIMIENTO DE REQUISITOS

### 26. ¿Cómo cumple el proyecto con los requisitos funcionales?

**Respuesta:**

**RF1: Administrar Herramientas**
- ✅ RF1.1: Dar de alta herramientas → `POST /api/v1/tools/`
- ✅ RF1.2: Dar de baja herramientas → `PUT /api/v1/tools/{id}/decommission`
- ✅ RF1.3: Listar herramientas → `GET /api/v1/tools/`

**RF2: Administrar Préstamos**
- ✅ RF2.1: Crear préstamos con validaciones → `POST /api/v1/loans/`
- ✅ RF2.2: Registrar devoluciones → `PUT /api/v1/loans/{id}/return`
- ✅ RF2.3: Calcular multas automáticamente → Scheduled task diario

**RF3: Administrar Clientes**
- ✅ RF3.1: Registrar clientes → `POST /api/v1/clients/`
- ✅ RF3.2: Actualizar estado automáticamente → `updateClientStateBasedOnLoans()`
- ✅ RF3.3: Validar límite de préstamos (máx 3)

**RF4: Seguridad**
- ✅ RF4.1: Autenticación OAuth2 con Keycloak
- ✅ RF4.2: Roles ADMIN y USER
- ✅ RF4.3: Control de acceso con `@PreAuthorize`

**RF5: Trazabilidad**
- ✅ RF5.1: Kardex con todos los movimientos
- ✅ RF5.2: Timestamp y usuario en cada operación

**RF6: Reportes**
- ✅ RF6.1: Herramientas más prestadas
- ✅ RF6.2: Clientes con más préstamos
- ✅ RF6.3: Multas pendientes por cobrar

**Cobertura de tests:** 92.25% (cumple requisito ≥90%)

---

## 📊 PREGUNTAS EXTRAS

### 27. ¿Por qué se usa puerto 8090 para el backend y no 8080?

**Respuesta:**

Porque el puerto **8080** es el puerto por defecto de:
- Spring Boot
- Jenkins
- Tomcat
- Muchos otros servicios

Para evitar conflictos, especialmente con Jenkins que también corre en contenedor Docker, el backend usa el puerto **8090**.

**Configuración:**
```properties
server.port=8090
```

Jenkins usa el puerto **8081** por la misma razón.

---

### 28. ¿Qué problema se solucionó con los caracteres acentuados?

**Respuesta:**

**Problema original:**
Los nombres con acentos como "María González" se mostraban como "Mar?a Gonz?lez" en el frontend.

**Causa:**
Falta de configuración UTF-8 en la cadena JDBC y en las propiedades de Hibernate.

**Solución implementada:**

1. **JDBC URL actualizada:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/toolrent?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci
```

2. **Propiedades Hibernate:**
```properties
spring.jpa.properties.hibernate.connection.CharSet=utf8mb4
spring.jpa.properties.hibernate.connection.characterEncoding=utf8mb4
spring.jpa.properties.hibernate.connection.useUnicode=true
```

3. **Nginx configurado:**
```nginx
charset utf-8;
add_header Content-Type "text/html; charset=utf-8";
```

4. **Reimportación de datos:**
```bash
docker cp seed-data.sql toolrent-mysql:/tmp/
docker exec toolrent-mysql mysql --default-character-set=utf8mb4 toolrent < /tmp/seed-data.sql
```

**Resultado:** ✅ Acentos funcionando correctamente en toda la aplicación

---

### 29. ¿Por qué los tests fallan si usan el puerto 8080?

**Respuesta:**

**Problema:**
Cuando Jenkins ejecuta los tests, algunos tests intentan levantar el servidor Spring Boot en el puerto 8080, que ya está ocupado por Jenkins o por otro test en ejecución paralela.

**Error:**
```
org.springframework.boot.web.server.PortInUseException: Port 8080 is already in use
```

**Solución:**

Configurar los tests para usar **puertos aleatorios**:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

    @Test
    void mainMethodExecutes() {
        assertThatCode(() -> {
            DemoApplication.main(new String[] {"--server.port=0"});
        }).doesNotThrowAnyException();
    }
}
```

**Beneficios:**
- Tests pueden correr en paralelo
- No hay conflictos de puerto
- Tests son más robustos
- Funciona tanto en local como en Jenkins

---

### 30. ¿Cuál es el flujo completo desde que el usuario hace click hasta que ve los datos?

**Respuesta:**

**Ejemplo: Usuario lista herramientas**

```
1. Usuario hace click en "Herramientas" en el menú
   ↓
2. React Router navega a /tools
   ↓
3. Componente ToolsList.jsx se monta
   ↓
4. useEffect llama a toolService.getAll()
   ↓
5. toolService.getAll() usa http.get('/api/v1/tools/')
   ↓
6. Axios intercepta la petición
   ↓
7. Interceptor agrega token JWT: Authorization: Bearer <token>
   ↓
8. Petición HTTP va a: http://localhost:8090/api/v1/tools/
   ↓
9. Nginx (si está configurado) reenvía la petición al backend
   ↓
10. Spring Boot recibe la petición en ToolController
    ↓
11. @PreAuthorize valida el rol del usuario con Keycloak
    ↓
12. Controller llama a toolService.getAllTools()
    ↓
13. ToolService llama a toolRepository.findAll()
    ↓
14. Repository ejecuta: SELECT * FROM tools
    ↓
15. MySQL retorna los registros
    ↓
16. Hibernate convierte registros a List<ToolEntity>
    ↓
17. Service retorna la lista al Controller
    ↓
18. Controller retorna ResponseEntity con JSON
    ↓
19. Axios recibe la respuesta HTTP 200
    ↓
20. Axios convierte JSON a objetos JavaScript
    ↓
21. Promise se resuelve con los datos
    ↓
22. React actualiza el estado: setTools(data)
    ↓
23. Componente se re-renderiza con los datos
    ↓
24. Material-UI DataGrid muestra la tabla
    ↓
25. Usuario ve las herramientas en pantalla
```

**Tiempo total:** ~100-300ms

---

## ✅ CHECKLIST DE PREPARACIÓN

Antes de la presentación, asegúrate de poder responder:

- [ ] ¿Cómo se comunica frontend con backend?
- [ ] ¿Qué endpoints expone la API?
- [ ] ¿Cómo funciona la autenticación con Keycloak?
- [ ] ¿Cómo se calculan las multas?
- [ ] ¿Qué validaciones tiene el sistema?
- [ ] ¿Cómo se ejecutan los tests?
- [ ] ¿Qué es el pipeline de Jenkins?
- [ ] ¿Cómo se despliega con Docker?
- [ ] ¿Qué tecnologías se usan?
- [ ] ¿Cómo se cumple con los requisitos funcionales?

---

**📌 Consejo final:** Practica demostrando cada funcionalidad en vivo. Es mejor mostrar que explicar.

**¡Buena suerte en la evaluación! 🚀**
