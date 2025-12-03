# 🛠️ ToolRent - Sistema de Gestión de Préstamos de Herramientas

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.8-green)
![React](https://img.shields.io/badge/React-18.2-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

Sistema integral de gestión de préstamos de herramientas para ToolRent, desarrollado como parte de la evaluación del curso **Métodos/Técnicas de Ingeniería de Software (2025-2)**.

---

## 📖 Descripción del Proyecto

ToolRent es una aplicación web monolítica basada en arquitectura por capas que permite:

- ✅ Gestión completa de inventario de herramientas
- ✅ Control de préstamos y devoluciones con reglas de negocio complejas
- ✅ Administración de clientes con estados automáticos
- ✅ Cálculo automático de multas y tarifas
- ✅ Sistema Kardex para trazabilidad de movimientos
- ✅ Reportes y consultas estratégicas
- ✅ Autenticación y autorización con Keycloak (OAuth2/JWT)

---

## 🏗️ Arquitectura

### Backend (Monolito por Capas)
```
backend-toolrent/
├── Controller/     # Capa de presentación (REST API)
├── Service/        # Capa de lógica de negocio
├── Repository/     # Capa de acceso a datos (JPA)
├── Entity/         # Capa de modelo de datos
└── config/         # Configuración (Security, etc.)
```

### Frontend (SPA)
```
toolrent-frontend/
├── components/     # Componentes React
├── services/       # Servicios HTTP (Axios)
├── contexts/       # Contextos React
└── utils/          # Utilidades
```

### Despliegue
```
Docker Compose
├── MySQL (Base de datos)
├── Keycloak (IAM)
├── Backend (3 réplicas)
├── Nginx (Load Balancer)
└── Frontend (Nginx)
```

---

## 🚀 Tecnologías Utilizadas

### Backend
- **Java 17** - Lenguaje de programación
- **Spring Boot 3.4.8** - Framework principal
- **Spring Data JPA** - ORM
- **Spring Security + OAuth2** - Seguridad
- **MySQL 8.0** - Base de datos relacional
- **Lombok** - Reducción de boilerplate
- **JUnit 5 + Mockito** - Testing
- **Jacoco** - Cobertura de código

### Frontend
- **React 18.2** - Framework UI
- **Vite** - Build tool
- **Material-UI (MUI)** - Componentes UI
- **Axios** - Cliente HTTP
- **React Router** - Navegación
- **Keycloak JS** - Autenticación

### DevOps
- **Docker** - Containerización
- **Docker Compose** - Orquestación
- **Jenkins** - CI/CD
- **Nginx** - Load Balancer / Web Server
- **Keycloak** - Identity & Access Management

---

## 📋 Requisitos Previos

- Docker Desktop (v20.10+)
- Docker Compose (v2.0+)
- Java 17 (para desarrollo local)
- Node.js 18+ (para desarrollo local)
- Jenkins (opcional, para CI/CD)
- Cuenta en Docker Hub

---

## 🚀 Inicio Rápido

### Opción 1: Despliegue con Docker Compose (Recomendado)

```bash
# 1. Clonar repositorio
git clone https://github.com/AresOnee/tingesolab1.git
cd tingesolab1

# 2. Configurar variables de entorno
cp .env.example .env
# Editar .env con tu usuario de Docker Hub

# 3. Desplegar
export DOCKER_USERNAME=tu-usuario
docker-compose up -d

# 4. Ver logs
docker-compose logs -f

# 5. Verificar estado
docker-compose ps
```

**Acceder a la aplicación**:
- Frontend: http://localhost
- Backend API: http://localhost:8090
- Keycloak Admin: http://localhost:9090 (admin/admin)

### Opción 2: Desarrollo Local

#### Backend
```bash
cd backend-toolrent
./mvnw spring-boot:run
```

#### Frontend
```bash
cd toolrent-frontend
npm install
npm run dev
```

---

## 🧪 Pruebas y Cobertura

### Ejecutar tests
```bash
cd backend-toolrent
./mvnw test
```

### Generar reporte de cobertura
```bash
./mvnw jacoco:report
open target/site/jacoco/index.html
```

**Cobertura actual**: ≥90% a nivel de líneas de código

---

## 📚 Documentación Adicional

- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Guía completa de despliegue
- **[Evaluacion1_v1_3.pdf](Evaluacion1_v1_3.pdf)** - Enunciado del proyecto
- **[Rubrica de Evaluación 1 _v2.pdf](Rubrica%20de%20Evaluación%201%20_v2.pdf)** - Criterios de evaluación

---

## 🔐 Seguridad

El sistema implementa:
- ✅ Autenticación OAuth2 con Keycloak
- ✅ Tokens JWT para sesiones
- ✅ Control de acceso basado en roles (RBAC)
- ✅ Validación de permisos en cada endpoint
- ✅ CORS configurado correctamente

**Roles disponibles**:
- `ADMIN`: Acceso completo al sistema
- `USER` (Empleado): Acceso limitado a operaciones básicas

---

## 📊 Funcionalidades Principales

### Épica 1: Gestión de Herramientas
- Registrar nuevas herramientas
- Dar de baja herramientas (solo admin)
- Control de stock y estados

### Épica 2: Préstamos y Devoluciones
- Crear préstamos con validaciones completas
- Registrar devoluciones
- Cálculo automático de multas
- Manejo de daños (reparables e irreparables)

### Épica 3: Gestión de Clientes
- CRUD de clientes con validaciones
- Actualización automática de estados
- Restricción de clientes morosos

### Épica 4: Tarifas y Configuración
- Configuración de tarifas diarias
- Gestión de valores de reposición
- Actualización dinámica (solo admin)

### Épica 5: Kardex
- Registro automático de movimientos
- Consulta de historial por herramienta
- Filtros por rango de fechas

### Épica 6: Reportes
- Préstamos activos (vigentes y atrasados)
- Clientes con atrasos
- Ranking de herramientas más prestadas

### Épica 7: Usuarios y Roles
- Autenticación con Keycloak
- Autorización basada en roles
- Control de acceso granular

---

## 🤝 Contribuciones

Este es un proyecto académico. Para sugerencias o mejoras:

1. Fork el repositorio
2. Crear branch: `git checkout -b feature/nueva-funcionalidad`
3. Commit: `git commit -am 'Agregar nueva funcionalidad'`
4. Push: `git push origin feature/nueva-funcionalidad`
5. Crear Pull Request

---

## 📝 Licencia

Este proyecto es desarrollado con fines académicos para el curso de Métodos/Técnicas de Ingeniería de Software, USACH 2025-2.

---

## 👥 Autor

Desarrollado por **AresOne** para la Evaluación 1 - Métodos/Técnicas de Ingeniería de Software.

---

## 🙏 Agradecimientos

- USACH - Departamento de Ingeniería de Software
- Profesor del curso Métodos/Técnicas de Ingeniería de Software
- Comunidad de Spring Boot y React

---
