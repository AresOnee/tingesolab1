# 🚀 GUÍA DE DESPLIEGUE - TOOLRENT

Esta guía explica cómo desplegar la aplicación ToolRent usando Docker y Jenkins.

---

## 📋 PREREQUISITOS

- Docker Desktop instalado (v20.10 o superior)
- Docker Compose instalado (v2.0 o superior)
- Jenkins instalado (opcional, para CI/CD)
- Cuenta en Docker Hub
- Git instalado

---

## 🔧 CONFIGURACIÓN INICIAL

### 1. Clonar el repositorio

```bash
git clone https://github.com/AresOnee/tingesolab1.git
cd tingesolab1
```

### 2. Configurar variables de entorno

```bash
cp .env.example .env
# Editar .env con tus credenciales de Docker Hub
nano .env
```

---

## 🐳 OPCIÓN 1: DESPLIEGUE MANUAL CON DOCKER

### Paso 1: Build de las imágenes Docker

#### Backend
```bash
cd backend-toolrent
docker build -t tu-usuario/toolrent-backend:latest .
cd ..
```

#### Frontend
```bash
cd toolrent-frontend
docker build -t tu-usuario/toolrent-frontend:latest .
cd ..
```

### Paso 2: Push a Docker Hub

```bash
# Login en Docker Hub
docker login

# Push backend
docker push tu-usuario/toolrent-backend:latest

# Push frontend
docker push tu-usuario/toolrent-frontend:latest
```

### Paso 3: Desplegar con Docker Compose

```bash
# Configurar variable de entorno
export DOCKER_USERNAME=tu-usuario

# Levantar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f

# Ver estado de los servicios
docker-compose ps
```

### Paso 4: Verificar despliegue

La aplicación estará disponible en:
- **Frontend**: http://localhost:80
- **Backend**: http://localhost:8090
- **Keycloak**: http://localhost:9090
- **MySQL**: localhost:3306

### Comandos útiles

```bash
# Detener servicios
docker-compose down

# Detener y eliminar volúmenes
docker-compose down -v

# Reiniciar un servicio específico
docker-compose restart backend-1

# Ver logs de un servicio
docker-compose logs -f frontend

# Escalar backend (ajustar réplicas)
docker-compose up -d --scale backend=5
```

---

## 🤖 OPCIÓN 2: DESPLIEGUE AUTOMATIZADO CON JENKINS

### Configuración de Jenkins

#### 1. Instalar plugins necesarios

En Jenkins, instalar:
- Docker Pipeline
- GitHub Plugin
- Jacoco Plugin
- JUnit Plugin

#### 2. Configurar credenciales en Jenkins

**Docker Hub Credentials**:
1. Ir a: Jenkins → Manage Jenkins → Manage Credentials
2. Agregar credenciales tipo "Username with password"
   - ID: `dockerhub-credentials`
   - Username: tu usuario de Docker Hub
   - Password: tu password de Docker Hub

**Docker Hub Username**:
1. Agregar credencial tipo "Secret text"
   - ID: `dockerhub-username`
   - Secret: tu usuario de Docker Hub

#### 3. Crear Pipeline en Jenkins

1. New Item → Pipeline
2. Nombre: `ToolRent-CI-CD`
3. En "Pipeline":
   - Definition: Pipeline script from SCM
   - SCM: Git
   - Repository URL: https://github.com/AresOnee/tingesolab1.git
   - Branch: */main (o tu branch)
   - Script Path: `Jenkinsfile`
4. Guardar

#### 4. Ejecutar Pipeline

1. Click en "Build Now"
2. El pipeline ejecutará:
   - ✅ Checkout del código
   - ✅ Tests unitarios del backend
   - ✅ Build de imágenes Docker
   - ✅ Push a Docker Hub
   - ✅ Cleanup

#### 5. Desplegar desde imágenes en Docker Hub

```bash
# Las imágenes ya están en Docker Hub
export DOCKER_USERNAME=tu-usuario
docker-compose pull
docker-compose up -d
```

---

## 🏗️ ARQUITECTURA DE DESPLIEGUE

```
┌─────────────────────────────────────────────────────────────┐
│                        USUARIO                              │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                   FRONTEND (React)                          │
│                   Puerto 80                                 │
│                   Nginx Server                              │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              NGINX LOAD BALANCER                            │
│                   Puerto 8090                               │
└───────────┬────────────┬────────────┬───────────────────────┘
            │            │            │
            ▼            ▼            ▼
    ┌───────────┐ ┌───────────┐ ┌───────────┐
    │ Backend 1 │ │ Backend 2 │ │ Backend 3 │
    │  :8090    │ │  :8090    │ │  :8090    │
    └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
          │             │             │
          └─────────────┴─────────────┘
                        │
            ┌───────────┴───────────┐
            │                       │
            ▼                       ▼
    ┌──────────────┐        ┌──────────────┐
    │    MySQL     │        │   Keycloak   │
    │   :3306      │        │    :9090     │
    └──────────────┘        └──────────────┘
```

---

## 📊 VERIFICACIÓN DE COBERTURA DE TESTS

### Generar reporte de cobertura localmente

```bash
cd backend-toolrent
./mvnw clean test jacoco:report

# El reporte estará en:
# target/site/jacoco/index.html
```

### Ver reporte en navegador

```bash
# Linux/Mac
open target/site/jacoco/index.html

# Windows
start target/site/jacoco/index.html
```

El reporte mostrará:
- ✅ Cobertura por paquete
- ✅ Cobertura por clase
- ✅ Cobertura a nivel de línea
- ✅ Indicadores visuales de qué código está cubierto

---

## 🔍 HEALTH CHECKS

### Backend
```bash
curl http://localhost:8090/actuator/health
```

### Frontend
```bash
curl http://localhost/health
```

### Nginx Load Balancer
```bash
curl http://localhost:8090/health
```

### Keycloak
```bash
curl http://localhost:9090/health/ready
```

---

## 🐛 TROUBLESHOOTING

### Problema: Backend no se conecta a MySQL

**Solución**:
```bash
# Verificar que MySQL esté corriendo
docker-compose ps mysql

# Ver logs de MySQL
docker-compose logs mysql

# Esperar a que MySQL esté completamente iniciado
docker-compose logs -f mysql | grep "ready for connections"
```

### Problema: Frontend no se conecta al backend

**Solución**:
```bash
# Verificar que Nginx LB esté corriendo
docker-compose ps nginx-backend

# Ver logs del load balancer
docker-compose logs nginx-backend

# Verificar que las 3 réplicas del backend estén up
docker-compose ps | grep backend
```

### Problema: Keycloak no inicia

**Solución**:
```bash
# Keycloak tarda ~60s en iniciar
# Ver logs
docker-compose logs -f keycloak

# Esperar mensaje: "Keycloak 26.0.0 started"
```

### Problema: Imágenes Docker no se encuentran

**Solución**:
```bash
# Pull manual de las imágenes
docker pull tu-usuario/toolrent-backend:latest
docker pull tu-usuario/toolrent-frontend:latest

# O build local
docker-compose build
```

---

## 📝 NOTAS IMPORTANTES

1. **Primera ejecución**: La primera vez que se ejecuta `docker-compose up`, puede tardar varios minutos mientras:
   - MySQL inicializa la base de datos
   - Keycloak crea el realm
   - Los backends se conectan

2. **Health Checks**: Todos los servicios tienen health checks configurados. Docker Compose esperará a que estén "healthy" antes de iniciar servicios dependientes.

3. **Persistencia**: Los datos de MySQL se guardan en un volumen Docker (`toolrent-mysql-data`) y persisten entre reinicios.

4. **Escalabilidad**: Puedes ajustar el número de réplicas del backend editando `docker-compose.yml` o usando `--scale`.

5. **Producción**: Para despliegue en producción:
   - Cambiar contraseñas por defecto
   - Configurar HTTPS
   - Usar secretos de Docker/Kubernetes
   - Configurar límites de recursos (CPU/RAM)

---

## 📞 SOPORTE

Para problemas o preguntas:
- Revisar logs: `docker-compose logs -f`
- Ver estado: `docker-compose ps`
- Reiniciar servicios: `docker-compose restart`

---

**¡Listo! Tu aplicación ToolRent debería estar funcionando correctamente.** 🎉
