-- ============================================
-- INICIALIZACIÓN DE BASE DE DATOS - TOOLRENT
-- ============================================
-- Crea la base de datos con UTF-8 correcto
-- ============================================

-- Eliminar base de datos si existe
DROP DATABASE IF EXISTS toolrent;

-- Crear base de datos con UTF-8
CREATE DATABASE toolrent
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Usar la base de datos
USE toolrent;

-- Verificar configuración
SELECT @@character_set_database, @@collation_database;

-- Mensaje de confirmación
SELECT 'Base de datos toolrent creada con UTF-8 correctamente' AS resultado;
