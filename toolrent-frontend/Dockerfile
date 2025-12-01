# ============================================
# DOCKERFILE PARA FRONTEND TOOLRENT
# ============================================
# Multi-stage build para optimizar tamaño de imagen

# ====== STAGE 1: BUILD ======
FROM node:18-alpine AS build

LABEL maintainer="ToolRent Team"
LABEL description="Frontend ToolRent - Build Stage"

WORKDIR /app

# Copiar archivos de dependencias
COPY package.json package-lock.json* ./

# Instalar dependencias
RUN npm ci --silent

# Copiar código fuente
COPY . .

# Build de la aplicación
RUN npm run build

# ====== STAGE 2: RUNTIME ======
FROM nginx:1.25-alpine

LABEL maintainer="ToolRent Team"
LABEL description="Frontend ToolRent - Runtime"

# Copiar build de React al directorio de Nginx
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar configuración personalizada de Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Exponer puerto 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

# Nginx se ejecuta en foreground por defecto
CMD ["nginx", "-g", "daemon off;"]
