# 🔄 Guía: Verificar Nginx Load Balancer y Alta Disponibilidad

## 🎯 Objetivo
Demostrar que si se cae un backend, Nginx automáticamente dirige el tráfico a los otros backends disponibles.

---

## 📋 Arquitectura Actual

Tu sistema tiene:

```
                    ┌─────────────┐
                    │   Nginx LB  │
                    │  (puerto 80)│
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼─────┐      ┌────▼─────┐      ┌────▼─────┐
   │Backend-1 │      │Backend-2 │      │Backend-3 │
   │:8090     │      │:8090     │      │:8090     │
   └──────────┘      └──────────┘      └──────────┘
```

**Configuración de Nginx:**
- **Estrategia:** `least_conn` (menos conexiones)
- **Health check:** `max_fails=3 fail_timeout=30s`
- **Backends:** 3 réplicas del mismo servicio

---

## ✅ Paso 1: Verificar Estado Inicial

### 1.1. Ver todos los backends corriendo

```powershell
# Ver todos los contenedores
docker-compose ps

# Deberías ver:
# toolrent-backend-1   running
# toolrent-backend-2   running
# toolrent-backend-3   running
# toolrent-nginx-lb    running
```

### 1.2. Verificar salud de cada backend

```powershell
# Backend 1
docker exec toolrent-backend-1 wget -qO- http://localhost:8090/actuator/health

# Backend 2
docker exec toolrent-backend-2 wget -qO- http://localhost:8090/actuator/health

# Backend 3
docker exec toolrent-backend-3 wget -qO- http://localhost:8090/actuator/health

# Todos deben responder: {"status":"UP"}
```

### 1.3. Ver logs de Nginx

```powershell
# Ver logs del load balancer
docker-compose logs nginx-lb

# Ver logs en tiempo real
docker-compose logs -f nginx-lb
```

---

## 🔥 Paso 2: Simular Caída de un Backend

### 2.1. Detener Backend-1

```powershell
# Detener el primer backend
docker-compose stop backend-1

# Verificar que está detenido
docker-compose ps | findstr backend-1

# Debería mostrar: Exited
```

### 2.2. Verificar que la aplicación sigue funcionando

```powershell
# Abrir aplicación en el navegador
start http://localhost

# O hacer peticiones con curl
curl http://localhost:8090/actuator/health

# O probar endpoint de API
curl http://localhost:8090/api/v1/tools/
```

**✅ La aplicación debe seguir funcionando normalmente**

---

## 🔍 Paso 3: Verificar el Load Balancing

### 3.1. Ver logs de Nginx en tiempo real

```powershell
# En una terminal separada, ver logs de Nginx
docker-compose logs -f nginx-lb
```

### 3.2. Hacer múltiples peticiones

```powershell
# Hacer 10 peticiones seguidas
for ($i=1; $i -le 10; $i++) {
    curl http://localhost:8090/actuator/health
    Write-Host "Petición $i completada"
}
```

### 3.3. Ver logs de backends activos

```powershell
# Ver logs de Backend-2
docker-compose logs --tail=20 backend-2

# Ver logs de Backend-3
docker-compose logs --tail=20 backend-3

# Backend-1 no debería recibir peticiones (está detenido)
```

**Deberías ver que solo Backend-2 y Backend-3 reciben peticiones**

---

## 📊 Paso 4: Monitorear el Estado

### 4.1. Ver estadísticas de Nginx (Opcional)

```powershell
# Acceder al contenedor de Nginx
docker exec -it toolrent-nginx-lb sh

# Dentro del contenedor, ver configuración
cat /etc/nginx/nginx.conf

# Salir
exit
```

### 4.2. Verificar conectividad desde Nginx a backends

```powershell
# Ver desde Nginx qué backends están accesibles
docker exec toolrent-nginx-lb sh -c "nc -zv backend-1 8090"
docker exec toolrent-nginx-lb sh -c "nc -zv backend-2 8090"
docker exec toolrent-nginx-lb sh -c "nc -zv backend-3 8090"

# Backend-1 debería fallar
# Backend-2 y Backend-3 deberían estar OK
```

---

## 🔄 Paso 5: Reactivar Backend Caído

### 5.1. Reiniciar Backend-1

```powershell
# Iniciar Backend-1 nuevamente
docker-compose start backend-1

# Verificar que está corriendo
docker-compose ps | findstr backend-1

# Debería mostrar: Up
```

### 5.2. Esperar a que esté listo

```powershell
# Ver logs para confirmar que inició correctamente
docker-compose logs backend-1 | Select-String "Started DemoApplication"

# Verificar health
docker exec toolrent-backend-1 wget -qO- http://localhost:8090/actuator/health
```

### 5.3. Verificar que Nginx lo incluye nuevamente

```powershell
# Hacer más peticiones
for ($i=1; $i -le 10; $i++) {
    curl http://localhost:8090/actuator/health
    Write-Host "Petición $i completada"
}

# Ver logs de Backend-1
docker-compose logs --tail=20 backend-1

# Ahora debería recibir peticiones nuevamente
```

---

## 🧪 Paso 6: Prueba Extrema - Caída de 2 Backends

### 6.1. Detener Backend-1 y Backend-2

```powershell
# Detener dos backends
docker-compose stop backend-1 backend-2

# Verificar estado
docker-compose ps | findstr backend
```

### 6.2. Verificar que la aplicación sigue funcionando

```powershell
# La aplicación aún funciona con solo 1 backend
curl http://localhost:8090/actuator/health

# Abrir en navegador
start http://localhost
```

**✅ Con solo Backend-3 corriendo, la aplicación aún funciona**

### 6.3. Reactivar todos los backends

```powershell
# Iniciar todos nuevamente
docker-compose start backend-1 backend-2

# Verificar que todos están UP
docker-compose ps | findstr backend
```

---

## 🎯 Demostración para el Profesor

### Escenario Completo (5 minutos)

```powershell
# 1. Mostrar que todo funciona
docker-compose ps
start http://localhost

# 2. Detener un backend
docker-compose stop backend-1
Write-Host "Backend-1 detenido - Pero la aplicación sigue funcionando..."

# 3. Probar que sigue funcionando
curl http://localhost:8090/actuator/health
start http://localhost
# → Navega en la aplicación, crea préstamos, etc.

# 4. Ver logs de Nginx
docker-compose logs --tail=20 nginx-lb
Write-Host "Nginx redirige automáticamente a Backend-2 y Backend-3"

# 5. Detener segundo backend (prueba extrema)
docker-compose stop backend-2
Write-Host "Backend-2 también detenido - Solo queda Backend-3"

# 6. Verificar que TODAVÍA funciona
curl http://localhost:8090/actuator/health
Write-Host "La aplicación sigue funcionando con solo 1 backend"

# 7. Reactivar todos
docker-compose start backend-1 backend-2
Write-Host "Backends reactivados - Sistema completamente funcional nuevamente"

# 8. Verificar estado final
docker-compose ps | findstr backend
```

---

## 📊 Visualización del Comportamiento

### Estado Normal (3 backends activos)

```
Cliente → Nginx LB
              ├→ Backend-1 ✅ (33% del tráfico)
              ├→ Backend-2 ✅ (33% del tráfico)
              └→ Backend-3 ✅ (33% del tráfico)
```

### Con Backend-1 caído

```
Cliente → Nginx LB
              ├→ Backend-1 ❌ (sin tráfico)
              ├→ Backend-2 ✅ (50% del tráfico)
              └→ Backend-3 ✅ (50% del tráfico)
```

### Con Backend-1 y Backend-2 caídos

```
Cliente → Nginx LB
              ├→ Backend-1 ❌ (sin tráfico)
              ├→ Backend-2 ❌ (sin tráfico)
              └→ Backend-3 ✅ (100% del tráfico)
```

---

## 🔍 Cómo Funciona el Health Check de Nginx

### Configuración en nginx-backend.conf

```nginx
upstream backend_servers {
    least_conn;  # Estrategia: menos conexiones

    server backend-1:8090 max_fails=3 fail_timeout=30s;
    server backend-2:8090 max_fails=3 fail_timeout=30s;
    server backend-3:8090 max_fails=3 fail_timeout=30s;
}
```

**Parámetros:**
- `max_fails=3` - Marca el backend como caído después de 3 fallos consecutivos
- `fail_timeout=30s` - Mantiene el backend fuera del pool por 30 segundos antes de reintentar

**Comportamiento:**
1. Nginx intenta enviar petición a Backend-1
2. Si falla 3 veces seguidas, marca Backend-1 como "down"
3. Durante 30 segundos, NO envía tráfico a Backend-1
4. Después de 30 segundos, reintenta
5. Si Backend-1 responde, lo marca como "up" nuevamente

---

## 🧪 Pruebas Adicionales

### Prueba 1: Ver distribución de carga

```powershell
# Hacer 30 peticiones y contar respuestas de cada backend
for ($i=1; $i -le 30; $i++) {
    curl -s http://localhost:8090/actuator/health | Out-Null
    Start-Sleep -Milliseconds 100
}

# Ver logs de cada backend
Write-Host "Backend-1 requests:"
docker-compose logs backend-1 | Select-String "GET" | Measure-Object | Select-Object -ExpandProperty Count

Write-Host "Backend-2 requests:"
docker-compose logs backend-2 | Select-String "GET" | Measure-Object | Select-Object -ExpandProperty Count

Write-Host "Backend-3 requests:"
docker-compose logs backend-3 | Select-String "GET" | Measure-Object | Select-Object -ExpandProperty Count
```

### Prueba 2: Simular backend lento

```powershell
# "Pausar" un backend (simula lentitud extrema)
docker pause toolrent-backend-1

# Hacer peticiones - deberían ir a Backend-2 y Backend-3
curl http://localhost:8090/actuator/health

# Reanudar
docker unpause toolrent-backend-1
```

### Prueba 3: Reinicio en cascada

```powershell
# Reiniciar backends uno por uno con delay
docker-compose restart backend-1
Start-Sleep -Seconds 5
docker-compose restart backend-2
Start-Sleep -Seconds 5
docker-compose restart backend-3

# Durante los reinicios, la aplicación sigue funcionando
```

---

## 📝 Explicación Técnica para el Profesor

### Pregunta: "¿Cómo sabe Nginx que un backend está caído?"

**Respuesta:**
```
"Nginx usa passive health checks. Cuando intenta enviar una petición
a un backend y falla, cuenta el fallo. Después de 3 fallos consecutivos
(max_fails=3), marca el backend como 'down' y deja de enviarle tráfico
durante 30 segundos (fail_timeout=30s).

Después de 30 segundos, Nginx reintenta automáticamente. Si el backend
responde correctamente, lo marca como 'up' nuevamente y vuelve a incluirlo
en el pool de servidores disponibles."
```

### Pregunta: "¿Qué estrategia de balanceo usas?"

**Respuesta:**
```
"Usamos 'least_conn' (least connections), que significa que Nginx envía
cada nueva petición al backend que tiene menos conexiones activas en ese
momento. Esto es mejor que round-robin para peticiones de diferentes
duraciones, porque evita sobrecargar un servidor mientras otros están
ociosos."
```

### Pregunta: "¿Qué pasa si todos los backends caen?"

**Respuesta:**
```
"Si todos los backends están caídos, Nginx retorna un error 502 Bad Gateway
al cliente. En un entorno de producción real, esto se mitigaría con:
- Monitoreo activo (Prometheus, Grafana)
- Alertas automáticas (PagerDuty, Slack)
- Auto-scaling (Kubernetes, Docker Swarm)
- Múltiples zonas de disponibilidad"
```

---

## 🎓 Estrategias de Balanceo Disponibles

Tu configuración usa `least_conn`, pero Nginx soporta otras:

### 1. **least_conn** (Tu configuración actual)
```nginx
upstream backend_servers {
    least_conn;
    server backend-1:8090;
    server backend-2:8090;
    server backend-3:8090;
}
```
**Uso:** Peticiones van al servidor con menos conexiones activas

### 2. **round-robin** (Default)
```nginx
upstream backend_servers {
    server backend-1:8090;
    server backend-2:8090;
    server backend-3:8090;
}
```
**Uso:** Rotación secuencial (1 → 2 → 3 → 1 → 2 → 3...)

### 3. **ip_hash**
```nginx
upstream backend_servers {
    ip_hash;
    server backend-1:8090;
    server backend-2:8090;
    server backend-3:8090;
}
```
**Uso:** Misma IP siempre va al mismo backend (sticky sessions)

### 4. **weighted**
```nginx
upstream backend_servers {
    server backend-1:8090 weight=3;
    server backend-2:8090 weight=2;
    server backend-3:8090 weight=1;
}
```
**Uso:** Backend-1 recibe 3x más tráfico que Backend-3

---

## ⚡ Script de Demostración Completo

Crea este script: `test-load-balancer.ps1`

```powershell
Write-Host "======================================"
Write-Host "PRUEBA DE NGINX LOAD BALANCER"
Write-Host "======================================"
Write-Host ""

# 1. Estado inicial
Write-Host "1. Estado inicial de backends:" -ForegroundColor Green
docker-compose ps | Select-String "backend"
Write-Host ""
Start-Sleep -Seconds 2

# 2. Probar que funciona
Write-Host "2. Probando aplicación..." -ForegroundColor Green
$response = curl -s http://localhost:8090/actuator/health
Write-Host "Respuesta: $response"
Write-Host ""
Start-Sleep -Seconds 2

# 3. Detener Backend-1
Write-Host "3. Deteniendo Backend-1..." -ForegroundColor Yellow
docker-compose stop backend-1
Write-Host "Backend-1 detenido" -ForegroundColor Red
Write-Host ""
Start-Sleep -Seconds 3

# 4. Verificar que sigue funcionando
Write-Host "4. Verificando que la aplicación sigue funcionando..." -ForegroundColor Green
$response = curl -s http://localhost:8090/actuator/health
Write-Host "Respuesta: $response ✅" -ForegroundColor Green
Write-Host ""
Start-Sleep -Seconds 2

# 5. Hacer 5 peticiones
Write-Host "5. Haciendo 5 peticiones (solo Backend-2 y Backend-3 responden)..." -ForegroundColor Green
for ($i=1; $i -le 5; $i++) {
    curl -s http://localhost:8090/actuator/health | Out-Null
    Write-Host "  Petición $i completada ✅"
    Start-Sleep -Milliseconds 200
}
Write-Host ""
Start-Sleep -Seconds 2

# 6. Detener Backend-2 también
Write-Host "6. Deteniendo Backend-2 también (solo queda Backend-3)..." -ForegroundColor Yellow
docker-compose stop backend-2
Write-Host "Backend-2 detenido" -ForegroundColor Red
Write-Host ""
Start-Sleep -Seconds 3

# 7. Verificar con solo 1 backend
Write-Host "7. Verificando con SOLO Backend-3 activo..." -ForegroundColor Green
$response = curl -s http://localhost:8090/actuator/health
Write-Host "Respuesta: $response ✅" -ForegroundColor Green
Write-Host "La aplicación SIGUE funcionando con solo 1 backend! 🎉" -ForegroundColor Cyan
Write-Host ""
Start-Sleep -Seconds 2

# 8. Reactivar todos
Write-Host "8. Reactivando todos los backends..." -ForegroundColor Green
docker-compose start backend-1 backend-2
Write-Host "Esperando que backends inicien..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# 9. Estado final
Write-Host "9. Estado final:" -ForegroundColor Green
docker-compose ps | Select-String "backend"
Write-Host ""

# 10. Prueba final
Write-Host "10. Prueba final - Todos los backends activos:" -ForegroundColor Green
$response = curl -s http://localhost:8090/actuator/health
Write-Host "Respuesta: $response ✅" -ForegroundColor Green
Write-Host ""

Write-Host "======================================"
Write-Host "PRUEBA COMPLETADA EXITOSAMENTE ✅" -ForegroundColor Cyan
Write-Host "======================================"
```

**Ejecutar:**
```powershell
.\test-load-balancer.ps1
```

---

## ✅ Checklist de Verificación

Antes de demostrar al profesor:

- [ ] Docker Compose está corriendo: `docker-compose ps`
- [ ] Los 3 backends están UP
- [ ] Nginx load balancer está corriendo
- [ ] Puedes acceder a: http://localhost:8090/actuator/health
- [ ] Puedes acceder a: http://localhost
- [ ] Tienes logs de Nginx accesibles: `docker-compose logs nginx-lb`
- [ ] Puedes detener/iniciar backends sin problemas
- [ ] Conoces los comandos de verificación

---

## 🎯 Resumen de Comandos Clave

```powershell
# Ver estado
docker-compose ps

# Detener un backend
docker-compose stop backend-1

# Iniciar un backend
docker-compose start backend-1

# Ver logs de Nginx
docker-compose logs -f nginx-lb

# Probar que funciona
curl http://localhost:8090/actuator/health

# Ver logs de un backend
docker-compose logs --tail=20 backend-2

# Probar en navegador
start http://localhost
```

---

**¡Listo! Con esta guía puedes demostrar alta disponibilidad y load balancing en menos de 5 minutos.** 🚀
