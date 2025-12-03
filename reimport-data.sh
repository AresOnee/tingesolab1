#!/bin/bash
# ============================================
# SCRIPT PARA REIMPORTAR DATOS CON UTF-8
# ============================================
# Este script limpia y reimporta los datos de
# ejemplo con la codificación UTF-8 correcta
# ============================================

set -e

echo "========================================"
echo "🔄 REIMPORTANDO DATOS CON UTF-8"
echo "========================================"

# Esperar a que MySQL esté listo
echo "⏳ Esperando a que MySQL esté disponible..."
sleep 5

# Limpiar datos existentes y reimportar
echo "🗑️  Limpiando datos existentes..."
docker exec -i toolrent-mysql mysql -uroot -proot123 --default-character-set=utf8mb4 toolrent <<EOF
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE kardex;
TRUNCATE TABLE loans;
TRUNCATE TABLE clients;
TRUNCATE TABLE tools;
TRUNCATE TABLE system_config;
SET FOREIGN_KEY_CHECKS = 1;
EOF

echo "✅ Datos limpiados correctamente"

# Reimportar datos con UTF-8
echo "📥 Importando datos de ejemplo con UTF-8..."
cat seed-data.sql | docker exec -i toolrent-mysql mysql -uroot -proot123 --default-character-set=utf8mb4 toolrent

echo ""
echo "========================================"
echo "✅ DATOS REIMPORTADOS EXITOSAMENTE"
echo "========================================"
echo ""
echo "🔍 Verificando algunos datos:"
docker exec -i toolrent-mysql mysql -uroot -proot123 --default-character-set=utf8mb4 toolrent <<EOF
SELECT id, name FROM clients LIMIT 3;
EOF

echo ""
echo "✅ Listo! Los caracteres acentuados deberían verse correctamente ahora."
echo "   Recarga la página en tu navegador (Ctrl+F5)"
