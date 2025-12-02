# ============================================
# SCRIPT DE MONITOREO DE DESPLIEGUE
# ============================================
# Monitorea el estado de todos los servicios hasta que esten completamente operativos

param(
    [int]$MaxWaitMinutes = 10
)

Write-Host "================================================" -ForegroundColor Blue
Write-Host "MONITOREANDO DESPLIEGUE DE TOOLRENT" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Blue
Write-Host ""

$startTime = Get-Date
$endTime = $startTime.AddMinutes($MaxWaitMinutes)

function Get-ServiceStatus {
    $services = @(
        @{Name="mysql"; Expected="healthy"}
        @{Name="keycloak"; Expected="healthy"}
        @{Name="backend-1"; Expected="healthy"}
        @{Name="backend-2"; Expected="healthy"}
        @{Name="backend-3"; Expected="healthy"}
        @{Name="nginx-backend"; Expected="Up"}
        @{Name="frontend"; Expected="Up"}
    )

    $allHealthy = $true
    $status = @()

    foreach ($service in $services) {
        $containerName = "toolrent-$($service.Name)"
        $containerStatus = docker inspect --format='{{.State.Status}}' $containerName 2>$null
        $containerHealth = docker inspect --format='{{.State.Health.Status}}' $containerName 2>$null

        if ($containerHealth -eq "healthy" -or ($containerStatus -eq "running" -and $service.Expected -eq "Up")) {
            $status += "  [OK] $($service.Name.PadRight(15)) - $containerStatus $(if ($containerHealth) { "($containerHealth)" })"
        } elseif ($containerStatus -eq "running") {
            $status += "  [..] $($service.Name.PadRight(15)) - $containerStatus $(if ($containerHealth) { "($containerHealth)" })"
            $allHealthy = $false
        } else {
            $status += "  [XX] $($service.Name.PadRight(15)) - $containerStatus"
            $allHealthy = $false
        }
    }

    return @{
        AllHealthy = $allHealthy
        Status = $status
    }
}

Write-Host "Esperando que todos los servicios esten operativos..." -ForegroundColor Yellow
Write-Host "Tiempo maximo de espera: $MaxWaitMinutes minutos" -ForegroundColor Yellow
Write-Host ""

$iteration = 0
while ((Get-Date) -lt $endTime) {
    $iteration++
    $result = Get-ServiceStatus

    Clear-Host
    Write-Host "================================================" -ForegroundColor Blue
    Write-Host "MONITOREANDO DESPLIEGUE - Iteracion $iteration" -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Blue
    Write-Host ""
    Write-Host "Estado de servicios:" -ForegroundColor Cyan
    Write-Host ""

    $result.Status | ForEach-Object {
        if ($_ -match "\[OK\]") {
            Write-Host $_ -ForegroundColor Green
        } elseif ($_ -match "\[\.\.\]") {
            Write-Host $_ -ForegroundColor Yellow
        } else {
            Write-Host $_ -ForegroundColor Red
        }
    }

    Write-Host ""

    if ($result.AllHealthy) {
        Write-Host "================================================" -ForegroundColor Blue
        Write-Host "TODOS LOS SERVICIOS ESTAN OPERATIVOS" -ForegroundColor Green
        Write-Host "================================================" -ForegroundColor Blue
        Write-Host ""
        Write-Host "Puedes acceder a la aplicacion en:" -ForegroundColor Yellow
        Write-Host "  - Frontend:  http://localhost" -ForegroundColor Green
        Write-Host "  - Backend:   http://localhost:8090/actuator/health" -ForegroundColor Green
        Write-Host "  - Keycloak:  http://localhost:9090" -ForegroundColor Green
        Write-Host ""
        Write-Host "Presiona cualquier tecla para salir..." -ForegroundColor Gray
        $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        exit 0
    }

    Write-Host "Esperando 15 segundos antes de verificar nuevamente..." -ForegroundColor Gray
    Write-Host "(Presiona Ctrl+C para cancelar)" -ForegroundColor Gray
    Start-Sleep -Seconds 15
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Red
Write-Host "TIMEOUT: Los servicios no iniciaron en $MaxWaitMinutes minutos" -ForegroundColor Red
Write-Host "================================================" -ForegroundColor Red
Write-Host ""
Write-Host "Ejecuta los siguientes comandos para diagnosticar:" -ForegroundColor Yellow
Write-Host "  .\deploy.ps1 logs keycloak" -ForegroundColor Cyan
Write-Host "  .\deploy.ps1 logs backend-1" -ForegroundColor Cyan
Write-Host "  docker ps -a" -ForegroundColor Cyan
Write-Host ""
exit 1
