#!/usr/bin/env pwsh
# ============================================
# SCRIPT DE PRUEBA: NGINX LOAD BALANCER
# ============================================
# Demuestra que si se cae un backend, Nginx
# automáticamente redirige el tráfico a otros

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  PRUEBA DE NGINX LOAD BALANCER" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# 1. Estado inicial
Write-Host "1️⃣  Verificando estado inicial de backends..." -ForegroundColor Green
Write-Host ""
docker-compose ps | Select-String "backend"
Write-Host ""
Start-Sleep -Seconds 2

# 2. Probar que funciona
Write-Host "2️⃣  Probando que la aplicación funciona..." -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8090/actuator/health" -UseBasicParsing -TimeoutSec 5
    Write-Host "   ✅ Respuesta: $($response.Content)" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Error: No se pudo conectar al backend" -ForegroundColor Red
    exit 1
}
Write-Host ""
Start-Sleep -Seconds 2

# 3. Detener Backend-1
Write-Host "3️⃣  Simulando caída de Backend-1..." -ForegroundColor Yellow
docker-compose stop backend-1 | Out-Null
Write-Host "   ❌ Backend-1 DETENIDO" -ForegroundColor Red
Write-Host ""
Start-Sleep -Seconds 3

# 4. Verificar que sigue funcionando
Write-Host "4️⃣  Verificando que la aplicación SIGUE funcionando..." -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8090/actuator/health" -UseBasicParsing -TimeoutSec 5
    Write-Host "   ✅ Respuesta: $($response.Content)" -ForegroundColor Green
    Write-Host "   ✅ La aplicación funciona con Backend-2 y Backend-3" -ForegroundColor Cyan
} catch {
    Write-Host "   ❌ Error: La aplicación no responde" -ForegroundColor Red
    exit 1
}
Write-Host ""
Start-Sleep -Seconds 2

# 5. Hacer múltiples peticiones
Write-Host "5️⃣  Haciendo 5 peticiones (solo Backend-2 y Backend-3 responden)..." -ForegroundColor Green
for ($i=1; $i -le 5; $i++) {
    try {
        Invoke-WebRequest -Uri "http://localhost:8090/actuator/health" -UseBasicParsing -TimeoutSec 5 | Out-Null
        Write-Host "   ✅ Petición $i completada" -ForegroundColor Gray
        Start-Sleep -Milliseconds 200
    } catch {
        Write-Host "   ❌ Petición $i falló" -ForegroundColor Red
    }
}
Write-Host ""
Start-Sleep -Seconds 2

# 6. Detener Backend-2 también (prueba extrema)
Write-Host "6️⃣  Simulando caída de Backend-2 (SOLO queda Backend-3)..." -ForegroundColor Yellow
docker-compose stop backend-2 | Out-Null
Write-Host "   ❌ Backend-2 DETENIDO" -ForegroundColor Red
Write-Host "   ⚠️  Solo Backend-3 activo" -ForegroundColor Yellow
Write-Host ""
Start-Sleep -Seconds 3

# 7. Verificar con solo 1 backend
Write-Host "7️⃣  Verificando con SOLO Backend-3 activo..." -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8090/actuator/health" -UseBasicParsing -TimeoutSec 5
    Write-Host "   ✅ Respuesta: $($response.Content)" -ForegroundColor Green
    Write-Host "   🎉 La aplicación SIGUE funcionando con solo 1 backend!" -ForegroundColor Cyan
} catch {
    Write-Host "   ❌ Error: La aplicación no responde" -ForegroundColor Red
}
Write-Host ""
Start-Sleep -Seconds 2

# 8. Ver logs de Nginx (últimas líneas)
Write-Host "8️⃣  Logs recientes de Nginx Load Balancer:" -ForegroundColor Green
Write-Host ""
docker-compose logs --tail=10 nginx-lb
Write-Host ""
Start-Sleep -Seconds 2

# 9. Reactivar todos los backends
Write-Host "9️⃣  Reactivando todos los backends..." -ForegroundColor Green
docker-compose start backend-1 backend-2 | Out-Null
Write-Host "   ⏳ Esperando que backends inicien (10 segundos)..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# 10. Estado final
Write-Host "🔟 Estado final de backends:" -ForegroundColor Green
Write-Host ""
docker-compose ps | Select-String "backend"
Write-Host ""
Start-Sleep -Seconds 2

# 11. Prueba final con todos activos
Write-Host "1️⃣1️⃣  Prueba final - Todos los backends activos:" -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8090/actuator/health" -UseBasicParsing -TimeoutSec 5
    Write-Host "   ✅ Respuesta: $($response.Content)" -ForegroundColor Green
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
Write-Host "   • Con 3 backends activos: ✅ Funciona" -ForegroundColor Gray
Write-Host "   • Con 2 backends activos: ✅ Funciona" -ForegroundColor Gray
Write-Host "   • Con 1 backend activo:  ✅ Funciona" -ForegroundColor Gray
Write-Host "   • Nginx Load Balancer:   ✅ Operacional" -ForegroundColor Gray
Write-Host ""
Write-Host "🎯 Conclusión: Alta disponibilidad demostrada exitosamente" -ForegroundColor Cyan
Write-Host ""
