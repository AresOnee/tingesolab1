# 🪟 GUÍA DE DESPLIEGUE PARA WINDOWS

Esta guía está específicamente diseñada para usuarios de Windows con PowerShell.

---

## ✅ PREREQUISITOS

1. **Docker Desktop para Windows** instalado y corriendo
   - Descargar de: https://www.docker.com/products/docker-desktop
   - Asegúrate de que esté iniciado (ícono en la bandeja del sistema)

2. **PowerShell** (viene instalado con Windows 10/11)
   - Busca "PowerShell" en el menú inicio
   - Click derecho → "Ejecutar como administrador"

3. **Git para Windows** (para clonar el repositorio)
   - Descargar de: https://git-scm.com/download/win

4. **Cuenta en Docker Hub**
   - Crear gratis en: https://hub.docker.com/

---

## 🚀 PASO A PASO COMPLETO

### PASO 1: Clonar el repositorio

Abre PowerShell y ejecuta:

```powershell
# Navegar a tu carpeta de proyectos (ejemplo)
cd C:\Users\TuUsuario\Desktop

# Clonar el repositorio
git clone https://github.com/AresOnee/tingesolab1.git

# Entrar al proyecto
cd tingesolab1
```

---

### PASO 2: Configurar variable de Docker Hub

```powershell
# Reemplaza "tu-usuario" con tu usuario de Docker Hub
$env:DOCKER_USERNAME="tu-usuario"
```

**⚠️ IMPORTANTE:** Esta variable solo dura mientras la ventana de PowerShell esté abierta.

**Para que persista entre sesiones** (opcional):
```powershell
[System.Environment]::SetEnvironmentVariable("DOCKER_USERNAME", "tu-usuario", "User")
```

Verificar que se configuró correctamente:
```powershell
echo $env:DOCKER_USERNAME
```

Deberías ver tu usuario de Docker Hub.

---

### PASO 3: Ver los comandos disponibles

```powershell
# Ver ayuda del script
.\deploy.ps1
```

Verás algo como esto:

```
============================================
   TOOLRENT - SCRIPT DE DESPLIEGUE
============================================

Uso: .\deploy.ps1 [comando] [opciones]

Comandos disponibles:

  build [push]     - Construir imágenes localmente
  pull             - Pull desde Docker Hub y desplegar
  down             - Detener todos los servicios
  clean            - Detener servicios y limpiar todo
  logs [servicio]  - Ver logs de servicios
  status           - Ver estado de servicios
  restart [serv]   - Reiniciar servicios
```

---

### PASO 4: Desplegar la aplicación

Tienes 2 opciones:

#### **Opción A: Build local** (Recomendado para primera vez)

```powershell
# Construir imágenes y desplegar
.\deploy.ps1 build

# Esto tomará varios minutos la primera vez
# Verás mensajes de descarga de dependencias
```

Si quieres también subir las imágenes a Docker Hub:

```powershell
# Build local + push a Docker Hub + desplegar
.\deploy.ps1 build push
```

#### **Opción B: Pull desde Docker Hub** (Solo si ya subiste las imágenes)

```powershell
# Descargar imágenes y desplegar
.\deploy.ps1 pull
```

---

### PASO 5: Verificar que todo esté funcionando

```powershell
# Ver estado de los servicios
.\deploy.ps1 status
```

Deberías ver:

```
ESTADO DE SERVICIOS
Name                    Command               State   Ports
-------------------------------------------------------------
toolrent-backend-1      ...                  Up      8090/tcp
toolrent-backend-2      ...                  Up      8090/tcp
toolrent-backend-3      ...                  Up      8090/tcp
toolrent-frontend       ...                  Up      0.0.0.0:80->80/tcp
toolrent-mysql          ...                  Up      0.0.0.0:3306->3306/tcp
toolrent-keycloak       ...                  Up      0.0.0.0:9090->8080/tcp
toolrent-nginx-backend  ...                  Up      0.0.0.0:8090->80/tcp

HEALTH CHECKS
Backend: OK
Frontend: OK
```

---

### PASO 6: Abrir la aplicación en el navegador

Abre tu navegador (Chrome, Edge, Firefox) y ve a:

- **Frontend**: http://localhost
- **Backend API**: http://localhost:8090
- **Keycloak Admin**: http://localhost:9090

---

## 📝 COMANDOS ÚTILES

### Ver logs en tiempo real

```powershell
# Ver logs de todos los servicios
.\deploy.ps1 logs

# Ver logs de un servicio específico
.\deploy.ps1 logs backend-1
.\deploy.ps1 logs frontend
.\deploy.ps1 logs mysql

# Presiona Ctrl+C para salir de los logs
```

### Reiniciar un servicio

```powershell
# Reiniciar todos los servicios
.\deploy.ps1 restart

# Reiniciar un servicio específico
.\deploy.ps1 restart backend-1
```

### Detener la aplicación

```powershell
# Detener todos los servicios (pero mantener volúmenes)
.\deploy.ps1 down
```

### Limpiar todo

```powershell
# Eliminar contenedores, volúmenes e imágenes
.\deploy.ps1 clean

# Te pedirá confirmación:
# ¿Estás seguro? (S/N): S
```

---

## 🐛 SOLUCIÓN DE PROBLEMAS

### ❌ Error: "No se puede ejecutar scripts en este sistema"

**Problema**: PowerShell tiene restricciones de seguridad.

**Solución**:
```powershell
# Abre PowerShell como Administrador y ejecuta:
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Confirma con "S" (Sí)
```

---

### ❌ Error: "Docker no está corriendo"

**Solución**:
1. Busca "Docker Desktop" en el menú inicio
2. Abrelo y espera a que inicie (puede tomar 1-2 minutos)
3. Verás un ícono de ballena en la bandeja del sistema cuando esté listo
4. Vuelve a intentar el comando

---

### ❌ Error: "Variable DOCKER_USERNAME no está configurada"

**Solución**:
```powershell
# Configurar la variable (reemplaza "tu-usuario")
$env:DOCKER_USERNAME="tu-usuario"

# Verificar
echo $env:DOCKER_USERNAME
```

---

### ❌ Error: "The system cannot find the path specified"

**Problema**: No estás en la carpeta del proyecto.

**Solución**:
```powershell
# Ver dónde estás
pwd

# Debería mostrar algo como: C:\Users\...\tingesolab1

# Si no estás ahí, navega:
cd C:\Users\TuUsuario\Desktop\tingesolab1
```

---

### ❌ Los servicios demoran mucho en iniciar

**Es normal**, la primera vez toma varios minutos:
- MySQL: ~30 segundos
- Keycloak: ~60 segundos
- Backend: ~45 segundos (cada réplica)

Puedes ver el progreso con:
```powershell
.\deploy.ps1 logs
```

---

### ❌ Frontend no carga / Error de conexión

1. Verifica que Docker Desktop esté corriendo
2. Verifica el estado:
   ```powershell
   .\deploy.ps1 status
   ```
3. Si algún servicio está "unhealthy", reinícialo:
   ```powershell
   .\deploy.ps1 restart frontend
   ```
4. Espera 30 segundos y recarga la página

---

## 📊 COMANDOS DOCKER DIRECTOS (Avanzado)

Si prefieres usar Docker directamente:

```powershell
# Ver contenedores corriendo
docker ps

# Ver logs de un contenedor
docker logs -f toolrent-frontend

# Entrar a un contenedor (shell)
docker exec -it toolrent-backend-1 sh

# Ver uso de recursos
docker stats

# Detener todo
docker-compose down

# Ver imágenes
docker images

# Ver volúmenes
docker volume ls
```

---

## 🎯 FLUJO COMPLETO PARA LA EVALUACIÓN

### Primera vez (Local):

```powershell
# 1. Configurar usuario
$env:DOCKER_USERNAME="tu-usuario"

# 2. Build y desplegar
.\deploy.ps1 build

# 3. Verificar
.\deploy.ps1 status

# 4. Abrir navegador
start http://localhost
```

### Con Docker Hub (Para presentación):

```powershell
# 1. Login en Docker Hub
docker login
# Ingresa tu usuario y password

# 2. Build, push y desplegar
.\deploy.ps1 build push

# 3. Verificar
.\deploy.ps1 status

# 4. Las imágenes ahora están en Docker Hub
# Puedes verlas en: https://hub.docker.com/u/tu-usuario
```

### Limpiar después de la presentación:

```powershell
# Detener y limpiar todo
.\deploy.ps1 clean
```

---

## 📱 ACCESOS RÁPIDOS

Después de ejecutar `.\deploy.ps1 build`, puedes abrir:

| Servicio | URL | Credenciales |
|----------|-----|--------------|
| **Frontend** | http://localhost | (Requiere Keycloak) |
| **Backend API** | http://localhost:8090/actuator/health | N/A |
| **Swagger UI** | http://localhost:8090/swagger-ui.html | (Requiere Keycloak) |
| **Keycloak Admin** | http://localhost:9090 | admin / admin |
| **MySQL** | localhost:3306 | toolrent / toolrent123 |

---

## 💡 TIPS Y TRUCOS

### 1. Abrir todo con un solo comando:

```powershell
# Desplegar y abrir navegador
.\deploy.ps1 build
start http://localhost
```

### 2. Ver logs de múltiples servicios:

```powershell
# En una ventana de PowerShell
.\deploy.ps1 logs backend-1

# En otra ventana de PowerShell
.\deploy.ps1 logs frontend
```

### 3. Verificar health checks manualmente:

```powershell
# Backend
curl http://localhost:8090/actuator/health

# Frontend
curl http://localhost/health
```

### 4. Reconstruir solo una imagen:

```powershell
# Backend
cd backend-toolrent
docker build -t $env:DOCKER_USERNAME/toolrent-backend:latest .
cd ..

# Frontend
cd toolrent-frontend
docker build -t $env:DOCKER_USERNAME/toolrent-frontend:latest .
cd ..

# Reiniciar el servicio
docker-compose restart backend-1
```

---

## ✅ CHECKLIST PARA LA EVALUACIÓN

- [ ] Docker Desktop instalado y corriendo
- [ ] Variable `DOCKER_USERNAME` configurada
- [ ] Ejecutado: `.\deploy.ps1 build`
- [ ] Estado verificado: `.\deploy.ps1 status` (todos "Up")
- [ ] Frontend accesible en http://localhost
- [ ] Backend responde en http://localhost:8090/actuator/health
- [ ] Imágenes subidas a Docker Hub (opcional): `.\deploy.ps1 build push`
- [ ] Screenshots/evidencia tomados
- [ ] Logs funcionando: `.\deploy.ps1 logs`

---

## 🆘 AYUDA ADICIONAL

Si tienes problemas:

1. **Revisa los logs**:
   ```powershell
   .\deploy.ps1 logs
   ```

2. **Reinicia Docker Desktop**:
   - Click derecho en el ícono de Docker → "Restart"

3. **Limpia y vuelve a intentar**:
   ```powershell
   .\deploy.ps1 clean
   .\deploy.ps1 build
   ```

4. **Verifica que Docker Compose funcione**:
   ```powershell
   docker-compose --version
   # Debe mostrar: Docker Compose version v2.x.x
   ```

---

**¡Listo! Ahora puedes desplegar ToolRent en Windows fácilmente.** 🎉
