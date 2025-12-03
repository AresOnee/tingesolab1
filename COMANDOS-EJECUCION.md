# 🚀 Comandos de Ejecución - ToolRent

## Guía Completa de Comandos para Todas las Formas de Ejecución

---

# 📋 Índice Rápido

1. [Ejecución Local (Sin Docker)](#1-ejecución-local-sin-docker)
2. [Ejecución con Docker](#2-ejecución-con-docker)
3. [Ejecución con Jenkins (CI/CD)](#3-ejecución-con-jenkins-cicd)
4. [Comandos de Verificación](#4-comandos-de-verificación)
5. [Comandos de Troubleshooting](#5-comandos-de-troubleshooting)

---

# 1. Ejecución Local (Sin Docker)

## 📌 Escenario: "Ejecuta la aplicación en local"

### Pre-requisitos
- ✅ Java 21 instalado
- ✅ Node.js 18+ instalado
- ✅ MySQL 8.0 corriendo
- ✅ Keycloak corriendo

---

## 1.1. Iniciar Base de Datos (MySQL)

### Opción A: MySQL ya instalado en Windows

```powershell
# Iniciar servicio MySQL
net start MySQL80

# O desde Services (Win + R → services.msc)
# Buscar "MySQL80" → Click derecho → Start
```

### Opción B: MySQL con Docker (solo BD)

```powershell
docker run -d \
  --name mysql-local \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=toolrent \
  -e MYSQL_USER=toolrent \
  -e MYSQL_PASSWORD=toolrent123 \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

### Importar datos iniciales

```powershell
# Si usas MySQL local
mysql -u root -p toolrent < seed-data.sql

# Si usas MySQL en Docker
docker exec -i mysql-local mysql -uroot -proot123 toolrent < seed-data.sql
```

---

## 1.2. Iniciar Keycloak (Autenticación)

### Con Docker

```powershell
docker run -d \
  --name keycloak-local \
  -p 9090:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e KC_HTTP_ENABLED=true \
  -v ${PWD}/keycloak-config:/opt/keycloak/data/import \
  quay.io/keycloak/keycloak:26.3.3 \
  start-dev --import-realm
```

**Verificar:**
```powershell
start http://localhost:9090
# Login: admin / admin
```

---

## 1.3. Iniciar Backend (Spring Boot)

```powershell
# Ir al directorio del backend
cd backend-toolrent

# Opción A: Con Maven Wrapper (Recomendado)
.\mvnw spring-boot:run

# Opción B: Compilar y ejecutar JAR
.\mvnw clean package -DskipTests
java -jar target/demo-0.0.1-SNAPSHOT.jar

# Opción C: Con Maven instalado
mvn spring-boot:run
```

**Variables de entorno (opcional):**
```powershell
# Si MySQL no está en localhost
$env:DB_HOST="127.0.0.1"
.\mvnw spring-boot:run
```

**Verificar:**
```powershell
# Debe mostrar: {"status":"UP"}
curl http://localhost:8090/actuator/health
```

---

## 1.4. Iniciar Frontend (React + Vite)

**Terminal NUEVA (dejar backend corriendo):**

```powershell
# Ir al directorio del frontend
cd toolrent-frontend

# Instalar dependencias (solo primera vez)
npm install

# Opción A: Modo desarrollo (recomendado para local)
npm run dev

# Opción B: Build y preview (simula producción)
npm run build
npm run preview
```

**Verificar:**
```powershell
# Modo dev
start http://localhost:5173

# Modo preview
start http://localhost:4173
```

---

## 1.5. Detener Aplicación Local

```powershell
# Detener backend
# → Presiona Ctrl + C en la terminal del backend

# Detener frontend
# → Presiona Ctrl + C en la terminal del frontend

# Detener MySQL (si es Docker)
docker stop mysql-local
docker rm mysql-local

# Detener Keycloak (si es Docker)
docker stop keycloak-local
docker rm keycloak-local

# Detener MySQL (si es servicio Windows)
net stop MySQL80
```

---

## 📊 Resumen Ejecución Local

```powershell
# Terminal 1: MySQL (si es Docker)
docker run -d --name mysql-local -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=toolrent mysql:8.0

# Terminal 2: Keycloak
docker run -d --name keycloak-local -p 9090:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:26.3.3 start-dev

# Terminal 3: Backend
cd backend-toolrent
.\mvnw spring-boot:run

# Terminal 4: Frontend
cd toolrent-frontend
npm run dev
```

**Acceder:**
- Frontend: http://localhost:5173
- Backend: http://localhost:8090
- Keycloak: http://localhost:9090

---

# 2. Ejecución con Docker

## 📌 Escenario: "Ejecuta la aplicación con Docker"

---

## 2.1. Ejecución Completa con Docker Compose

### Opción A: Script Automatizado (RECOMENDADO)

```powershell
# Script que hace todo automáticamente
.\deploy-complete.ps1
```

**Este script:**
1. ✅ Detiene servicios existentes
2. ✅ Construye imágenes Docker
3. ✅ Inicia todos los servicios
4. ✅ Espera a que MySQL esté listo
5. ✅ Importa datos iniciales
6. ✅ Configura Keycloak
7. ✅ Verifica que todo funcione

**Tiempo:** ~5-7 minutos

---

### Opción B: Comandos Manuales Paso a Paso

#### Paso 1: Detener servicios existentes

```powershell
docker-compose down
```

#### Paso 2: Construir imágenes

```powershell
# Construir todas las imágenes
docker-compose build

# O construir solo una
docker-compose build backend-1
docker-compose build frontend
```

#### Paso 3: Iniciar servicios

```powershell
# Iniciar todos los servicios
docker-compose up -d

# Ver logs en tiempo real
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f backend-1
```

#### Paso 4: Esperar a que MySQL esté listo

```powershell
# Ver logs de MySQL
docker-compose logs -f mysql

# Esperar mensaje: "ready for connections"
```

#### Paso 5: Importar datos iniciales

```powershell
# Copiar archivo SQL al contenedor
docker cp seed-data.sql toolrent-mysql:/tmp/seed-data.sql

# Importar datos
docker exec -i toolrent-mysql mysql -uroot -proot123 --default-character-set=utf8mb4 toolrent < /tmp/seed-data.sql

# Verificar datos importados
docker exec -i toolrent-mysql mysql -uroot -proot123 -e "SELECT COUNT(*) FROM toolrent.clients;"
```

#### Paso 6: Verificar servicios

```powershell
# Ver estado de todos los servicios
docker-compose ps

# Verificar salud de backend
curl http://localhost:8090/actuator/health

# Abrir aplicación
start http://localhost
```

---

## 2.2. Ejecución Solo con Imágenes de Docker Hub

**Escenario:** Usar imágenes pre-construidas sin compilar localmente

```powershell
# 1. Descargar imágenes de Docker Hub
docker pull fergusone/toolrent-backend:latest
docker pull fergusone/toolrent-frontend:latest

# 2. Modificar docker-compose.yml temporalmente
# Comentar las secciones "build:" para backend y frontend

# 3. Iniciar servicios
docker-compose up -d

# 4. Importar datos
docker cp seed-data.sql toolrent-mysql:/tmp/
docker exec -i toolrent-mysql mysql -uroot -proot123 toolrent < /tmp/seed-data.sql

# 5. Verificar
start http://localhost
```

---

## 2.3. Comandos de Gestión Docker

### Ver estado de servicios

```powershell
# Ver todos los contenedores corriendo
docker-compose ps

# Ver logs de todos los servicios
docker-compose logs

# Ver logs de un servicio específico
docker-compose logs backend-1

# Ver logs en tiempo real
docker-compose logs -f

# Ver logs de últimos 100 líneas
docker-compose logs --tail=100
```

### Reiniciar servicios

```powershell
# Reiniciar todos los servicios
docker-compose restart

# Reiniciar un servicio específico
docker-compose restart backend-1
docker-compose restart frontend

# Reiniciar MySQL
docker-compose restart mysql
```

### Detener servicios

```powershell
# Detener todos los servicios (mantiene contenedores)
docker-compose stop

# Detener y eliminar contenedores
docker-compose down

# Detener, eliminar contenedores y volúmenes
docker-compose down -v

# Detener un servicio específico
docker-compose stop backend-1
```

### Escalar servicios

```powershell
# Ya tienes 3 réplicas de backend configuradas
# Para cambiar el número:
docker-compose up -d --scale backend-1=5
```

---

## 2.4. Reconstruir Después de Cambios

```powershell
# Si cambias código del backend
docker-compose stop backend-1 backend-2 backend-3
docker-compose build backend-1
docker-compose up -d backend-1 backend-2 backend-3

# Si cambias código del frontend
docker-compose stop frontend
docker-compose build frontend
docker-compose up -d frontend

# Reconstruir todo
docker-compose down
docker-compose build
docker-compose up -d
```

---

## 📊 Resumen Ejecución Docker

### Método Rápido (Un Comando)

```powershell
.\deploy-complete.ps1
```

### Método Manual (Paso a Paso)

```powershell
# 1. Detener servicios anteriores
docker-compose down

# 2. Construir imágenes
docker-compose build

# 3. Iniciar servicios
docker-compose up -d

# 4. Importar datos
docker cp seed-data.sql toolrent-mysql:/tmp/
docker exec -i toolrent-mysql mysql -uroot -proot123 toolrent < /tmp/seed-data.sql

# 5. Verificar
docker-compose ps
start http://localhost
```

**Acceder:**
- Frontend: http://localhost
- Backend: http://localhost:8090
- Keycloak: http://localhost:9090
- MySQL: localhost:3307

---

# 3. Ejecución con Jenkins (CI/CD)

## 📌 Escenario: "Ejecuta el pipeline de Jenkins"

---

## 3.1. Iniciar Jenkins

### Primera Vez (Setup Completo)

```powershell
# Script automatizado
.\setup-jenkins.ps1
```

**El script:**
1. ✅ Crea contenedor Jenkins
2. ✅ Configura volúmenes
3. ✅ Muestra contraseña inicial
4. ✅ Configura permisos Docker

**Después del script:**
1. Abrir http://localhost:8081
2. Ingresar contraseña inicial
3. Instalar plugins sugeridos
4. Crear usuario admin
5. Configurar credenciales Docker Hub

---

### Jenkins Ya Configurado

```powershell
# Iniciar Jenkins (si está detenido)
docker start jenkins

# Verificar que está corriendo
docker ps | findstr jenkins

# Ver logs
docker logs -f jenkins

# Abrir Jenkins
start http://localhost:8081
```

---

## 3.2. Ejecutar Pipeline Manualmente

### Desde la UI de Jenkins

```
1. Abrir: http://localhost:8081
2. Login: admin / admin
3. Click en: "ToolRent-Pipeline"
4. Click en: "Build Now"
5. Ver progreso en "Build History"
6. Click en el número del build (ej: #10)
7. Click en "Console Output" para ver logs
```

### Tiempo esperado:
- ⏱️ Checkout: ~10 segundos
- ⏱️ Test Backend: ~30-60 segundos
- ⏱️ Build Images: ~3-4 minutos
- ⏱️ Push to DockerHub: ~1-2 minutos
- ⏱️ Cleanup: ~10 segundos
- **Total: ~5-7 minutos**

---

## 3.3. Verificar Resultados del Pipeline

```powershell
# Ver último build
start http://localhost:8081/job/ToolRent-Pipeline/lastBuild/

# Ver cobertura JaCoCo
start http://localhost:8081/job/ToolRent-Pipeline/lastBuild/jacoco/

# Ver resultados de tests
start http://localhost:8081/job/ToolRent-Pipeline/lastBuild/testReport/

# Verificar imágenes en Docker Hub
start https://hub.docker.com/r/fergusone/toolrent-backend
start https://hub.docker.com/r/fergusone/toolrent-frontend
```

---

## 3.4. Desplegar Después del Pipeline

Una vez que Jenkins construye y sube las imágenes:

```powershell
# Opción 1: Despliegue completo
.\deploy-complete.ps1

# Opción 2: Solo actualizar con nuevas imágenes
docker-compose pull
docker-compose up -d

# Opción 3: Actualizar servicio específico
docker-compose pull backend-1
docker-compose up -d backend-1 backend-2 backend-3
```

---

## 3.5. Comandos de Gestión de Jenkins

```powershell
# Ver estado de Jenkins
docker ps | findstr jenkins

# Ver logs de Jenkins
docker logs jenkins
docker logs -f jenkins

# Reiniciar Jenkins
docker restart jenkins

# Detener Jenkins
docker stop jenkins

# Iniciar Jenkins
docker start jenkins

# Eliminar Jenkins (borra configuración)
docker stop jenkins
docker rm jenkins
docker volume rm jenkins_home

# Reinstalar Jenkins desde cero
.\setup-jenkins.ps1
```

---

## 📊 Resumen Ejecución Jenkins

### Flujo Completo

```powershell
# 1. Iniciar Jenkins (si no está corriendo)
docker start jenkins

# 2. Abrir navegador
start http://localhost:8081

# 3. Ejecutar pipeline (desde UI)
# → ToolRent-Pipeline → Build Now

# 4. Esperar SUCCESS (~5-7 min)

# 5. Desplegar con nuevas imágenes
.\deploy-complete.ps1

# 6. Verificar aplicación
start http://localhost
```

---

# 4. Comandos de Verificación

## 4.1. Verificar Estado de Servicios

```powershell
# Ver todos los contenedores Docker
docker ps

# Ver servicios de docker-compose
docker-compose ps

# Ver imágenes disponibles
docker images

# Ver redes Docker
docker network ls

# Ver volúmenes Docker
docker volume ls
```

---

## 4.2. Verificar Conectividad

```powershell
# Backend health check
curl http://localhost:8090/actuator/health

# Frontend (debe retornar HTML)
curl http://localhost

# Keycloak (debe retornar JSON)
curl http://localhost:9090/realms/sisgr-realm

# MySQL (desde contenedor)
docker exec -it toolrent-mysql mysql -uroot -proot123 -e "SELECT 1"
```

---

## 4.3. Verificar Logs

```powershell
# Logs de todos los servicios
docker-compose logs

# Logs de un servicio específico
docker-compose logs backend-1
docker-compose logs frontend
docker-compose logs mysql
docker-compose logs keycloak

# Logs en tiempo real
docker-compose logs -f backend-1

# Últimas 50 líneas
docker-compose logs --tail=50 backend-1
```

---

## 4.4. Verificar Base de Datos

```powershell
# Conectar a MySQL
docker exec -it toolrent-mysql mysql -uroot -proot123 toolrent

# Dentro de MySQL:
SHOW TABLES;
SELECT COUNT(*) FROM clients;
SELECT COUNT(*) FROM tools;
SELECT COUNT(*) FROM loans;
EXIT;

# Desde PowerShell (una línea)
docker exec -it toolrent-mysql mysql -uroot -proot123 -e "SELECT COUNT(*) FROM toolrent.clients;"
```

---

## 4.5. Verificar Tests

```powershell
# Ejecutar tests del backend
cd backend-toolrent
.\mvnw test

# Ver reporte de cobertura
start target/site/jacoco/index.html

# Ejecutar tests con logs detallados
.\mvnw test -X
```

---

# 5. Comandos de Troubleshooting

## 5.1. Problemas con Docker

```powershell
# Reiniciar Docker Desktop
# → Tray icon → Restart Docker Desktop

# Limpiar recursos Docker
docker system prune -a
docker volume prune

# Ver uso de espacio
docker system df

# Eliminar contenedores detenidos
docker container prune

# Eliminar imágenes no usadas
docker image prune -a

# Eliminar volúmenes no usados
docker volume prune
```

---

## 5.2. Problemas con Puertos Ocupados

```powershell
# Ver qué proceso usa un puerto
netstat -ano | findstr :8080
netstat -ano | findstr :8090
netstat -ano | findstr :3306

# Matar proceso por PID
taskkill /F /PID <PID>

# Ver todos los puertos en uso
netstat -ano | findstr LISTENING
```

---

## 5.3. Problemas con MySQL

```powershell
# Reiniciar MySQL
docker-compose restart mysql

# Ver logs de MySQL
docker-compose logs mysql

# Conectar y verificar
docker exec -it toolrent-mysql mysql -uroot -proot123

# Reimportar datos
docker cp seed-data.sql toolrent-mysql:/tmp/
docker exec -i toolrent-mysql mysql -uroot -proot123 toolrent < /tmp/seed-data.sql
```

---

## 5.4. Problemas con Keycloak

```powershell
# Reiniciar Keycloak
docker-compose restart keycloak

# Ver logs
docker-compose logs keycloak

# Acceder a admin console
start http://localhost:9090/admin
# Login: admin / admin

# Verificar realm
curl http://localhost:9090/realms/sisgr-realm
```

---

## 5.5. Problemas con Backend

```powershell
# Ver logs
docker-compose logs backend-1

# Reiniciar backend
docker-compose restart backend-1

# Reconstruir backend
docker-compose stop backend-1
docker-compose build backend-1
docker-compose up -d backend-1

# Verificar health
curl http://localhost:8090/actuator/health

# Ver variables de entorno
docker exec backend-1 env
```

---

## 5.6. Problemas con Frontend

```powershell
# Ver logs
docker-compose logs frontend

# Reiniciar frontend
docker-compose restart frontend

# Reconstruir frontend
docker-compose stop frontend
docker-compose build frontend
docker-compose up -d frontend

# Limpiar caché del navegador
# Ctrl + Shift + Delete → Clear cache

# Forzar recarga
# Ctrl + F5
```

---

## 5.7. Problemas con Jenkins

```powershell
# Ver logs
docker logs jenkins

# Reiniciar Jenkins
docker restart jenkins

# Reinstalar Docker CLI en Jenkins
docker exec -u root jenkins bash -c "apt-get update && apt-get install -y docker-ce-cli"

# Verificar permisos
docker exec -u root jenkins chmod 666 /var/run/docker.sock

# Acceder al contenedor Jenkins
docker exec -it jenkins bash
```

---

# 6. Comandos de Git

## 6.1. Comandos Básicos

```powershell
# Ver estado
git status

# Ver cambios
git diff

# Agregar archivos
git add .
git add archivo.txt

# Commit
git commit -m "mensaje"

# Push
git push

# Pull
git pull
```

---

## 6.2. Para la Evaluación

```powershell
# Ver último commit
git log -1

# Ver cambios del último commit
git show

# Ver historial
git log --oneline

# Ver branches
git branch

# Ver remotes
git remote -v
```

---

# 7. Resumen de Escenarios Comunes

## 🎯 Escenario 1: "Ejecuta la aplicación localmente"

```powershell
cd backend-toolrent
.\mvnw spring-boot:run

# Nueva terminal
cd toolrent-frontend
npm run dev

# Abrir: http://localhost:5173
```

---

## 🎯 Escenario 2: "Ejecuta con Docker"

```powershell
.\deploy-complete.ps1

# O manual:
docker-compose up -d
docker cp seed-data.sql toolrent-mysql:/tmp/
docker exec -i toolrent-mysql mysql -uroot -proot123 toolrent < /tmp/seed-data.sql

# Abrir: http://localhost
```

---

## 🎯 Escenario 3: "Ejecuta el pipeline de CI/CD"

```powershell
# Iniciar Jenkins
docker start jenkins

# Abrir navegador
start http://localhost:8081

# → ToolRent-Pipeline → Build Now
# → Esperar SUCCESS

# Desplegar
.\deploy-complete.ps1
```

---

## 🎯 Escenario 4: "Ejecuta los tests"

```powershell
cd backend-toolrent
.\mvnw test

# Ver cobertura
start target/site/jacoco/index.html
```

---

## 🎯 Escenario 5: "Construye las imágenes Docker"

```powershell
# Todas las imágenes
docker-compose build

# Solo backend
docker build -t fergusone/toolrent-backend:latest ./backend-toolrent

# Solo frontend
docker build -t fergusone/toolrent-frontend:latest ./toolrent-frontend
```

---

## 🎯 Escenario 6: "Sube las imágenes a Docker Hub"

```powershell
# Con Jenkins (automático)
start http://localhost:8081
# → Build Now

# Manual
docker login
docker push fergusone/toolrent-backend:latest
docker push fergusone/toolrent-frontend:latest
```

---

# 8. URLs Importantes

```
Aplicación (Local):      http://localhost:5173
Aplicación (Docker):     http://localhost
Backend API:             http://localhost:8090
Backend Health:          http://localhost:8090/actuator/health
Keycloak Admin:          http://localhost:9090/admin
Keycloak Realm:          http://localhost:9090/realms/sisgr-realm
Jenkins:                 http://localhost:8081
Docker Hub Backend:      https://hub.docker.com/r/fergusone/toolrent-backend
Docker Hub Frontend:     https://hub.docker.com/r/fergusone/toolrent-frontend
JaCoCo Report:           target/site/jacoco/index.html
```

---

# 9. Checklist Pre-Evaluación

Antes de la evaluación, verifica:

- [ ] Java 21 instalado: `java -version`
- [ ] Node.js instalado: `node -v`
- [ ] Docker corriendo: `docker ps`
- [ ] Git configurado: `git --version`
- [ ] MySQL funcionando (local o Docker)
- [ ] Keycloak funcionando
- [ ] Jenkins corriendo: `docker ps | findstr jenkins`
- [ ] Credenciales Docker Hub configuradas en Jenkins
- [ ] Pipeline ejecutado exitosamente al menos 1 vez
- [ ] Aplicación funcionando con Docker: `http://localhost`
- [ ] Puedes ejecutar: `.\deploy-complete.ps1`
- [ ] Puedes ejecutar: `.\mvnw test`

---

**¡Listo! Con esta guía puedes ejecutar la aplicación de cualquier forma que el profesor solicite.** 🚀
