# ============================================
# SCRIPT DE DESPLIEGUE COMPLETO - TOOLRENT
# ============================================
# Despliega toda la aplicación e importa datos de ejemplo
#
# Uso: .\deploy-complete.ps1

param(
    [switch]$SkipBuild,
    [switch]$SkipData
)

Write-Host "================================================" -ForegroundColor Blue
Write-Host "DESPLIEGUE COMPLETO DE TOOLRENT" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Blue
Write-Host ""

# Función para mostrar mensajes
function Write-Step {
    param([string]$Message)
    Write-Host "[PASO] $Message" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Yellow
}

# ============================================
# PASO 1: Verificar requisitos
# ============================================
Write-Step "Verificando requisitos previos..."

# Verificar Docker
if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker no está instalado o no está en el PATH"
    exit 1
}
Write-Success "Docker instalado"

# Verificar Docker Compose
if (!(Get-Command docker-compose -ErrorAction SilentlyContinue)) {
    Write-Error "Docker Compose no está instalado"
    exit 1
}
Write-Success "Docker Compose instalado"

# Verificar archivos necesarios
if (!(Test-Path "docker-compose.yml")) {
    Write-Error "No se encuentra docker-compose.yml"
    exit 1
}

if (!(Test-Path "seed-data.sql")) {
    Write-Error "No se encuentra seed-data.sql"
    exit 1
}

Write-Success "Todos los archivos necesarios están presentes"
Write-Host ""

# ============================================
# PASO 2: Limpiar contenedores antiguos
# ============================================
Write-Step "Limpiando contenedores antiguos..."

docker-compose down 2>$null
Write-Success "Contenedores antiguos eliminados"
Write-Host ""

# ============================================
# PASO 3: Construir imágenes (con UTF-8 configurado)
# ============================================
if (!$SkipBuild) {
    Write-Step "Construyendo imágenes Docker con configuración UTF-8..."
    Write-Info "Esto puede tomar varios minutos la primera vez..."

    # Usar docker compose build para reconstruir con los cambios de UTF-8
    Write-Info "Reconstruyendo backend y frontend con UTF-8..."
    docker compose build --no-cache backend-1 backend-2 backend-3 frontend

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Fallo al construir imágenes"
        exit 1
    }

    Write-Success "Imágenes construidas con configuración UTF-8"
    Write-Host ""
} else {
    Write-Info "Omitiendo construcción de imágenes (se usarán las existentes)"
    Write-Host ""
}

# ============================================
# PASO 4: Iniciar servicios
# ============================================
Write-Step "Iniciando servicios con Docker Compose..."

docker-compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Error "Fallo al iniciar servicios"
    exit 1
}

Write-Success "Servicios iniciados"
Write-Host ""

# ============================================
# PASO 5: Esperar a que MySQL esté listo
# ============================================
Write-Step "Esperando a que MySQL esté listo..."

$maxAttempts = 30
$attempt = 0
$mysqlReady = $false

while ($attempt -lt $maxAttempts -and !$mysqlReady) {
    $attempt++
    Write-Host "  Intento $attempt/$maxAttempts..." -NoNewline

    $result = docker exec toolrent-mysql mysqladmin ping -h localhost -u root -proot123 2>&1
    if ($result -match "mysqld is alive") {
        $mysqlReady = $true
        Write-Host " OK" -ForegroundColor Green
    } else {
        Write-Host " esperando..." -ForegroundColor Yellow
        Start-Sleep -Seconds 2
    }
}

if (!$mysqlReady) {
    Write-Error "MySQL no inició correctamente después de $maxAttempts intentos"
    Write-Info "Verifica los logs: docker logs toolrent-mysql"
    exit 1
}

Write-Success "MySQL está operativo"
Write-Host ""

# ============================================
# PASO 6: Esperar a que Keycloak esté listo
# ============================================
Write-Step "Esperando a que Keycloak esté listo..."

$maxAttempts = 60
$attempt = 0
$keycloakReady = $false

while ($attempt -lt $maxAttempts -and !$keycloakReady) {
    $attempt++
    Write-Host "  Intento $attempt/$maxAttempts..." -NoNewline

    $health = docker inspect toolrent-keycloak --format='{{.State.Health.Status}}' 2>$null
    if ($health -eq "healthy") {
        $keycloakReady = $true
        Write-Host " OK" -ForegroundColor Green
    } else {
        Write-Host " esperando... (estado: $health)" -ForegroundColor Yellow
        Start-Sleep -Seconds 3
    }
}

if (!$keycloakReady) {
    Write-Error "Keycloak no inició correctamente después de $maxAttempts intentos"
    Write-Info "Verifica los logs: docker logs toolrent-keycloak"
    exit 1
}

Write-Success "Keycloak está operativo y realm importado"
Write-Host ""

# ============================================
# PASO 7: Esperar a que backends estén listos y creen las tablas
# ============================================
Write-Step "Esperando a que backends estén listos y creen las tablas..."

Start-Sleep -Seconds 15

$backends = @("toolrent-backend-1", "toolrent-backend-2", "toolrent-backend-3")
foreach ($backend in $backends) {
    $maxAttempts = 20
    $attempt = 0
    $backendReady = $false

    while ($attempt -lt $maxAttempts -and !$backendReady) {
        $attempt++
        Write-Host "  $backend - Intento $attempt/$maxAttempts..." -NoNewline

        $health = docker inspect $backend --format='{{.State.Health.Status}}' 2>$null
        if ($health -eq "healthy") {
            $backendReady = $true
            Write-Host " OK" -ForegroundColor Green
        } else {
            Write-Host " esperando..." -ForegroundColor Yellow
            Start-Sleep -Seconds 3
        }
    }

    if (!$backendReady) {
        Write-Error "$backend no inició correctamente"
    }
}

Write-Host ""

# ============================================
# PASO 8: Importar datos de ejemplo (después de que las tablas existan)
# ============================================
if (!$SkipData) {
    Write-Step "Importando datos de ejemplo desde seed-data.sql..."

    Get-Content seed-data.sql -Encoding UTF8 | docker exec -i toolrent-mysql mysql -uroot -proot123 --default-character-set=utf8mb4 toolrent

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Fallo al importar datos de ejemplo"
        Write-Info "Puedes intentar manualmente: Get-Content seed-data.sql -Encoding UTF8 | docker exec -i toolrent-mysql mysql -uroot -proot123 --default-character-set=utf8mb4 toolrent"
        exit 1
    }

    Write-Success "Datos de ejemplo importados correctamente"
    Write-Host ""
} else {
    Write-Info "Omitiendo importación de datos de ejemplo"
    Write-Host ""
}

# ============================================
# PASO 9: Verificar estado final
# ============================================
Write-Step "Verificando estado de todos los servicios..."
Write-Host ""

docker-compose ps

Write-Host ""
Write-Host "================================================" -ForegroundColor Blue
Write-Host "DESPLIEGUE COMPLETADO EXITOSAMENTE" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Blue
Write-Host ""
Write-Host "Puedes acceder a la aplicación en:" -ForegroundColor Yellow
Write-Host "  - Frontend:       http://localhost:5173" -ForegroundColor Green
Write-Host "  - Backend API:    http://localhost:8090/actuator/health" -ForegroundColor Green
Write-Host "  - Keycloak Admin: http://localhost:9090 (admin/admin)" -ForegroundColor Green
Write-Host ""
Write-Host "Datos de ejemplo importados:" -ForegroundColor Yellow
Write-Host "  - 9 Clientes (7 Activos + 2 Restringidos)" -ForegroundColor White
Write-Host "  - 19 Herramientas en 7 categorías" -ForegroundColor White
Write-Host "  - 18 Préstamos (activos, atrasados, cerrados)" -ForegroundColor White
Write-Host "  - 24 Movimientos de Kardex" -ForegroundColor White
Write-Host "  - 5 Configuraciones del sistema" -ForegroundColor White
Write-Host ""
Write-Host "Comandos útiles:" -ForegroundColor Yellow
Write-Host "  Ver logs:           docker-compose logs -f [servicio]" -ForegroundColor Cyan
Write-Host "  Detener todo:       docker-compose down" -ForegroundColor Cyan
Write-Host "  Reiniciar servicio: docker-compose restart [servicio]" -ForegroundColor Cyan
Write-Host "  Estado servicios:   docker-compose ps" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para monitoreo continuo ejecuta:" -ForegroundColor Yellow
Write-Host "  .\monitor-deployment.ps1" -ForegroundColor Cyan
Write-Host ""
