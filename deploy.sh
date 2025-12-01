#!/bin/bash

# ============================================
# SCRIPT DE DESPLIEGUE RÁPIDO - TOOLRENT
# ============================================
# Este script automatiza el proceso de build y despliegue

set -e  # Salir en caso de error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para imprimir con color
print_step() {
    echo -e "${BLUE}================================================${NC}"
    echo -e "${GREEN}$1${NC}"
    echo -e "${BLUE}================================================${NC}"
}

print_error() {
    echo -e "${RED}❌ ERROR: $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  WARNING: $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Verificar que Docker esté corriendo
if ! docker info > /dev/null 2>&1; then
    print_error "Docker no está corriendo. Por favor inicia Docker Desktop."
    exit 1
fi

# Verificar que DOCKER_USERNAME esté configurado
if [ -z "$DOCKER_USERNAME" ]; then
    print_error "Variable DOCKER_USERNAME no está configurada."
    echo "Por favor ejecuta: export DOCKER_USERNAME=tu-usuario"
    exit 1
fi

print_step "INICIANDO DESPLIEGUE TOOLRENT"
echo "Docker Username: $DOCKER_USERNAME"

# OPCIÓN 1: BUILD LOCAL Y DESPLIEGUE
if [ "$1" == "build" ]; then
    print_step "OPCIÓN 1: BUILD LOCAL DE IMÁGENES"

    # Build Backend
    print_step "Construyendo imagen Backend..."
    cd backend-toolrent
    docker build -t $DOCKER_USERNAME/toolrent-backend:latest .
    print_success "Imagen backend construida"
    cd ..

    # Build Frontend
    print_step "Construyendo imagen Frontend..."
    cd toolrent-frontend
    docker build -t $DOCKER_USERNAME/toolrent-frontend:latest .
    print_success "Imagen frontend construida"
    cd ..

    # Push a Docker Hub (opcional)
    if [ "$2" == "push" ]; then
        print_step "Subiendo imágenes a Docker Hub..."
        docker push $DOCKER_USERNAME/toolrent-backend:latest
        docker push $DOCKER_USERNAME/toolrent-frontend:latest
        print_success "Imágenes subidas a Docker Hub"
    fi

    # Desplegar
    print_step "Desplegando con Docker Compose..."
    docker-compose up -d

    print_success "Despliegue completado (build local)"

# OPCIÓN 2: PULL DESDE DOCKER HUB Y DESPLIEGUE
elif [ "$1" == "pull" ]; then
    print_step "OPCIÓN 2: PULL DESDE DOCKER HUB"

    docker-compose pull
    docker-compose up -d

    print_success "Despliegue completado (desde Docker Hub)"

# OPCIÓN 3: DETENER SERVICIOS
elif [ "$1" == "down" ]; then
    print_step "DETENIENDO SERVICIOS"

    docker-compose down

    print_success "Servicios detenidos"

# OPCIÓN 4: DETENER Y LIMPIAR TODO
elif [ "$1" == "clean" ]; then
    print_step "LIMPIEZA COMPLETA"

    print_warning "Esto eliminará todos los contenedores, volúmenes e imágenes"
    read -p "¿Estás seguro? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose down -v
        docker rmi $DOCKER_USERNAME/toolrent-backend:latest || true
        docker rmi $DOCKER_USERNAME/toolrent-frontend:latest || true
        print_success "Limpieza completada"
    else
        print_warning "Limpieza cancelada"
    fi

# OPCIÓN 5: VER LOGS
elif [ "$1" == "logs" ]; then
    if [ -z "$2" ]; then
        docker-compose logs -f
    else
        docker-compose logs -f $2
    fi

# OPCIÓN 6: VER STATUS
elif [ "$1" == "status" ]; then
    print_step "ESTADO DE SERVICIOS"
    docker-compose ps
    echo ""
    print_step "HEALTH CHECKS"
    echo "Backend: $(curl -s http://localhost:8090/actuator/health 2>/dev/null || echo 'No disponible')"
    echo "Frontend: $(curl -s http://localhost/health 2>/dev/null || echo 'No disponible')"

# OPCIÓN 7: RESTART
elif [ "$1" == "restart" ]; then
    print_step "REINICIANDO SERVICIOS"

    if [ -z "$2" ]; then
        docker-compose restart
        print_success "Todos los servicios reiniciados"
    else
        docker-compose restart $2
        print_success "Servicio $2 reiniciado"
    fi

# OPCIÓN POR DEFECTO: MOSTRAR AYUDA
else
    echo -e "${BLUE}============================================${NC}"
    echo -e "${GREEN}   TOOLRENT - SCRIPT DE DESPLIEGUE${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
    echo "Uso: ./deploy.sh [comando] [opciones]"
    echo ""
    echo "Comandos disponibles:"
    echo ""
    echo "  ${GREEN}build${NC} [push]     - Construir imágenes localmente"
    echo "                    Si se agrega 'push', sube a Docker Hub"
    echo "                    Ejemplo: ./deploy.sh build push"
    echo ""
    echo "  ${GREEN}pull${NC}             - Pull desde Docker Hub y desplegar"
    echo "                    (requiere imágenes en Docker Hub)"
    echo ""
    echo "  ${GREEN}down${NC}             - Detener todos los servicios"
    echo ""
    echo "  ${GREEN}clean${NC}            - Detener servicios y limpiar todo"
    echo "                    (elimina contenedores, volúmenes e imágenes)"
    echo ""
    echo "  ${GREEN}logs${NC} [servicio] - Ver logs de servicios"
    echo "                    Ejemplo: ./deploy.sh logs backend-1"
    echo ""
    echo "  ${GREEN}status${NC}           - Ver estado de servicios"
    echo ""
    echo "  ${GREEN}restart${NC} [serv]   - Reiniciar servicios"
    echo "                    Ejemplo: ./deploy.sh restart backend-1"
    echo ""
    echo "Prerequisitos:"
    echo "  export DOCKER_USERNAME=tu-usuario"
    echo ""
    echo "Servicios disponibles:"
    echo "  - mysql, keycloak"
    echo "  - backend-1, backend-2, backend-3"
    echo "  - nginx-backend, frontend"
    echo ""
    echo "Acceso a la aplicación después del despliegue:"
    echo "  - Frontend:  ${GREEN}http://localhost${NC}"
    echo "  - Backend:   ${GREEN}http://localhost:8090${NC}"
    echo "  - Keycloak:  ${GREEN}http://localhost:9090${NC}"
    echo ""
fi
