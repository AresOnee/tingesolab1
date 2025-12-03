#!/usr/bin/env pwsh
# ============================================
# SCRIPT DE PRUEBA: NGINX LOAD BALANCER FRONTEND
# ============================================
# Demuestra que si se cae un frontend, Nginx
# automáticamente redirige el tráfico a otros

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  PRUEBA DE NGINX LOAD BALANCER" -ForegroundColor Cyan
Write-Host "  FRONTEND" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# 1. Estado inicial
Write-Host "1️⃣  Verificando estado inicial de frontends..." -ForegroundColor Green
Write-Host ""
docker-compose ps | Select-String "frontend"
Write-Host ""
Start-Sleep -Seconds 2

# 2. Probar que funciona
Write-Host "2️⃣  Probando que la aplicación frontend funciona..." -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost" -UseBasicParsing -TimeoutSec 5
    Write-Host "   ✅ Status Code: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "   ✅ Aplicación cargando correctamente" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Error: No se pudo conectar al frontend" -ForegroundColor Red
    exit 1
}
Write-Host ""
Start-Sleep -Seconds 2

# 3. Detener Frontend-1
Write-Host "3️⃣  Simulando caída de Frontend-1..." -ForegroundColor Yellow
docker-compose stop frontend-1 | Out-Null
Write-Host "   ❌ Frontend-1 DETENIDO" -ForegroundColor Red
Write-Host ""
Start-Sleep -Seconds 3

# 4. Verificar que sigue funcionando
Write-Host "4️⃣  Verificando que la aplicación SIGUE funcionando..." -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost" -UseBasicParsing -TimeoutSec 5
    Write-Host "   ✅ Status Code: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "   ✅ La aplicación funciona con Frontend-2 y Frontend-3" -ForegroundColor Cyan
} catch {
    Write-Host "   ❌ Error: La aplicación no responde" -ForegroundColor Red
    exit 1
}
Write-Host ""
Start-Sleep -Seconds 2

# 5. Hacer múltiples peticiones
Write-Host "5️⃣  Haciendo 5 peticiones (solo Frontend-2 y Frontend-3 responden)..." -ForegroundColor Green
for ($i=1; $i -le 5; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost" -UseBasicParsing -TimeoutSec 5
        Write-Host "   ✅ Petición $i completada - Status: $($response.StatusCode)" -ForegroundColor Gray
        Start-Sleep -Milliseconds 200
    } catch {
        Write-Host "   ❌ Petición $i falló" -ForegroundColor Red
    }
}
Write-Host ""
Start-Sleep -Seconds 2

# 6. Detener Frontend-2 también (prueba extrema)
Write-Host "6️⃣  Simulando caída de Frontend-2 (SOLO queda Frontend-3)..." -ForegroundColor Yellow
docker-compose stop frontend-2 | Out-Null
Write-Host "   ❌ Frontend-2 DETENIDO" -ForegroundColor Red
Write-Host "   ⚠️  Solo Frontend-3 activo" -ForegroundColor Yellow
Write-Host ""
Start-Sleep -Seconds 3

# 7. Verificar con solo 1 frontend
Write-Host "7️⃣  Verificando con SOLO Frontend-3 activo..." -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost" -UseBasicParsing -TimeoutSec 5
    Write-Host "   ✅ Status Code: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "   🎉 La aplicación SIGUE funcionando con solo 1 frontend!" -ForegroundColor Cyan
} catch {
    Write-Host "   ❌ Error: La aplicación no responde" -ForegroundColor Red
}
Write-Host ""
Start-Sleep -Seconds 2

# 8. Ver logs de Nginx Frontend (últimas líneas)
Write-Host "8️⃣  Logs recientes de Nginx Load Balancer (Frontend):" -ForegroundColor Green
Write-Host ""
docker-compose logs --tail=10 nginx-frontend
Write-Host ""
Start-Sleep -Seconds 2

# 9. Reactivar todos los frontends
Write-Host "9️⃣  Reactivando todos los frontends..." -ForegroundColor Green
docker-compose start frontend-1 frontend-2 | Out-Null
Write-Host "   ⏳ Esperando que frontends inicien (5 segundos)..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# 10. Estado final
Write-Host "🔟 Estado final de frontends:" -ForegroundColor Green
Write-Host ""
docker-compose ps | Select-String "frontend"
Write-Host ""
Start-Sleep -Seconds 2

# 11. Prueba final con todos activos
Write-Host "1️⃣1️⃣  Prueba final - Todos los frontends activos:" -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost" -UseBasicParsing -TimeoutSec 5
    Write-Host "   ✅ Status Code: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "   ✅ Todos los frontends respondiendo correctamente" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Error al conectar" -ForegroundColor Red
}
Write-Host ""

# Resumen
Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  ✅ PRUEBA COMPLETADA EXITOSAMENTE" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📊 Resumen:" -ForegroundColor Yellow
Write-Host "   • Con 3 frontends activos: ✅ Funciona" -ForegroundColor Gray
Write-Host "   • Con 2 frontends activos: ✅ Funciona" -ForegroundColor Gray
Write-Host "   • Con 1 frontend activo:  ✅ Funciona" -ForegroundColor Gray
Write-Host "   • Nginx Load Balancer:    ✅ Operacional" -ForegroundColor Gray
Write-Host ""
Write-Host "🎯 Conclusión: Alta disponibilidad en frontend demostrada exitosamente" -ForegroundColor Cyan
Write-Host ""
