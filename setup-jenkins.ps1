# ============================================
# SCRIPT DE CONFIGURACIÓN AUTOMÁTICA DE JENKINS
# ============================================
# Configura Jenkins con Docker para el proyecto ToolRent
#
# Uso: .\setup-jenkins.ps1

param(
    [string]$DockerHubUsername = "",
    [string]$DockerHubPassword = ""
)

# Colores para output
function Write-Step {
    param([string]$Message)
    Write-Host "================================================" -ForegroundColor Blue
    Write-Host $Message -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Yellow
}

Write-Step "CONFIGURACION AUTOMATICA DE JENKINS PARA TOOLRENT"

# ============================================
# PASO 1: Verificar Docker
# ============================================
Write-Step "Verificando Docker..."

try {
    docker --version | Out-Null
    Write-Success "Docker instalado y funcionando"
} catch {
    Write-Error-Custom "Docker no esta instalado o no esta corriendo"
    Write-Info "Por favor instala Docker Desktop desde: https://www.docker.com/products/docker-desktop"
    exit 1
}

# ============================================
# PASO 2: Detener Jenkins existente (si existe)
# ============================================
Write-Info "Verificando si existe Jenkins corriendo..."

$existingJenkins = docker ps -a --filter "name=jenkins" --format "{{.Names}}"
if ($existingJenkins -eq "jenkins") {
    Write-Info "Se encontro contenedor Jenkins existente. Deteniendo..."
    docker stop jenkins 2>$null
    docker rm jenkins 2>$null
    Write-Success "Contenedor anterior eliminado"
}

# ============================================
# PASO 3: Crear volumen para Jenkins
# ============================================
Write-Step "Creando volumen persistente para Jenkins..."

$existingVolume = docker volume ls --filter "name=jenkins_home" --format "{{.Name}}"
if ($existingVolume -ne "jenkins_home") {
    docker volume create jenkins_home
    Write-Success "Volumen jenkins_home creado"
} else {
    Write-Info "Volumen jenkins_home ya existe (se reutilizara)"
}

# ============================================
# PASO 4: Iniciar contenedor Jenkins
# ============================================
Write-Step "Iniciando Jenkins en Docker..."

Write-Info "Esto puede tomar 1-2 minutos en la primera ejecucion..."

docker run -d `
    --name jenkins `
    --restart unless-stopped `
    -p 8080:8080 `
    -p 50000:50000 `
    -v jenkins_home:/var/jenkins_home `
    -v //var/run/docker.sock:/var/run/docker.sock `
    jenkins/jenkins:lts

if ($LASTEXITCODE -ne 0) {
    Write-Error-Custom "Fallo al iniciar Jenkins"
    exit 1
}

Write-Success "Contenedor Jenkins iniciado"

# ============================================
# PASO 5: Esperar a que Jenkins este listo
# ============================================
Write-Step "Esperando a que Jenkins inicie completamente..."

$maxAttempts = 60
$attempt = 0
$jenkinsReady = $false

while ($attempt -lt $maxAttempts -and !$jenkinsReady) {
    $attempt++
    Write-Host "  Intento $attempt/$maxAttempts..." -NoNewline

    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080" -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 403) {
            $jenkinsReady = $true
            Write-Host " OK" -ForegroundColor Green
        } else {
            Write-Host " esperando..." -ForegroundColor Yellow
            Start-Sleep -Seconds 3
        }
    } catch {
        Write-Host " esperando..." -ForegroundColor Yellow
        Start-Sleep -Seconds 3
    }
}

if (!$jenkinsReady) {
    Write-Error-Custom "Jenkins no inicio correctamente despues de $maxAttempts intentos"
    Write-Info "Verifica los logs: docker logs jenkins"
    exit 1
}

Write-Success "Jenkins esta corriendo!"

# ============================================
# PASO 6: Obtener contraseña inicial
# ============================================
Write-Step "Obteniendo contraseña inicial de Jenkins..."

Start-Sleep -Seconds 5  # Esperar a que se genere el archivo de password

$initialPassword = docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword 2>$null

if ($initialPassword) {
    Write-Success "Contraseña inicial obtenida"
} else {
    Write-Info "No se pudo obtener la contraseña automaticamente"
    Write-Info "Ejecuta manualmente: docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword"
}

# ============================================
# PASO 7: Configurar permisos de Docker
# ============================================
Write-Step "Configurando permisos de Docker dentro del contenedor..."

docker exec -u root jenkins chmod 666 /var/run/docker.sock 2>$null
Write-Success "Permisos configurados"

# ============================================
# PASO 8: Solicitar credenciales Docker Hub (si no se proporcionaron)
# ============================================
if ($DockerHubUsername -eq "" -or $DockerHubPassword -eq "") {
    Write-Step "Configuracion de credenciales de Docker Hub"
    Write-Info "Necesitas proporcionar tus credenciales de Docker Hub"
    Write-Info "Estas seran usadas para configurar Jenkins"
    Write-Host ""

    if ($DockerHubUsername -eq "") {
        $DockerHubUsername = Read-Host "Ingresa tu usuario de Docker Hub"
    }

    if ($DockerHubPassword -eq "") {
        $DockerHubPasswordSecure = Read-Host "Ingresa tu password de Docker Hub" -AsSecureString
        $BSTR = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($DockerHubPasswordSecure)
        $DockerHubPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($BSTR)
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)
    }
}

# ============================================
# RESUMEN FINAL
# ============================================
Write-Host ""
Write-Host "================================================" -ForegroundColor Blue
Write-Host "JENKINS CONFIGURADO EXITOSAMENTE" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Blue
Write-Host ""
Write-Host "Accede a Jenkins en:" -ForegroundColor Yellow
Write-Host "  URL: http://localhost:8080" -ForegroundColor Green
Write-Host ""
Write-Host "Contraseña inicial de administrador:" -ForegroundColor Yellow
Write-Host "  $initialPassword" -ForegroundColor Cyan
Write-Host ""
Write-Host "Siguientes pasos:" -ForegroundColor Yellow
Write-Host "  1. Abre http://localhost:8080 en tu navegador" -ForegroundColor White
Write-Host "  2. Ingresa la contraseña inicial mostrada arriba" -ForegroundColor White
Write-Host "  3. Click en 'Install suggested plugins'" -ForegroundColor White
Write-Host "  4. Crea un usuario administrador" -ForegroundColor White
Write-Host "  5. Sigue la guia en JENKINS-SETUP.md para configurar el pipeline" -ForegroundColor White
Write-Host ""
Write-Host "Credenciales de Docker Hub a configurar en Jenkins:" -ForegroundColor Yellow
Write-Host "  Username: $DockerHubUsername" -ForegroundColor Cyan
Write-Host "  (Ir a Manage Jenkins > Manage Credentials para agregarlas)" -ForegroundColor White
Write-Host ""
Write-Host "Comandos utiles:" -ForegroundColor Yellow
Write-Host "  Ver logs:      docker logs -f jenkins" -ForegroundColor Cyan
Write-Host "  Detener:       docker stop jenkins" -ForegroundColor Cyan
Write-Host "  Iniciar:       docker start jenkins" -ForegroundColor Cyan
Write-Host "  Reiniciar:     docker restart jenkins" -ForegroundColor Cyan
Write-Host ""
Write-Host "Documentacion completa: JENKINS-SETUP.md" -ForegroundColor Yellow
Write-Host ""
