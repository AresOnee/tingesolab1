# 🐳 Guía: Subir Imágenes a Docker Hub

## 🎯 Imágenes que Necesitas Subir

Tu proyecto usa **2 imágenes Docker personalizadas**:

1. **Backend:** `fergusone/toolrent-backend:latest`
2. **Frontend:** `fergusone/toolrent-frontend:latest`

**Nota:** MySQL, Keycloak y Nginx usan imágenes oficiales que ya están en Docker Hub.

---

## ✅ Opción 1: Usar Jenkins (RECOMENDADO - Automático)

Jenkins ya está configurado para construir y subir las imágenes automáticamente.

### Pasos:

#### 1. Verificar que Jenkins tiene las credenciales configuradas

Abrir: **http://localhost:8081**

1. **Manage Jenkins** → **Manage Credentials**
2. Verificar que existen:
   - ✅ `dockerhub-credentials` (Username with password)
   - ✅ `dockerhub-username` (Secret text)

Si **NO existen**, configurarlas:

**Credencial 1: dockerhub-credentials**
- Click en **(global)** → **Add Credentials**
- **Kind:** `Username with password`
- **Username:** `fergusone`
- **Password:** [tu password de Docker Hub]
- **ID:** `dockerhub-credentials`
- **Description:** `Docker Hub Login`
- Click **Create**

**Credencial 2: dockerhub-username**
- Click en **Add Credentials** nuevamente
- **Kind:** `Secret text`
- **Secret:** `fergusone`
- **ID:** `dockerhub-username`
- **Description:** `Docker Hub Username`
- Click **Create**

#### 2. Ejecutar el Pipeline

1. Ve a **ToolRent-Pipeline**
2. Click en **"Build Now"**
3. Espera ~5-7 minutos
4. Verás: **"Finished: SUCCESS"**

#### 3. Verificar en Docker Hub

1. Abre: **https://hub.docker.com/u/fergusone**
2. Deberías ver:
   - ✅ `fergusone/toolrent-backend`
   - ✅ `fergusone/toolrent-frontend`

**¡Listo!** Las imágenes están en Docker Hub.

---

## 🔧 Opción 2: Manual con Docker CLI

Si prefieres subirlas manualmente sin Jenkins:

### Paso 1: Login a Docker Hub

```powershell
docker login
```

**Te pedirá:**
- **Username:** `fergusone`
- **Password:** [tu password de Docker Hub]

Deberías ver: `Login Succeeded`

### Paso 2: Construir las Imágenes

```powershell
# Backend
docker build -t fergusone/toolrent-backend:latest ./backend-toolrent

# Frontend
docker build -t fergusone/toolrent-frontend:latest ./toolrent-frontend
```

**Tiempo:** ~3-5 minutos por imagen

### Paso 3: Verificar las Imágenes Localmente

```powershell
docker images | findstr toolrent
```

Deberías ver:
```
fergusone/toolrent-backend    latest    abc123def456   1 minute ago   500MB
fergusone/toolrent-frontend   latest    def456abc789   1 minute ago   50MB
```

### Paso 4: Subir las Imágenes a Docker Hub

```powershell
# Subir Backend
docker push fergusone/toolrent-backend:latest

# Subir Frontend
docker push fergusone/toolrent-frontend:latest
```

**Tiempo:** ~2-5 minutos dependiendo de tu conexión

### Paso 5: Verificar en Docker Hub

Abre: **https://hub.docker.com/u/fergusone**

Deberías ver tus 2 repositorios con la etiqueta `latest`

---

## 🚀 Opción 3: Usando docker-compose (Rápido)

Si ya tienes docker-compose configurado:

```powershell
# Construir todas las imágenes
docker-compose build

# Subir las imágenes
docker-compose push
```

**Nota:** Esto requiere estar logueado con `docker login` primero.

---

## 📊 Comparación de Métodos

| Método | Tiempo | Tests | CI/CD | Recomendado |
|--------|--------|-------|-------|-------------|
| **Jenkins** | 5-7 min | ✅ Sí | ✅ Sí | ✅ **Para evaluación** |
| **Manual** | 5-10 min | ❌ No | ❌ No | ⚠️ Solo desarrollo |
| **Docker Compose** | 3-5 min | ❌ No | ❌ No | ⚠️ Solo desarrollo |

---

## 🔍 Verificar que las Imágenes Están en Docker Hub

### Método 1: Navegador Web

1. Abre: **https://hub.docker.com/**
2. Login con tu cuenta `fergusone`
3. Ve a **Repositories**
4. Deberías ver:
   ```
   fergusone/toolrent-backend    ✅
   fergusone/toolrent-frontend   ✅
   ```

### Método 2: Docker Pull (Prueba)

Desde cualquier computadora con Docker:

```bash
# Intentar descargar tus imágenes
docker pull fergusone/toolrent-backend:latest
docker pull fergusone/toolrent-frontend:latest
```

Si se descargan correctamente, significa que están públicas en Docker Hub.

---

## 🎯 Flujo Completo Recomendado (Jenkins)

```
┌─────────────────┐
│ 1. CÓDIGO       │
│    Git push     │
└────────┬────────┘
         │
┌────────▼────────┐
│ 2. JENKINS      │
│    Pipeline     │
│    - Checkout   │
│    - Test       │ ✅ 92.25% coverage
│    - Build      │ 🐳 Docker build
│    - Push       │ ⬆️ Push to Docker Hub
└────────┬────────┘
         │
┌────────▼────────┐
│ 3. DOCKER HUB   │
│    Imágenes     │
│    disponibles  │
│    públicamente │
└────────┬────────┘
         │
┌────────▼────────┐
│ 4. DEPLOYMENT   │
│    docker pull  │
│    & compose up │
└─────────────────┘
```

---

## 🐛 Troubleshooting

### Problema 1: "docker login" falla

**Error:** `Error saving credentials: The stub received bad data`

**Solución:**
```powershell
# Borrar credenciales antiguas
del %USERPROFILE%\.docker\config.json

# Intentar login nuevamente
docker login
```

### Problema 2: "denied: requested access to the resource is denied"

**Causa:** No estás autenticado o no tienes permisos

**Solución:**
```powershell
# Verificar que estás logueado
docker info | findstr Username

# Si no aparece, hacer login
docker login
```

### Problema 3: "unauthorized: authentication required"

**Causa:** Token expirado

**Solución:**
```powershell
# Logout y volver a login
docker logout
docker login
```

### Problema 4: Build falla con "No space left on device"

**Solución:**
```powershell
# Limpiar imágenes y contenedores no usados
docker system prune -a

# Verificar espacio
docker system df
```

### Problema 5: Push es muy lento

**Causa:** Imágenes muy grandes

**Solución:**
- Usa conexión rápida (no WiFi público)
- El primer push es lento, los siguientes son incrementales
- Jenkins ya optimiza con multi-stage builds

---

## 📝 Verificación Post-Push

Después de subir las imágenes, verifica:

### 1. En Docker Hub Web

```
https://hub.docker.com/r/fergusone/toolrent-backend
https://hub.docker.com/r/fergusone/toolrent-frontend
```

Deberías ver:
- ✅ Tamaño de la imagen
- ✅ Última actualización (timestamp)
- ✅ Tags disponibles (latest)
- ✅ Pull command

### 2. Información de la Imagen

```powershell
# Ver metadatos de la imagen en Docker Hub
docker manifest inspect fergusone/toolrent-backend:latest
docker manifest inspect fergusone/toolrent-frontend:latest
```

### 3. Probar Pull desde Docker Hub

```powershell
# Eliminar imagen local
docker rmi fergusone/toolrent-backend:latest

# Descargar desde Docker Hub
docker pull fergusone/toolrent-backend:latest

# Verificar
docker images | findstr toolrent-backend
```

Si funciona, significa que la imagen está correctamente subida y disponible.

---

## 💡 Información Adicional

### Tamaño de las Imágenes

**Backend (~500 MB):**
- OpenJDK 21
- Spring Boot JAR
- Dependencias de Maven

**Frontend (~50 MB):**
- Nginx Alpine
- Archivos estáticos compilados de React

### Tags de Versión

Actualmente usas `latest`, pero puedes versionar:

```bash
# Jenkins usa el build number automáticamente
docker tag fergusone/toolrent-backend:latest fergusone/toolrent-backend:v1.0.0
docker push fergusone/toolrent-backend:v1.0.0
```

El Jenkinsfile ya hace esto con `${BUILD_NUMBER}`.

### Imágenes Públicas vs Privadas

Por defecto, las imágenes son **públicas**. Si quieres hacerlas privadas:

1. En Docker Hub → Repository Settings
2. Cambiar a **Private**
3. **Nota:** Docker Hub Free tiene límite de 1 repositorio privado

---

## 🎓 Para la Evaluación

### Demuestra el Flujo Completo

1. **Muestra el código** en el repositorio
2. **Ejecuta Jenkins** → Build Now
3. **Muestra el pipeline** ejecutándose
4. **Abre Docker Hub** en el navegador
5. **Muestra las imágenes** actualizadas con timestamp reciente
6. **Explica:** "Jenkins automatiza todo el flujo de CI/CD"

### Preguntas del Profesor

**P: ¿Por qué usar Docker Hub?**
**R:**
- Almacenamiento centralizado de imágenes
- Versionado de imágenes
- Facilita deployment en múltiples entornos
- Cualquier máquina puede descargar las imágenes

**P: ¿Cuánto tiempo tarda en subir una imagen?**
**R:**
- Primera vez: 2-5 minutos (sube todas las capas)
- Siguientes veces: 30 segundos (solo capas modificadas)
- Jenkins lo hace automáticamente en paralelo

**P: ¿Qué pasa si la imagen ya existe?**
**R:**
- Docker Hub la sobrescribe (usando el tag `latest`)
- Las capas no modificadas se reutilizan
- Solo se suben las capas nuevas o modificadas

---

## 📋 Checklist de Verificación

Antes de decir "las imágenes están subidas":

- [ ] `docker login` exitoso
- [ ] Imágenes construidas localmente (o por Jenkins)
- [ ] `docker push` completado sin errores
- [ ] Imágenes visibles en https://hub.docker.com/u/fergusone
- [ ] Timestamp actualizado en Docker Hub
- [ ] `docker pull` funciona desde otra terminal
- [ ] Jenkins pipeline con stage "Push to DockerHub" en verde

---

## 🚀 Resumen de Comandos

```powershell
# === MÉTODO MANUAL ===

# 1. Login a Docker Hub
docker login
# Username: fergusone
# Password: [tu password]

# 2. Construir imágenes
docker build -t fergusone/toolrent-backend:latest ./backend-toolrent
docker build -t fergusone/toolrent-frontend:latest ./toolrent-frontend

# 3. Verificar imágenes locales
docker images | findstr toolrent

# 4. Subir a Docker Hub
docker push fergusone/toolrent-backend:latest
docker push fergusone/toolrent-frontend:latest

# 5. Verificar en navegador
start https://hub.docker.com/u/fergusone

# === MÉTODO JENKINS (RECOMENDADO) ===

# 1. Abrir Jenkins
start http://localhost:8081

# 2. ToolRent-Pipeline → Build Now

# 3. Esperar SUCCESS

# 4. Verificar Docker Hub
start https://hub.docker.com/u/fergusone
```

---

## 📚 Archivos Relacionados

- `Jenkinsfile` - Pipeline que sube automáticamente
- `docker-compose.yml` - Configuración de imágenes
- `backend-toolrent/Dockerfile` - Build del backend
- `toolrent-frontend/Dockerfile` - Build del frontend

---

## ✅ Resultado Esperado

Después de seguir esta guía, deberías tener:

```
https://hub.docker.com/r/fergusone/toolrent-backend
├── Tags: latest, 7, 8, 9, ... (build numbers)
├── Size: ~500 MB
└── Last pushed: hace X minutos

https://hub.docker.com/r/fergusone/toolrent-frontend
├── Tags: latest, 7, 8, 9, ... (build numbers)
├── Size: ~50 MB
└── Last pushed: hace X minutos
```

**¡Listo! Tus imágenes están disponibles públicamente en Docker Hub.** 🎉

---

**Recomendación Final:** Usa **Jenkins** para subir las imágenes durante la evaluación, ya que demuestra todo el flujo de CI/CD automatizado. 🚀
