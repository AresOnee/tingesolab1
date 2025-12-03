-- ============================================
-- SEED DATA PARA TOOLRENT
-- ============================================
-- Datos de ejemplo para demostrar todas las funcionalidades
-- del sistema según enunciado y rúbrica
-- ============================================

-- Configurar encoding UTF-8
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Limpiar datos existentes (orden importante por foreign keys)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE kardex;
TRUNCATE TABLE loans;
TRUNCATE TABLE clients;
TRUNCATE TABLE tools;
TRUNCATE TABLE system_config;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 1. CONFIGURACIÓN DEL SISTEMA (Épica 4: Gestión de Tarifas)
-- ============================================
-- ✅ Solo configuraciones que se usan en el código
INSERT INTO system_config (id, config_key, config_value, description, last_modified, modified_by) VALUES
(1, 'TARIFA_ARRIENDO_DIARIA', 5000.00, 'Tarifa base de arriendo por día (CLP)', NOW(), 'admin'),
(2, 'TARIFA_MULTA_DIARIA', 2000.00, 'Multa por día de atraso (CLP)', NOW(), 'admin'),
(3, 'CARGO_REPARACION', 10000.00, 'Cargo fijo por reparación de herramientas con daños leves', NOW(), 'admin');

-- ============================================
-- 2. CLIENTES (Épica 2: Gestión de Clientes)
-- ============================================
-- Clientes ACTIVOS (pueden arrendar)
INSERT INTO clients (id, name, rut, phone, email, state) VALUES
(1, 'María González Pérez', '16.789.234-5', '+56987654321', 'maria.gonzalez@email.cl', 'Activo'),
(2, 'Pedro Martínez López', '18.456.789-2', '+56912345678', 'pedro.martinez@empresa.cl', 'Activo'),
(3, 'Ana Silva Rojas', '20.123.456-7', '+56945678901', 'ana.silva@constructor.cl', 'Activo'),
(4, 'Carlos Fernández Muñoz', '19.876.543-K', '+56923456789', 'carlos.fernandez@taller.cl', 'Activo'),
(5, 'Isabel Torres Vargas', '17.234.567-8', '+56934567890', 'isabel.torres@arquitectura.cl', 'Activo'),
(6, 'Roberto Sánchez Díaz', '21.345.678-9', '+56956789012', 'roberto.sanchez@construcciones.cl', 'Activo'),
(7, 'Carmen Ramírez Flores', '15.987.654-3', '+56978901234', 'carmen.ramirez@hogar.cl', 'Activo');

-- Clientes RESTRINGIDOS (con préstamos atrasados/multas pendientes)
INSERT INTO clients (id, name, rut, phone, email, state) VALUES
(8, 'Diego Morales Castro', '19.234.567-1', '+56967890123', 'diego.morales@email.cl', 'Restringido'),
(9, 'Francisca Herrera Soto', '18.765.432-6', '+56989012345', 'francisca.herrera@empresa.cl', 'Restringido');

-- ============================================
-- 3. HERRAMIENTAS (Épica 1: Gestión de Herramientas)
-- ============================================
-- Categoría: TALADROS
INSERT INTO tools (id, name, category, status, replacement_value, stock) VALUES
(1, 'Taladro Percutor Bosch 850W', 'Taladros', 'Disponible', 89990, 5),
(2, 'Taladro Inalámbrico Dewalt 20V', 'Taladros', 'Disponible', 129990, 3),
(3, 'Taladro de Columna Industrial', 'Taladros', 'Prestada', 450000, 0),

-- Categoría: SIERRAS
(4, 'Sierra Circular Makita 7-1/4"', 'Sierras', 'Disponible', 109990, 4),
(5, 'Sierra Caladora Bosch 650W', 'Sierras', 'Disponible', 69990, 6),
(6, 'Sierra de Mesa DeWalt 10"', 'Sierras', 'Prestada', 389990, 0),
(7, 'Sierra Sable Black+Decker', 'Sierras', 'En Reparación', 79990, 1),

-- Categoría: LIJADORAS
(8, 'Lijadora Orbital Bosch 250W', 'Lijadoras', 'Disponible', 59990, 8),
(9, 'Lijadora de Banda Makita', 'Lijadoras', 'Disponible', 149990, 2),

-- Categoría: HERRAMIENTAS MANUALES
(10, 'Kit Llaves Combinadas 12 Piezas', 'Herramientas Manuales', 'Disponible', 45990, 10),
(11, 'Juego Destornilladores Profesional', 'Herramientas Manuales', 'Disponible', 29990, 15),
(12, 'Escalera Telescópica Aluminio 3.8m', 'Herramientas Manuales', 'Prestada', 129990, 0),

-- Categoría: EQUIPOS DE MEDICIÓN
(13, 'Nivel Láser Autonivelante', 'Equipos de Medición', 'Disponible', 89990, 3),
(14, 'Huincha Métrica Láser 50m', 'Equipos de Medición', 'Disponible', 39990, 7),

-- Categoría: COMPRESORES Y AIRE
(15, 'Compresor de Aire 50L 2HP', 'Compresores', 'Disponible', 189990, 2),
(16, 'Pistola de Pintura HVLP', 'Compresores', 'Disponible', 79990, 4),

-- Categoría: EQUIPOS DE SOLDADURA
(17, 'Soldadora Inverter 200A', 'Soldadura', 'Disponible', 159990, 2),

-- Herramientas dadas de BAJA (no disponibles)
(18, 'Taladro Antiguo (Obsoleto)', 'Taladros', 'Baja', 45000, 0),
(19, 'Sierra Circular Dañada', 'Sierras', 'Baja', 89990, 0);

-- ============================================
-- 4. PRÉSTAMOS (Épica 3: Gestión de Préstamos)
-- ============================================

-- PRÉSTAMOS ACTIVOS (en plazo, sin problemas)
INSERT INTO loans (id, client_id, tool_id, start_date, due_date, return_date, status, fine, rental_cost, damaged, irreparable) VALUES
-- María tiene 2 herramientas prestadas, todo en orden
(1, 1, 3, '2025-11-25', '2025-12-09', NULL, 'ACTIVO', 0, 70000, FALSE, FALSE),
(2, 1, 6, '2025-11-28', '2025-12-12', NULL, 'ACTIVO', 0, 70000, FALSE, FALSE),

-- Pedro tiene 1 herramienta, aún en plazo
(3, 2, 12, '2025-11-30', '2025-12-14', NULL, 'ACTIVO', 0, 70000, FALSE, FALSE);

-- PRÉSTAMOS ATRASADOS (vencidos, generan multa)
INSERT INTO loans (id, client_id, tool_id, start_date, due_date, return_date, status, fine, rental_cost, damaged, irreparable) VALUES
-- Diego tiene préstamo atrasado (15 días de atraso = 30.000 CLP multa)
(4, 8, 1, '2025-10-15', '2025-10-29', NULL, 'ATRASADO', 30000, 70000, FALSE, FALSE),

-- Francisca tiene préstamo muy atrasado (30 días = 60.000 CLP multa)
(5, 9, 4, '2025-10-01', '2025-10-15', NULL, 'ATRASADO', 60000, 70000, FALSE, FALSE);

-- PRÉSTAMOS CERRADOS (histórico completo)
INSERT INTO loans (id, client_id, tool_id, start_date, due_date, return_date, status, fine, rental_cost, damaged, irreparable) VALUES
-- Ana: Devolvió a tiempo, sin problemas
(6, 3, 8, '2025-10-01', '2025-10-15', '2025-10-14', 'CERRADO', 0, 70000, FALSE, FALSE),
(7, 3, 13, '2025-10-20', '2025-11-03', '2025-11-02', 'CERRADO', 0, 70000, FALSE, FALSE),

-- Carlos: Devolvió con 3 días de atraso (6.000 CLP multa)
(8, 4, 9, '2025-10-10', '2025-10-24', '2025-10-27', 'CERRADO', 6000, 70000, FALSE, FALSE),

-- Isabel: Devolvió con daño reparable (10.000 CLP multa adicional)
(9, 5, 5, '2025-09-15', '2025-09-29', '2025-09-29', 'CERRADO', 10000, 70000, TRUE, FALSE),

-- Roberto: Devolvió con daño IRREPARABLE (pagó 100% valor reposición = 129.990)
(10, 6, 2, '2025-09-01', '2025-09-15', '2025-09-14', 'CERRADO', 129990, 70000, TRUE, TRUE),

-- Carmen: Varios préstamos históricos exitosos
(11, 7, 14, '2025-08-01', '2025-08-15', '2025-08-15', 'CERRADO', 0, 70000, FALSE, FALSE),
(12, 7, 10, '2025-08-20', '2025-09-03', '2025-09-02', 'CERRADO', 0, 70000, FALSE, FALSE),
(13, 7, 11, '2025-09-10', '2025-09-24', '2025-09-23', 'CERRADO', 0, 70000, FALSE, FALSE),

-- Préstamos adicionales para reportes y estadísticas
(14, 1, 15, '2025-07-01', '2025-07-15', '2025-07-14', 'CERRADO', 0, 70000, FALSE, FALSE),
(15, 2, 16, '2025-07-10', '2025-07-24', '2025-07-23', 'CERRADO', 0, 70000, FALSE, FALSE),
(16, 3, 17, '2025-07-15', '2025-07-29', '2025-07-30', 'CERRADO', 2000, 70000, FALSE, FALSE),
(17, 4, 1, '2025-08-05', '2025-08-19', '2025-08-18', 'CERRADO', 0, 70000, FALSE, FALSE),
(18, 5, 4, '2025-08-10', '2025-08-24', '2025-08-24', 'CERRADO', 0, 70000, FALSE, FALSE);

-- ============================================
-- 5. KARDEX (Épica 5: Trazabilidad de Inventario)
-- ============================================

-- REGISTRO inicial de herramientas
INSERT INTO kardex (id, movement_type, tool_id, quantity, username, movement_date, observations, loan_id) VALUES
(1, 'REGISTRO', 1, 5, 'admin', '2025-01-15 10:00:00', 'Alta inicial de taladros Bosch', NULL),
(2, 'REGISTRO', 2, 3, 'admin', '2025-01-15 10:15:00', 'Alta inicial de taladros Dewalt', NULL),
(3, 'REGISTRO', 3, 1, 'admin', '2025-01-15 10:30:00', 'Alta taladro de columna industrial', NULL),
(4, 'REGISTRO', 4, 4, 'admin', '2025-01-16 09:00:00', 'Alta sierras circulares Makita', NULL),
(5, 'REGISTRO', 5, 6, 'admin', '2025-01-16 09:30:00', 'Alta sierras caladoras Bosch', NULL),
(6, 'REGISTRO', 8, 8, 'admin', '2025-01-20 11:00:00', 'Alta lijadoras orbitales', NULL),
(7, 'REGISTRO', 13, 3, 'admin', '2025-01-25 14:00:00', 'Alta niveles láser', NULL),
(8, 'REGISTRO', 15, 2, 'admin', '2025-02-01 10:00:00', 'Alta compresores de aire', NULL);

-- PRÉSTAMOS (salidas de stock)
INSERT INTO kardex (id, movement_type, tool_id, quantity, username, movement_date, observations, loan_id) VALUES
(9, 'PRESTAMO', 3, -1, 'maria.gonzalez', '2025-11-25 14:30:00', 'Préstamo taladro columna a María González', 1),
(10, 'PRESTAMO', 6, -1, 'maria.gonzalez', '2025-11-28 10:15:00', 'Préstamo sierra de mesa a María González', 2),
(11, 'PRESTAMO', 12, -1, 'pedro.martinez', '2025-11-30 11:00:00', 'Préstamo escalera a Pedro Martínez', 3),
(12, 'PRESTAMO', 1, -1, 'diego.morales', '2025-10-15 09:00:00', 'Préstamo taladro Bosch a Diego (ATRASADO)', 4),
(13, 'PRESTAMO', 4, -1, 'francisca.herrera', '2025-10-01 10:30:00', 'Préstamo sierra circular a Francisca (ATRASADO)', 5);

-- DEVOLUCIONES (ingresos de stock)
INSERT INTO kardex (id, movement_type, tool_id, quantity, username, movement_date, observations, loan_id) VALUES
(14, 'DEVOLUCION', 8, 1, 'ana.silva', '2025-10-14 16:00:00', 'Devolución lijadora en perfecto estado', 6),
(15, 'DEVOLUCION', 13, 1, 'ana.silva', '2025-11-02 15:30:00', 'Devolución nivel láser sin problemas', 7),
(16, 'DEVOLUCION', 9, 1, 'carlos.fernandez', '2025-10-27 17:00:00', 'Devolución con 3 días de atraso', 8),
(17, 'DEVOLUCION', 5, 1, 'isabel.torres', '2025-09-29 14:00:00', 'Devolución con daño reparable detectado', 9),
(18, 'DEVOLUCION', 2, 1, 'roberto.sanchez', '2025-09-14 16:30:00', 'Devolución con daño IRREPARABLE - reposición cobrada', 10);

-- REPARACIONES
INSERT INTO kardex (id, movement_type, tool_id, quantity, username, movement_date, observations, loan_id) VALUES
(19, 'REPARACION', 7, -1, 'admin', '2025-11-15 10:00:00', 'Sierra sable enviada a reparación por desgaste', NULL),
(20, 'REPARACION', 5, -1, 'admin', '2025-09-29 16:00:00', 'Sierra caladora en reparación por daño en préstamo #9', 9),
(21, 'DEVOLUCION', 5, 1, 'admin', '2025-10-05 11:00:00', 'Sierra caladora reparada y lista para uso', 9);

-- BAJAS (herramientas dadas de baja)
INSERT INTO kardex (id, movement_type, tool_id, quantity, username, movement_date, observations, loan_id) VALUES
(22, 'BAJA', 18, 0, 'admin', '2025-08-01 09:00:00', 'Taladro obsoleto dado de baja por antigüedad', NULL),
(23, 'BAJA', 19, 0, 'admin', '2025-08-15 10:00:00', 'Sierra circular con daño irreparable - baja definitiva', NULL),
(24, 'BAJA', 2, -1, 'admin', '2025-09-14 17:00:00', 'Taladro Dewalt dado de baja tras daño irreparable en préstamo #10', 10);

-- ============================================
-- RESUMEN DE DATOS INSERTADOS
-- ============================================
-- ✅ 9 Clientes (7 Activos + 2 Restringidos)
-- ✅ 19 Herramientas (5 categorías diferentes, varios estados)
-- ✅ 18 Préstamos (3 activos + 2 atrasados + 13 cerrados)
-- ✅ 3 Configuraciones del sistema (solo las que se usan en el código)
-- ✅ 24 Movimientos de kardex (trazabilidad completa)
--
-- CASOS DE USO CUBIERTOS:
-- ✅ Épica 1: Gestión de herramientas (CRUD, categorías, estados, stock)
-- ✅ Épica 2: Gestión de clientes (Activos/Restringidos, validaciones)
-- ✅ Épica 3: Préstamos (activos, atrasados, multas, daños)
-- ✅ Épica 4: Autenticación (Keycloak - ya configurado)
-- ✅ Épica 5: Kardex e inventario (trazabilidad completa)
-- ✅ Épica 6: Configuración de tarifas
-- ✅ Épica 7: Reportes (datos suficientes para estadísticas)
-- ============================================

SELECT 'Datos de ejemplo insertados correctamente' AS resultado;
