# 🚀 Despliegue Rápido - ToolRent

## Despliegue Automático Completo (Recomendado)

Este script despliega TODA la aplicación automáticamente, incluyendo:
- ✅ Construcción de imágenes Docker
- ✅ Inicio de todos los servicios (MySQL, Keycloak, 3 Backends, Nginx, Frontend)
- ✅ Importación automática de datos de ejemplo
- ✅ Verificación de salud de todos los servicios

### Windows (PowerShell)

```powershell
.\deploy-complete.ps1
```

### Linux/Mac (Bash)

```bash
./deploy-complete.sh
```

## ⏱️ Tiempo Estimado

- **Primera ejecución** (construye imágenes): ~5-8 minutos
- **Ejecuciones posteriores** (usa imágenes existentes): ~2-3 minutos

## 🎯 Resultado

Una vez completado, tendrás acceso a:

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8090
- **Keycloak Admin**: http://localhost:9090 (admin/admin)

Con datos de ejemplo ya importados:
- 9 Clientes
- 19 Herramientas
- 18 Préstamos
- 24 Movimientos de Kardex

## ⚙️ Opciones Avanzadas

### Omitir construcción de imágenes (usa imágenes existentes)

**PowerShell:**
```powershell
.\deploy-complete.ps1 -SkipBuild
```

**Bash:**
```bash
./deploy-complete.sh --skip-build
```

### Omitir importación de datos (base de datos vacía)

**PowerShell:**
```powershell
.\deploy-complete.ps1 -SkipData
```

**Bash:**
```bash
./deploy-complete.sh --skip-data
```

### Combinar opciones

**PowerShell:**
```powershell
.\deploy-complete.ps1 -SkipBuild -SkipData
```

**Bash:**
```bash
./deploy-complete.sh --skip-build --skip-data
```

## 🔧 Solución de Problemas

### Error: "Docker no está instalado"
Instala Docker Desktop desde https://www.docker.com/products/docker-desktop

### Error: "No se puede cargar el archivo... la ejecución de scripts está deshabilitada"
Ejecuta en PowerShell como Administrador:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Error: "Puerto ya en uso"
Detén servicios que usen los puertos 3307, 5173, 8090, 9090:
```powershell
# Ver qué está usando un puerto
netstat -ano | findstr :8090

# Detener contenedores antiguos
docker-compose down
docker stop $(docker ps -aq)
```

### Los servicios no inician correctamente
Verifica los logs:
```powershell
docker logs toolrent-mysql
docker logs toolrent-keycloak
docker logs toolrent-backend-1
```

## 🛑 Detener Todo

```powershell
docker-compose down
```

## 🔄 Reiniciar desde Cero

```powershell
# Detener y eliminar todo (incluyendo volúmenes)
docker-compose down -v

# Volver a desplegar
.\deploy-complete.ps1
```

## 📊 Monitoreo Continuo

Una vez desplegado, usa el script de monitoreo:

```powershell
.\monitor-deployment.ps1
```

Esto mostrará el estado de todos los servicios en tiempo real y te avisará cuando todos estén operativos.

## 📝 Importar Solo los Datos (sin redesplegar)

Si ya tienes los servicios corriendo y solo quieres importar datos:

```powershell
docker exec -i toolrent-mysql mysql -uroot -proot123 toolrent < seed-data.sql
```

## 🎓 Para Evaluación

Este script es ideal para demostrar el proyecto al profesor:
1. Ejecuta `.\deploy-complete.ps1`
2. Espera 2-3 minutos
3. Abre http://localhost:5173
4. ¡Listo! Toda la aplicación funcionando con datos de ejemplo

Todas las funcionalidades del enunciado y rúbrica están implementadas y listas para probar.
