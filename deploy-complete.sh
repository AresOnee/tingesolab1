#!/bin/bash

# ============================================
# SCRIPT DE DESPLIEGUE COMPLETO - TOOLRENT
# ============================================
# Despliega toda la aplicación e importa datos de ejemplo
#
# Uso: ./deploy-complete.sh

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Variables
SKIP_BUILD=false
SKIP_DATA=false

# Procesar argumentos
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --skip-data)
            SKIP_DATA=true
            shift
            ;;
        *)
            echo "Argumento desconocido: $1"
            exit 1
            ;;
    esac
done

# Funciones para mensajes
print_step() {
    echo -e "${CYAN}[PASO] $1${NC}"
}

print_success() {
    echo -e "${GREEN}[OK] $1${NC}"
}

print_error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

print_info() {
    echo -e "${YELLOW}[INFO] $1${NC}"
}

echo -e "${BLUE}================================================${NC}"
echo -e "${GREEN}DESPLIEGUE COMPLETO DE TOOLRENT${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# ============================================
# PASO 1: Verificar requisitos
# ============================================
print_step "Verificando requisitos previos..."

# Verificar Docker
if ! command -v docker &> /dev/null; then
    print_error "Docker no está instalado"
    exit 1
fi
print_success "Docker instalado"

# Verificar Docker Compose
if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose no está instalado"
    exit 1
fi
print_success "Docker Compose instalado"

# Verificar archivos necesarios
if [ ! -f "docker-compose.yml" ]; then
    print_error "No se encuentra docker-compose.yml"
    exit 1
fi

if [ ! -f "seed-data.sql" ]; then
    print_error "No se encuentra seed-data.sql"
    exit 1
fi

print_success "Todos los archivos necesarios están presentes"
echo ""

# ============================================
# PASO 2: Limpiar contenedores antiguos
# ============================================
print_step "Limpiando contenedores antiguos..."

docker-compose down 2>/dev/null || true
print_success "Contenedores antiguos eliminados"
echo ""

# ============================================
# PASO 3: Construir imágenes (opcional)
# ============================================
if [ "$SKIP_BUILD" = false ]; then
    print_step "Construyendo imágenes Docker..."
    print_info "Esto puede tomar varios minutos la primera vez..."

    # Backend
    print_info "Construyendo backend..."
    cd backend-toolrent
    docker build -t aresone/toolrent-backend:latest . > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        print_error "Fallo al construir imagen del backend"
        cd ..
        exit 1
    fi
    cd ..
    print_success "Backend construido"

    # Frontend
    print_info "Construyendo frontend..."
    cd toolrent-frontend
    docker build -t aresone/toolrent-frontend:latest . > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        print_error "Fallo al construir imagen del frontend"
        cd ..
        exit 1
    fi
    cd ..
    print_success "Frontend construido"

    echo ""
else
    print_info "Omitiendo construcción de imágenes (se usarán las existentes)"
    echo ""
fi

# ============================================
# PASO 4: Iniciar servicios
# ============================================
print_step "Iniciando servicios con Docker Compose..."

docker-compose up -d
if [ $? -ne 0 ]; then
    print_error "Fallo al iniciar servicios"
    exit 1
fi

print_success "Servicios iniciados"
echo ""

# ============================================
# PASO 5: Esperar a que MySQL esté listo
# ============================================
print_step "Esperando a que MySQL esté listo..."

max_attempts=30
attempt=0
mysql_ready=false

while [ $attempt -lt $max_attempts ] && [ "$mysql_ready" = false ]; do
    attempt=$((attempt + 1))
    echo -n "  Intento $attempt/$max_attempts..."

    if docker exec toolrent-mysql mysqladmin ping -h localhost -u root -proot123 2>&1 | grep -q "mysqld is alive"; then
        mysql_ready=true
        echo -e " ${GREEN}OK${NC}"
    else
        echo -e " ${YELLOW}esperando...${NC}"
        sleep 2
    fi
done

if [ "$mysql_ready" = false ]; then
    print_error "MySQL no inició correctamente después de $max_attempts intentos"
    print_info "Verifica los logs: docker logs toolrent-mysql"
    exit 1
fi

print_success "MySQL está operativo"
echo ""

# ============================================
# PASO 6: Esperar a que Keycloak esté listo
# ============================================
print_step "Esperando a que Keycloak esté listo..."

max_attempts=60
attempt=0
keycloak_ready=false

while [ $attempt -lt $max_attempts ] && [ "$keycloak_ready" = false ]; do
    attempt=$((attempt + 1))
    echo -n "  Intento $attempt/$max_attempts..."

    health=$(docker inspect toolrent-keycloak --format='{{.State.Health.Status}}' 2>/dev/null || echo "unknown")
    if [ "$health" = "healthy" ]; then
        keycloak_ready=true
        echo -e " ${GREEN}OK${NC}"
    else
        echo -e " ${YELLOW}esperando... (estado: $health)${NC}"
        sleep 3
    fi
done

if [ "$keycloak_ready" = false ]; then
    print_error "Keycloak no inició correctamente después de $max_attempts intentos"
    print_info "Verifica los logs: docker logs toolrent-keycloak"
    exit 1
fi

print_success "Keycloak está operativo y realm importado"
echo ""

# ============================================
# PASO 7: Esperar a que backends estén listos y creen las tablas
# ============================================
print_step "Esperando a que backends estén listos y creen las tablas..."

sleep 15

backends=("toolrent-backend-1" "toolrent-backend-2" "toolrent-backend-3")
for backend in "${backends[@]}"; do
    max_attempts=20
    attempt=0
    backend_ready=false

    while [ $attempt -lt $max_attempts ] && [ "$backend_ready" = false ]; do
        attempt=$((attempt + 1))
        echo -n "  $backend - Intento $attempt/$max_attempts..."

        health=$(docker inspect $backend --format='{{.State.Health.Status}}' 2>/dev/null || echo "unknown")
        if [ "$health" = "healthy" ]; then
            backend_ready=true
            echo -e " ${GREEN}OK${NC}"
        else
            echo -e " ${YELLOW}esperando...${NC}"
            sleep 3
        fi
    done

    if [ "$backend_ready" = false ]; then
        print_error "$backend no inició correctamente"
    fi
done

echo ""

# ============================================
# PASO 8: Importar datos de ejemplo (después de que las tablas existan)
# ============================================
if [ "$SKIP_DATA" = false ]; then
    print_step "Importando datos de ejemplo desde seed-data.sql..."

    cat seed-data.sql | docker exec -i toolrent-mysql mysql -uroot -proot123 --default-character-set=utf8mb4 toolrent

    if [ $? -ne 0 ]; then
        print_error "Fallo al importar datos de ejemplo"
        print_info "Puedes intentar manualmente: docker exec -i toolrent-mysql mysql -uroot -proot123 --default-character-set=utf8mb4 toolrent < seed-data.sql"
        exit 1
    fi

    print_success "Datos de ejemplo importados correctamente"
    echo ""
else
    print_info "Omitiendo importación de datos de ejemplo"
    echo ""
fi

# ============================================
# PASO 9: Verificar estado final
# ============================================
print_step "Verificando estado de todos los servicios..."
echo ""

docker-compose ps

echo ""
echo -e "${BLUE}================================================${NC}"
echo -e "${GREEN}DESPLIEGUE COMPLETADO EXITOSAMENTE${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""
echo -e "${YELLOW}Puedes acceder a la aplicación en:${NC}"
echo -e "  - Frontend:       ${GREEN}http://localhost:5173${NC}"
echo -e "  - Backend API:    ${GREEN}http://localhost:8090/actuator/health${NC}"
echo -e "  - Keycloak Admin: ${GREEN}http://localhost:9090 (admin/admin)${NC}"
echo ""
echo -e "${YELLOW}Datos de ejemplo importados:${NC}"
echo "  - 9 Clientes (7 Activos + 2 Restringidos)"
echo "  - 19 Herramientas en 7 categorías"
echo "  - 18 Préstamos (activos, atrasados, cerrados)"
echo "  - 24 Movimientos de Kardex"
echo "  - 5 Configuraciones del sistema"
echo ""
echo -e "${YELLOW}Comandos útiles:${NC}"
echo -e "  Ver logs:           ${CYAN}docker-compose logs -f [servicio]${NC}"
echo -e "  Detener todo:       ${CYAN}docker-compose down${NC}"
echo -e "  Reiniciar servicio: ${CYAN}docker-compose restart [servicio]${NC}"
echo -e "  Estado servicios:   ${CYAN}docker-compose ps${NC}"
echo ""
