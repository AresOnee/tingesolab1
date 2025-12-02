# ============================================
# SCRIPT DE DESPLIEGUE RÁPIDO - TOOLRENT
# PowerShell Version for Windows
# ============================================

param(
    [Parameter(Position=0)]
    [string]$Command = "help",

    [Parameter(Position=1)]
    [string]$Option = ""
)

# Colores para output
function Write-Step {
    param([string]$Message)
    Write-Host "================================================" -ForegroundColor Blue
    Write-Host $Message -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Blue
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "❌ ERROR: $Message" -ForegroundColor Red
}

function Write-Warning-Custom {
    param([string]$Message)
    Write-Host "⚠️  WARNING: $Message" -ForegroundColor Yellow
}

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

# Verificar que Docker esté corriendo
try {
    docker info | Out-Null
} catch {
    Write-Error-Custom "Docker no está corriendo. Por favor inicia Docker Desktop."
    exit 1
}

# Verificar que DOCKER_USERNAME esté configurado
if (-not $env:DOCKER_USERNAME) {
    Write-Error-Custom "Variable DOCKER_USERNAME no está configurada."
    Write-Host "Por favor ejecuta en PowerShell:" -ForegroundColor Yellow
    Write-Host '  $env:DOCKER_USERNAME="tu-usuario"' -ForegroundColor Cyan
    Write-Host ""
    Write-Host "O para que persista entre sesiones:" -ForegroundColor Yellow
    Write-Host '  [System.Environment]::SetEnvironmentVariable("DOCKER_USERNAME", "tu-usuario", "User")' -ForegroundColor Cyan
    exit 1
}

Write-Step "INICIANDO DESPLIEGUE TOOLRENT"
Write-Host "Docker Username: $env:DOCKER_USERNAME"
Write-Host ""

# ====== OPCIÓN 1: BUILD LOCAL Y DESPLIEGUE ======
if ($Command -eq "build") {
    Write-Step "OPCIÓN 1: BUILD LOCAL DE IMÁGENES"

    # Build Backend
    Write-Step "Construyendo imagen Backend..."
    Set-Location backend-toolrent
    docker build -t "$env:DOCKER_USERNAME/toolrent-backend:latest" .
    if ($LASTEXITCODE -ne 0) {
        Write-Error-Custom "Error al construir imagen backend"
        Set-Location ..
        exit 1
    }
    Write-Success "Imagen backend construida"
    Set-Location ..

    # Build Frontend
    Write-Step "Construyendo imagen Frontend..."
    Set-Location toolrent-frontend
    docker build -t "$env:DOCKER_USERNAME/toolrent-frontend:latest" .
    if ($LASTEXITCODE -ne 0) {
        Write-Error-Custom "Error al construir imagen frontend"
        Set-Location ..
        exit 1
    }
    Write-Success "Imagen frontend construida"
    Set-Location ..

    # Push a Docker Hub (opcional)
    if ($Option -eq "push") {
        Write-Step "Subiendo imágenes a Docker Hub..."
        docker push "$env:DOCKER_USERNAME/toolrent-backend:latest"
        docker push "$env:DOCKER_USERNAME/toolrent-frontend:latest"
        Write-Success "Imágenes subidas a Docker Hub"
    }

    # Desplegar
    Write-Step "Desplegando con Docker Compose..."
    docker-compose up -d

    Write-Success "Despliegue completado (build local)"
}

# ====== OPCIÓN 2: PULL DESDE DOCKER HUB Y DESPLIEGUE ======
elseif ($Command -eq "pull") {
    Write-Step "OPCIÓN 2: PULL DESDE DOCKER HUB"

    docker-compose pull
    docker-compose up -d

    Write-Success "Despliegue completado (desde Docker Hub)"
}

# ====== OPCIÓN 3: DETENER SERVICIOS ======
elseif ($Command -eq "down") {
    Write-Step "DETENIENDO SERVICIOS"

    docker-compose down

    Write-Success "Servicios detenidos"
}

# ====== OPCIÓN 4: DETENER Y LIMPIAR TODO ======
elseif ($Command -eq "clean") {
    Write-Step "LIMPIEZA COMPLETA"

    Write-Warning-Custom "Esto eliminará todos los contenedores, volúmenes e imágenes"
    $confirmation = Read-Host "¿Estás seguro? (S/N)"

    if ($confirmation -eq 'S' -or $confirmation -eq 's') {
        docker-compose down -v
        docker rmi "$env:DOCKER_USERNAME/toolrent-backend:latest" 2>$null
        docker rmi "$env:DOCKER_USERNAME/toolrent-frontend:latest" 2>$null
        Write-Success "Limpieza completada"
    } else {
        Write-Warning-Custom "Limpieza cancelada"
    }
}

# ====== OPCIÓN 5: VER LOGS ======
elseif ($Command -eq "logs") {
    if ($Option -eq "") {
        docker-compose logs -f
    } else {
        docker-compose logs -f $Option
    }
}

# ====== OPCIÓN 6: VER STATUS ======
elseif ($Command -eq "status") {
    Write-Step "ESTADO DE SERVICIOS"
    docker-compose ps
    Write-Host ""
    Write-Step "HEALTH CHECKS"

    try {
        $backendHealth = Invoke-RestMethod -Uri "http://localhost:8090/actuator/health" -ErrorAction SilentlyContinue
        Write-Host "Backend: OK" -ForegroundColor Green
    } catch {
        Write-Host "Backend: No disponible" -ForegroundColor Red
    }

    try {
        $frontendHealth = Invoke-RestMethod -Uri "http://localhost/health" -ErrorAction SilentlyContinue
        Write-Host "Frontend: OK" -ForegroundColor Green
    } catch {
        Write-Host "Frontend: No disponible" -ForegroundColor Red
    }
}

# ====== OPCIÓN 7: RESTART ======
elseif ($Command -eq "restart") {
    Write-Step "REINICIANDO SERVICIOS"

    if ($Option -eq "") {
        docker-compose restart
        Write-Success "Todos los servicios reiniciados"
    } else {
        docker-compose restart $Option
        Write-Success "Servicio $Option reiniciado"
    }
}

# ====== OPCIÓN POR DEFECTO: MOSTRAR AYUDA ======
else {
    Write-Host "============================================" -ForegroundColor Blue
    Write-Host "   TOOLRENT - SCRIPT DE DESPLIEGUE" -ForegroundColor Green
    Write-Host "============================================" -ForegroundColor Blue
    Write-Host ""
    Write-Host "Uso: .\deploy.ps1 [comando] [opciones]" -ForegroundColor White
    Write-Host ""
    Write-Host "Comandos disponibles:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  build [push]     " -NoNewline -ForegroundColor Green
    Write-Host "- Construir imágenes localmente"
    Write-Host "                    Si se agrega 'push', sube a Docker Hub"
    Write-Host "                    Ejemplo: .\deploy.ps1 build push" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  pull             " -NoNewline -ForegroundColor Green
    Write-Host "- Pull desde Docker Hub y desplegar"
    Write-Host "                    (requiere imágenes en Docker Hub)"
    Write-Host ""
    Write-Host "  down             " -NoNewline -ForegroundColor Green
    Write-Host "- Detener todos los servicios"
    Write-Host ""
    Write-Host "  clean            " -NoNewline -ForegroundColor Green
    Write-Host "- Detener servicios y limpiar todo"
    Write-Host "                    (elimina contenedores, volúmenes e imágenes)"
    Write-Host ""
    Write-Host "  logs [servicio]  " -NoNewline -ForegroundColor Green
    Write-Host "- Ver logs de servicios"
    Write-Host "                    Ejemplo: .\deploy.ps1 logs backend-1" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  status           " -NoNewline -ForegroundColor Green
    Write-Host "- Ver estado de servicios"
    Write-Host ""
    Write-Host "  restart [serv]   " -NoNewline -ForegroundColor Green
    Write-Host "- Reiniciar servicios"
    Write-Host "                    Ejemplo: .\deploy.ps1 restart backend-1" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Prerequisitos:" -ForegroundColor Yellow
    Write-Host '  $env:DOCKER_USERNAME="tu-usuario"' -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Servicios disponibles:" -ForegroundColor Yellow
    Write-Host "  - mysql, keycloak"
    Write-Host "  - backend-1, backend-2, backend-3"
    Write-Host "  - nginx-backend, frontend"
    Write-Host ""
    Write-Host "Acceso a la aplicación después del despliegue:" -ForegroundColor Yellow
    Write-Host "  - Frontend:  " -NoNewline
    Write-Host "http://localhost" -ForegroundColor Green
    Write-Host "  - Backend:   " -NoNewline
    Write-Host "http://localhost:8090" -ForegroundColor Green
    Write-Host "  - Keycloak:  " -NoNewline
    Write-Host "http://localhost:9090" -ForegroundColor Green
    Write-Host ""
}
