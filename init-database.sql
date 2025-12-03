-- ============================================
-- INICIALIZACIÓN DE BASE DE DATOS - TOOLRENT
-- ============================================
-- Crea las bases de datos con UTF-8 correcto
-- ============================================

-- Crear base de datos toolrent con UTF-8
CREATE DATABASE IF NOT EXISTS toolrent
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Crear base de datos keycloak con UTF-8
CREATE DATABASE IF NOT EXISTS keycloak
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Verificar configuración
USE toolrent;
SELECT @@character_set_database AS toolrent_charset, @@collation_database AS toolrent_collation;

USE keycloak;
SELECT @@character_set_database AS keycloak_charset, @@collation_database AS keycloak_collation;

-- Mensaje de confirmación
SELECT 'Bases de datos toolrent y keycloak creadas con UTF-8 correctamente' AS resultado;

