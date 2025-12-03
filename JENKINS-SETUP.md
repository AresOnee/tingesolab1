# 🚀 Guía Completa de Automatización con Jenkins

## 📋 Tabla de Contenidos
1. [Instalación de Jenkins](#1-instalación-de-jenkins)
2. [Configuración Inicial](#2-configuración-inicial)
3. [Instalación de Plugins Necesarios](#3-instalación-de-plugins)
4. [Configuración de Credenciales](#4-configuración-de-credenciales)
5. [Creación del Pipeline](#5-creación-del-pipeline)
6. [Ejecución y Monitoreo](#6-ejecución-y-monitoreo)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Instalación de Jenkins

### Opción A: Instalación con Docker (Recomendado)

```powershell
# 1. Crear volumen para persistir datos de Jenkins
docker volume create jenkins_home

# 2. Ejecutar Jenkins en Docker
docker run -d `
  --name jenkins `
  -p 8081:8080 `
  -p 50000:50000 `
  -v jenkins_home:/var/jenkins_home `
  -v //var/run/docker.sock:/var/run/docker.sock `
  jenkins/jenkins:lts

# 3. Obtener la contraseña inicial
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### Opción B: Instalación en Windows

1. Descargar Jenkins desde: https://www.jenkins.io/download/
2. Ejecutar el instalador `.msi`
3. Seguir el asistente de instalación
4. Acceder a: http://localhost:8081

---

## 2. Configuración Inicial

### 2.1 Primera Configuración

1. **Acceder a Jenkins**: http://localhost:8081
2. **Ingresar contraseña inicial** (obtenida en paso anterior)
3. **Instalar plugins sugeridos**
4. **Crear usuario administrador**:
   - Username: `admin`
   - Password: `admin` (o tu preferencia)
   - Full name: `Admin ToolRent`
   - Email: `admin@toolrent.local`

### 2.2 Configurar Herramientas Globales

1. Ir a: **Manage Jenkins** → **Global Tool Configuration**

2. **Configurar Maven**:
   - Nombre: `Maven-3.9`
   - Versión: `3.9.x` (Install automatically)

3. **Configurar JDK**:
   - Nombre: `JDK-17`
   - Seleccionar: Install automatically
   - Versión: `Java 17`

4. **Configurar Docker** (si no está instalado):
   - Nombre: `Docker`
   - Install automatically

---

## 3. Instalación de Plugins

### 3.1 Plugins Necesarios

Ir a: **Manage Jenkins** → **Manage Plugins** → **Available**

Instalar los siguientes plugins:

```
✅ Pipeline
✅ Git Plugin
✅ Docker Pipeline
✅ Docker Plugin
✅ JUnit Plugin
✅ Jacoco Plugin
✅ Credentials Binding Plugin
✅ Blue Ocean (opcional, para mejor UI)
```

### 3.2 Instalación Rápida

```groovy
// En la consola de Jenkins (Manage Jenkins → Script Console)
def plugins = [
    'workflow-aggregator',
    'git',
    'docker-workflow',
    'docker-plugin',
    'junit',
    'jacoco',
    'credentials-binding',
    'blueocean'
]

plugins.each { plugin ->
    println "Installing ${plugin}..."
    Jenkins.instance.updateCenter.getPlugin(plugin).deploy()
}
```

---

## 4. Configuración de Credenciales

### 4.1 Credenciales de Docker Hub

1. Ir a: **Manage Jenkins** → **Manage Credentials**
2. Click en **(global)** → **Add Credentials**

**Credencial 1: Docker Hub Username/Password**
- Kind: `Username with password`
- Scope: `Global`
- Username: `tu-usuario-dockerhub`
- Password: `tu-password-dockerhub`
- ID: `dockerhub-credentials`
- Description: `Docker Hub Login`

**Credencial 2: Docker Hub Username (solo texto)**
- Kind: `Secret text`
- Scope: `Global`
- Secret: `tu-usuario-dockerhub`
- ID: `dockerhub-username`
- Description: `Docker Hub Username`

### 4.2 Credenciales de GitHub (opcional)

Si tu repositorio es privado:

- Kind: `Username with password`
- Scope: `Global`
- Username: `tu-usuario-github`
- Password: `tu-personal-access-token`
- ID: `github-credentials`

---

## 5. Creación del Pipeline

### 5.1 Crear Nuevo Job

1. En Jenkins Dashboard, click **New Item**
2. Nombre: `ToolRent-Pipeline`
3. Tipo: **Pipeline**
4. Click **OK**

### 5.2 Configurar Pipeline

**General Tab:**
- ✅ Description: `Pipeline CI/CD para ToolRent Application`
- ✅ Discard old builds: Keep last 10 builds

**Build Triggers:**
- ☑ Poll SCM: `H/5 * * * *` (cada 5 minutos)
- O ☑ GitHub hook trigger (si configuraste webhook)

**Pipeline Tab:**
- Definition: `Pipeline script from SCM`
- SCM: `Git`
- Repository URL: `https://github.com/AresOnee/tingesolab1.git`
- Branch: `*/main` (o tu branch)
- Script Path: `Jenkinsfile`

### 5.3 Configuración Avanzada (Opcional)

```groovy
// En Pipeline → Pipeline Syntax → Snippet Generator
// Puedes generar snippets de código
```

---

## 6. Ejecución y Monitoreo

### 6.1 Ejecutar el Pipeline

**Opción 1: Manual**
1. En el Dashboard, click en `ToolRent-Pipeline`
2. Click **Build Now**
3. Ver el progreso en **Build History**

**Opción 2: Automático (Git Hook)**
- Cada vez que hagas `git push`, Jenkins ejecutará automáticamente

### 6.2 Monitorear Ejecución

**Ver Logs en Tiempo Real:**
1. Click en el número de build (#1, #2, etc.)
2. Click **Console Output**
3. Ver logs en tiempo real

**Ver Stages Visuales:**
1. Instalar Blue Ocean plugin
2. Acceder a: http://localhost:8081/blue/
3. Ver pipeline visual con stages

### 6.3 Resultados Esperados

```
✅ Stage 1: Checkout            (~10 segundos)
✅ Stage 2: Test Backend        (~2-3 minutos)
✅ Stage 3: Build Docker Images (~5-8 minutos)
✅ Stage 4: Push to DockerHub   (~2-3 minutos)
✅ Stage 5: Cleanup             (~5 segundos)
```

**Total: ~10-15 minutos**

---

## 7. Troubleshooting

### Problema 1: "Docker not found"

**Error:**
```
docker: command not found
```

**Solución:**
```powershell
# Asegurarse de que Docker está instalado y corriendo
docker --version

# Si Jenkins está en Docker, montarlo correctamente
docker run -d `
  --name jenkins `
  -v //var/run/docker.sock:/var/run/docker.sock `
  jenkins/jenkins:lts

# Dar permisos a Jenkins dentro del contenedor
docker exec -u root jenkins chmod 666 /var/run/docker.sock
```

---

### Problema 2: "Permission Denied" al ejecutar mvnw

**Error:**
```
Permission denied: ./mvnw
```

**Solución:**
Agregar en el Jenkinsfile antes de ejecutar mvnw:
```groovy
sh 'chmod +x mvnw'
./mvnw clean test
```

---

### Problema 3: "Jacoco Plugin not found"

**Solución:**
1. Ir a **Manage Jenkins** → **Manage Plugins**
2. Buscar e instalar `Jacoco Plugin`
3. Reiniciar Jenkins

---

### Problema 4: Tests Fallan en Jenkins pero funcionan localmente

**Causas comunes:**
- Diferente versión de Java
- Variables de entorno faltantes
- Base de datos no disponible

**Solución:**
```groovy
// En el Jenkinsfile, agregar:
environment {
    JAVA_HOME = tool 'JDK-17'
    PATH = "${JAVA_HOME}/bin:${env.PATH}"
}
```

---

### Problema 5: Docker Hub Push Falla

**Error:**
```
denied: requested access to the resource is denied
```

**Solución:**
1. Verificar credenciales en Jenkins
2. Hacer login manual en Jenkins:
```bash
docker exec jenkins docker login -u username -p password
```

---

## 📊 Verificación de Cobertura de Tests

### Ver Reporte Jacoco en Jenkins

1. Después de ejecutar el build exitoso
2. En la página del build, ver **Code Coverage**
3. Click en el porcentaje para ver detalles
4. Verificar que sea ≥ 90%

### Acceder al Reporte HTML

```
http://localhost:8081/job/ToolRent-Pipeline/lastBuild/jacoco/
```

---

## 🔄 Flujo Completo de CI/CD

### Desarrollo → Despliegue

```
1. Desarrollador hace cambios en código
   ↓
2. git add . && git commit -m "mensaje"
   ↓
3. git push origin main
   ↓
4. Jenkins detecta cambios (webhook o polling)
   ↓
5. Pipeline se ejecuta automáticamente:
   - ✅ Checkout código
   - ✅ Ejecuta tests (debe pasar 90% cobertura)
   - ✅ Build imágenes Docker
   - ✅ Push a Docker Hub
   ↓
6. Imágenes disponibles en Docker Hub
   ↓
7. Despliegue local con: .\deploy-complete.ps1
   (pull de Docker Hub → deploy con compose)
```

---

## 📝 Configuración Recomendada del Jenkinsfile

El proyecto ya tiene un `Jenkinsfile` completo. Asegúrate de tener:

```groovy
environment {
    DOCKER_CREDENTIALS_ID = 'dockerhub-credentials'
    DOCKER_USERNAME = credentials('dockerhub-username')
}
```

**Estos IDs deben coincidir con los configurados en Credentials**

---

## 🎯 Checklist Pre-Evaluación

### Antes de la evaluación, verificar:

- [ ] Jenkins corriendo en http://localhost:8081
- [ ] Credenciales de Docker Hub configuradas
- [ ] Pipeline `ToolRent-Pipeline` creado
- [ ] Al menos 1 build exitoso
- [ ] Reporte Jacoco muestra ≥90% cobertura
- [ ] Imágenes en Docker Hub actualizadas
- [ ] `deploy-complete.ps1` funciona correctamente

---

## 🚀 Comandos Rápidos Jenkins

### Iniciar Jenkins (Docker)
```powershell
docker start jenkins
```

### Detener Jenkins
```powershell
docker stop jenkins
```

### Ver logs de Jenkins
```powershell
docker logs -f jenkins
```

### Reiniciar Jenkins
```powershell
docker restart jenkins
```

### Backup de Jenkins
```powershell
docker cp jenkins:/var/jenkins_home ./jenkins_backup
```

---

## 📚 Recursos Adicionales

- **Documentación Jenkins**: https://www.jenkins.io/doc/
- **Pipeline Syntax**: https://www.jenkins.io/doc/book/pipeline/syntax/
- **Jacoco Plugin**: https://plugins.jenkins.io/jacoco/
- **Docker Pipeline**: https://plugins.jenkins.io/docker-workflow/

---

## ✅ Siguiente Paso

Después de configurar Jenkins:

```powershell
# 1. Hacer un cambio en el código
git add .
git commit -m "test: Trigger Jenkins pipeline"
git push

# 2. Ver en Jenkins que el pipeline se ejecuta
# 3. Esperar a que termine exitosamente
# 4. Verificar imágenes en Docker Hub
# 5. Desplegar localmente
.\deploy-complete.ps1
```

**¡Tu pipeline CI/CD está listo! 🎉**
