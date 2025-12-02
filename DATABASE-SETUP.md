# 📦 Configuración de Base de Datos - ToolRent

## Importar Datos de Ejemplo

Para poblar la base de datos con datos de prueba realistas:

```powershell
# Windows PowerShell
docker exec -i toolrent-mysql mysql -uroot -proot123 toolrent < seed-data.sql
```

```bash
# Linux/Mac
docker exec -i toolrent-mysql mysql -uroot -proot123 toolrent < seed-data.sql
```

## 📊 Datos Incluidos

### Clientes (9 total)
- **7 Activos**: María González, Pedro Martínez, Ana Silva, Carlos Fernández, Isabel Torres, Roberto Sánchez, Carmen Ramírez
- **2 Restringidos**: Diego Morales, Francisca Herrera (tienen préstamos atrasados con multas)

### Herramientas (19 total)
- **Taladros**: Percutor Bosch, Inalámbrico Dewalt, Columna Industrial
- **Sierras**: Circular Makita, Caladora Bosch, Mesa DeWalt, Sable Black+Decker
- **Lijadoras**: Orbital Bosch, Banda Makita
- **Herramientas Manuales**: Llaves, Destornilladores, Escalera
- **Medición**: Nivel Láser, Huincha Métrica Láser
- **Compresores**: Compresor 50L, Pistola Pintura
- **Soldadura**: Soldadora Inverter 200A

### Préstamos (18 total)
- **3 Activos**: En plazo, sin problemas
- **2 Atrasados**: Con multas acumulándose (15 y 30 días de atraso)
- **13 Cerrados**: Histórico completo con diferentes escenarios:
  - Devoluciones a tiempo
  - Devoluciones con atraso (multas)
  - Devoluciones con daño reparable
  - Devoluciones con daño irreparable (cobro de reposición)

### Kardex (24 movimientos)
- Registros iniciales de herramientas
- Préstamos (salidas de inventario)
- Devoluciones (entradas de inventario)
- Reparaciones
- Bajas definitivas

### Configuración del Sistema
- Tarifa arriendo diaria: $5.000 CLP
- Multa por atraso diaria: $2.000 CLP
- Días máximos de préstamo: 14
- Multa daño reparable: $10.000 CLP
- Multa daño irreparable: 100% del valor de reposición

## 🎯 Casos de Uso Cubiertos

✅ **Épica 1**: Gestión completa de herramientas (CRUD, categorías, estados, stock)
✅ **Épica 2**: Gestión de clientes (estados Activo/Restringido, validaciones)
✅ **Épica 3**: Préstamos (activos, atrasados, multas automáticas, gestión de daños)
✅ **Épica 4**: Autenticación OAuth2 con Keycloak (ya configurado)
✅ **Épica 5**: Kardex e inventario (trazabilidad completa de movimientos)
✅ **Épica 6**: Configuración de tarifas (ajustables por administrador)
✅ **Épica 7**: Reportes y estadísticas (datos suficientes para análisis)

## 🔄 Crear Backup de Datos Actuales

```powershell
# Windows PowerShell
docker exec toolrent-mysql mysqldump -uroot -proot123 toolrent > backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql
```

```bash
# Linux/Mac
docker exec toolrent-mysql mysqldump -uroot -proot123 toolrent > backup_$(date +%Y%m%d_%H%M%S).sql
```

## 🗑️ Limpiar Base de Datos

Si necesitas resetear completamente:

```sql
-- Conectarse a MySQL
docker exec -it toolrent-mysql mysql -uroot -proot123 toolrent

-- Limpiar todas las tablas
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE kardex;
TRUNCATE TABLE loans;
TRUNCATE TABLE clients;
TRUNCATE TABLE tools;
TRUNCATE TABLE system_config;
SET FOREIGN_KEY_CHECKS = 1;
```

## 📝 Notas

- **seed-data.sql**: Datos de ejemplo para demostración (SÍ versionar en Git)
- **backup_*.sql**: Backups personales (NO versionar - están en .gitignore)
- Los datos de ejemplo usan formato chileno válido para RUT, teléfonos y nombres
- Las multas se calculan automáticamente según configuración del sistema
