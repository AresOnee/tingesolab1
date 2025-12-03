# 🔄 Guía: Cambiar Título del Frontend y Reflejarlo en Producción

## 📋 Escenario
El profesor te pide: **"Cambia el título del frontend a 'Sistema ToolRent' y que se vea reflejado en la aplicación"**

---

## 🎯 Archivos a Modificar

Hay **3 ubicaciones** donde aparece el título en el frontend:

### 1. **Título de la Pestaña del Navegador**
📁 `toolrent-frontend/index.html` - Línea 7
```html
<title>ToolRent</title>
```

### 2. **Título en el Navbar (Barra Superior)**
📁 `toolrent-frontend/src/components/Navbar.jsx` - Línea 22
```jsx
<Typography variant="h6" sx={{ flexGrow: 1 }}>
  Sistema de Gestión Remuneraciones
</Typography>
```

### 3. **Título en el Footer (Pie de Página)**
📁 `toolrent-frontend/src/layouts/MainLayout.jsx` - Línea 220
```jsx
ToolRent © {new Date().getFullYear()} - Sistema de Gestión de Préstamos
```

---

## 🚀 Pasos para Cambiar y Reflejar el Título

### **Paso 1: Modificar los Archivos (2 minutos)**

#### 1.1. Cambiar título de la pestaña
```bash
# Abrir el archivo
notepad toolrent-frontend\index.html

# O en VS Code
code toolrent-frontend\index.html
```

**Cambiar línea 7:**
```html
<!-- Antes -->
<title>ToolRent</title>

<!-- Después (ejemplo con lo que pida el profesor) -->
<title>Sistema ToolRent</title>
```

#### 1.2. Cambiar título del Navbar
```bash
notepad toolrent-frontend\src\components\Navbar.jsx
```

**Cambiar línea 22:**
```jsx
// Antes
<Typography variant="h6" sx={{ flexGrow: 1 }}>
  Sistema de Gestión Remuneraciones
</Typography>

// Después
<Typography variant="h6" sx={{ flexGrow: 1 }}>
  Sistema ToolRent
</Typography>
```

#### 1.3. (Opcional) Cambiar footer
```bash
notepad toolrent-frontend\src\layouts\MainLayout.jsx
```

**Cambiar línea 220:**
```jsx
// Antes
ToolRent © {new Date().getFullYear()} - Sistema de Gestión de Préstamos

// Después
Sistema ToolRent © {new Date().getFullYear()}
```

---

### **Paso 2: Verificar Localmente (Opcional - 3 minutos)**

Si tienes tiempo, puedes verificar que los cambios funcionan:

```bash
# Ir al directorio del frontend
cd toolrent-frontend

# Instalar dependencias (si no están instaladas)
npm install

# Ejecutar en modo desarrollo
npm run dev
```

Abre **http://localhost:5173** y verifica que:
- ✅ Título de la pestaña cambió
- ✅ Título del Navbar cambió
- ✅ Footer cambió (si lo modificaste)

**Detener el servidor:** `Ctrl + C`

---

### **Paso 3: Commit y Push a GitHub (1 minuto)**

```bash
# Volver al directorio raíz
cd ..

# Ver los cambios realizados
git status

# Agregar los archivos modificados
git add toolrent-frontend/index.html
git add toolrent-frontend/src/components/Navbar.jsx
git add toolrent-frontend/src/layouts/MainLayout.jsx

# Hacer commit
git commit -m "feat: Update frontend title to 'Sistema ToolRent'"

# Push a GitHub
git push origin claude/fix-accent-characters-01RTXKDWowNDXFAoRFhVhXsw
```

**Nota:** Si estás en otra rama, cambia el nombre de la rama en el comando push.

---

### **Paso 4: Ejecutar Pipeline en Jenkins (5-7 minutos)**

#### 4.1. Abrir Jenkins
```
http://localhost:8081
```

#### 4.2. Ir al Pipeline
- Click en **"ToolRent-Pipeline"**

#### 4.3. Ejecutar Build
- Click en **"Build Now"**

#### 4.4. Monitorear la Ejecución
- Click en el número del build (ej: **#8**)
- Click en **"Console Output"**
- Espera a ver: **"Finished: SUCCESS"**

**El pipeline hará:**
1. ✅ Descargar el código actualizado
2. ✅ Ejecutar tests
3. ✅ **Construir nueva imagen Docker del frontend**
4. ✅ **Subir nueva imagen a Docker Hub**
5. ✅ Limpiar imágenes locales

**Tiempo:** ~5-7 minutos

---

### **Paso 5: Re-desplegar con la Nueva Imagen (2 minutos)**

Una vez que el pipeline termine exitosamente, necesitas desplegar la nueva versión:

```powershell
# Opción 1: Despliegue completo (reinicia todo)
.\deploy-complete.ps1

# Opción 2: Solo actualizar el frontend (más rápido)
# Detener el contenedor actual
docker-compose stop frontend

# Eliminar el contenedor
docker-compose rm -f frontend

# Descargar la nueva imagen
docker pull fergusone/toolrent-frontend:latest

# Iniciar el contenedor con la nueva imagen
docker-compose up -d frontend
```

**Recomendación:** Usa la **Opción 2** en la evaluación para ser más rápido.

---

### **Paso 6: Verificar el Cambio (30 segundos)**

#### 6.1. Abrir el navegador
```
http://localhost
```

#### 6.2. Verificar que el título cambió
- ✅ Pestaña del navegador muestra: **"Sistema ToolRent"**
- ✅ Navbar (barra superior) muestra: **"Sistema ToolRent"**
- ✅ Footer muestra el nuevo texto (si lo cambiaste)

#### 6.3. (Opcional) Forzar recarga
Si no ves los cambios, presiona:
- **Ctrl + F5** (Windows)
- **Cmd + Shift + R** (Mac)

Esto fuerza al navegador a descargar los archivos nuevamente sin usar caché.

---

## ⚡ Flujo Rápido (Sin Verificación Local)

Si el profesor quiere verlo rápido, sigue este flujo de **~8 minutos**:

```powershell
# 1. Cambiar archivos (2 min)
notepad toolrent-frontend\index.html
notepad toolrent-frontend\src\components\Navbar.jsx

# 2. Commit y Push (1 min)
git add toolrent-frontend/
git commit -m "feat: Update frontend title to 'Sistema ToolRent'"
git push

# 3. Ejecutar Jenkins Build (5-7 min)
# Ir a http://localhost:8081
# ToolRent-Pipeline → Build Now

# 4. Mientras Jenkins corre, preparar el despliegue
# Cuando Jenkins termine:

# 5. Actualizar solo el frontend (1 min)
docker-compose stop frontend
docker-compose rm -f frontend
docker pull fergusone/toolrent-frontend:latest
docker-compose up -d frontend

# 6. Verificar en el navegador
# http://localhost
# Ctrl + F5 para recargar
```

**Tiempo total:** ~8-10 minutos

---

## 🎯 Explicación Técnica (Para el Profesor)

Durante la demostración, puedes explicar:

### 1. **Arquitectura del Cambio**
```
Código Fuente (GitHub)
     ↓
Pipeline Jenkins
     ↓
Build Docker Image (frontend)
     ↓
Push a Docker Hub
     ↓
Pull nueva imagen
     ↓
Despliegue con Docker Compose
     ↓
Usuario ve el cambio
```

### 2. **Por qué es necesario reconstruir la imagen?**

**Respuesta:**
El frontend de React es una **aplicación estática** que se compila y se sirve desde Nginx. Los cambios en el código fuente (JSX, HTML) necesitan:

1. **Compilación:** Vite compila el código React a JavaScript optimizado
2. **Build de imagen:** Los archivos compilados se copian a una imagen Docker
3. **Deployment:** La nueva imagen reemplaza a la anterior

No es como el backend donde un cambio de código puede reflejarse con un simple reinicio (en modo desarrollo).

### 3. **Alternativa sin Pipeline (Para desarrollo rápido)**

Si quisieras ver el cambio SIN pasar por Jenkins (solo para desarrollo):

```powershell
# Reconstruir solo la imagen del frontend localmente
docker-compose build frontend

# Reiniciar el contenedor
docker-compose up -d frontend
```

Esto toma ~2 minutos, pero **NO sube la imagen a Docker Hub**.

---

## 📊 Comparación de Métodos

| Método | Tiempo | Sube a Docker Hub | Ejecuta Tests | Para Evaluación |
|--------|--------|-------------------|---------------|-----------------|
| **Pipeline Jenkins** | 8-10 min | ✅ Sí | ✅ Sí | ✅ **Recomendado** |
| **Build Local** | 2 min | ❌ No | ❌ No | ⚠️ Solo desarrollo |
| **Solo Verificar Local** | 1 min | ❌ No | ❌ No | ❌ No refleja en producción |

**Para la evaluación:** Usa el **Pipeline Jenkins** para demostrar el flujo completo de CI/CD.

---

## 🐛 Troubleshooting

### Problema 1: No veo el cambio en el navegador

**Causa:** Caché del navegador

**Solución:**
```
Ctrl + F5 (forzar recarga)
```

### Problema 2: Jenkins falló en el build

**Solución:**
1. Ver **Console Output** en Jenkins
2. Verificar que el código compila localmente: `npm run build`
3. Corregir errores y hacer nuevo push

### Problema 3: La imagen no se descarga

**Solución:**
```powershell
# Eliminar imagen local
docker rmi fergusone/toolrent-frontend:latest

# Forzar pull
docker pull fergusone/toolrent-frontend:latest

# Verificar
docker images | findstr toolrent-frontend
```

### Problema 4: El contenedor no inicia

**Solución:**
```powershell
# Ver logs
docker-compose logs frontend

# Reiniciar todo el stack
docker-compose restart
```

---

## ✅ Checklist de Verificación

Antes de decirle al profesor "está listo", verifica:

- [ ] Archivos modificados y guardados
- [ ] Commit realizado
- [ ] Push a GitHub exitoso
- [ ] Jenkins pipeline ejecutado: **SUCCESS**
- [ ] Nueva imagen en Docker Hub
- [ ] Contenedor frontend actualizado
- [ ] Título visible en el navegador (pestaña)
- [ ] Título visible en el Navbar
- [ ] Sin errores en la consola del navegador (F12)

---

## 💡 Tips para la Evaluación

### 1. **Mientras Jenkins corre**
Aprovecha para explicar al profesor:
- Qué hace cada etapa del pipeline
- Por qué la cobertura es 92.25%
- Cómo funciona el build multi-stage de Docker

### 2. **Muestra el cambio en múltiples lugares**
- Pestaña del navegador
- Navbar cuando inicias sesión
- Código fuente en VS Code
- Commit en GitHub
- Nueva imagen en Docker Hub

### 3. **Explica el flujo DevOps**
```
Desarrollo → Git → Jenkins → Docker Build → Docker Hub → Deployment
```

Esto demuestra que entiendes el ciclo completo de CI/CD.

---

## 🎯 Resumen de Comandos

```powershell
# === CAMBIO DE TÍTULO ===

# 1. Modificar archivos
notepad toolrent-frontend\index.html
notepad toolrent-frontend\src\components\Navbar.jsx

# 2. Commit y Push
git add toolrent-frontend/
git commit -m "feat: Update frontend title to 'Sistema ToolRent'"
git push

# 3. Jenkins: http://localhost:8081 → Build Now

# 4. Actualizar deployment
docker-compose stop frontend
docker-compose rm -f frontend
docker pull fergusone/toolrent-frontend:latest
docker-compose up -d frontend

# 5. Verificar: http://localhost (Ctrl + F5)
```

---

## 📚 Archivos de Referencia

- `toolrent-frontend/index.html` - Título de pestaña
- `toolrent-frontend/src/components/Navbar.jsx` - Título del navbar
- `toolrent-frontend/src/layouts/MainLayout.jsx` - Footer
- `Jenkinsfile` - Pipeline de CI/CD
- `docker-compose.yml` - Configuración de deployment

---

**¡Listo! Con esta guía puedes cambiar el título del frontend en menos de 10 minutos durante la evaluación.** 🚀
