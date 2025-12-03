# ⚡ Jenkins Quick Start - Guía Rápida 5 Minutos

## 🎯 Objetivo
Configurar Jenkins y ejecutar el pipeline CI/CD en menos de 5 minutos.

---

## 📋 Pre-requisitos
- ✅ Docker Desktop instalado y corriendo
- ✅ Cuenta en Docker Hub
- ✅ Git configurado

---

## 🚀 Pasos Rápidos

### 1. Ejecutar Script de Configuración (1 minuto)

```powershell
# Navega al directorio del proyecto
cd tingesolab1

# Ejecuta el script de setup
.\setup-jenkins.ps1
```

**El script automáticamente:**
- Crea contenedor Jenkins
- Configura volúmenes
- Obtiene la contraseña inicial
- Muestra las instrucciones

---

### 2. Acceder a Jenkins (30 segundos)

1. Abrir: **http://localhost:8081**
2. Ingresar la contraseña mostrada por el script
3. Click: **"Install suggested plugins"**
4. Esperar a que instale (~2 minutos)

---

### 3. Crear Usuario Admin (30 segundos)

- Username: `admin`
- Password: `admin`
- Full name: `Admin ToolRent`
- Email: `admin@toolrent.local`
- Click: **Save and Continue** → **Save and Finish**

---

### 4. Configurar Credenciales Docker Hub (1 minuto)

1. **Manage Jenkins** → **Manage Credentials**
2. Click en **(global)** → **Add Credentials**

**Primera Credencial:**
- Kind: `Username with password`
- Username: `tu-usuario-dockerhub`
- Password: `tu-password-dockerhub`
- ID: `dockerhub-credentials`
- Click **Create**

**Segunda Credencial:**
- Kind: `Secret text`
- Secret: `tu-usuario-dockerhub`
- ID: `dockerhub-username`
- Click **Create**

---

### 5. Instalar Plugins Requeridos (2 minutos)

1. **Manage Jenkins** → **Manage Plugins** → **Available**
2. Buscar e instalar:
   - ☑ `Jacoco`
   - ☑ `Docker Pipeline`
3. Click **Install without restart**
4. Esperar a que termine

---

### 6. Crear Pipeline (1 minuto)

1. Dashboard → **New Item**
2. Nombre: `ToolRent-Pipeline`
3. Tipo: **Pipeline**
4. Click **OK**

**Configurar Pipeline:**
- Definition: `Pipeline script from SCM`
- SCM: `Git`
- Repository URL: `https://github.com/AresOnee/tingesolab1.git`
- Branch: `*/main`
- Script Path: `Jenkinsfile`
- Click **Save**

---

### 7. Ejecutar Pipeline (10-15 minutos)

1. Click **Build Now**
2. Ver progreso en **Build History** → Click en #1
3. Click **Console Output** para ver logs

**Esperar a que termine:**
```
✅ Checkout
✅ Test Backend (con Jacoco)
✅ Build Docker Images
✅ Push to DockerHub
✅ Cleanup
```

---

### 8. Verificar Resultados (30 segundos)

**En Jenkins:**
- Ver porcentaje de cobertura Jacoco (debe ser ≥90%)
- Verificar que el build esté verde ✅

**En Docker Hub:**
- Verificar que las imágenes estén actualizadas:
  - `tu-usuario/toolrent-backend:latest`
  - `tu-usuario/toolrent-frontend:latest`

---

## ✅ Checklist Final

Antes de la evaluación, verificar:

- [ ] Jenkins corriendo en http://localhost:8081
- [ ] Usuario admin creado
- [ ] Credenciales Docker Hub configuradas
- [ ] Pipeline `ToolRent-Pipeline` creado
- [ ] Al menos 1 build exitoso (verde)
- [ ] Jacoco muestra ≥90% cobertura
- [ ] Imágenes en Docker Hub actualizadas
- [ ] Puedes ejecutar `.\deploy-complete.ps1` exitosamente

---

## 🔥 Comandos de Emergencia

### Si algo falla:

```powershell
# Reiniciar Jenkins
docker restart jenkins

# Ver logs
docker logs -f jenkins

# Acceder al contenedor
docker exec -it jenkins bash

# Eliminar y volver a crear
docker stop jenkins
docker rm jenkins
.\setup-jenkins.ps1
```

---

## 📊 Dashboard de Jenkins

Después del primer build exitoso, podrás ver:

```
ToolRent-Pipeline
├── Build #1 ✅ SUCCESS
│   ├── Console Output
│   ├── Test Result (JUnit)
│   ├── Code Coverage (Jacoco) - 92%
│   └── Build Artifacts
├── Workspace
└── Pipeline
```

---

## 🎯 Para la Evaluación

**Demostrar:**

1. **Jenkinsfile en GitHub** ✅
2. **Pipeline configurado** ✅
3. **Ejecutar build manualmente** ✅
4. **Mostrar resultados:**
   - Tests pasando
   - Cobertura ≥90%
   - Imágenes en Docker Hub
5. **Desplegar con las imágenes:**
   ```powershell
   .\deploy-complete.ps1
   ```

---

## 💡 Tips Importantes

1. **Primera ejecución es lenta**: El primer build toma ~15 minutos (descarga dependencias)
2. **Builds posteriores más rápidos**: ~5-8 minutos
3. **Si falla un test**: El pipeline se detiene (esto es correcto)
4. **Jacoco debe mostrar ≥90%**: Verificar en cada build

---

## 📚 Documentación Completa

Para más detalles, ver: **JENKINS-SETUP.md**

---

**¡Listo para automatizar! 🚀**
