CREATE DATABASE  IF NOT EXISTS `toolrent` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `toolrent`;
-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: toolrent
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `clients`
--

DROP TABLE IF EXISTS `clients`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `clients` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rut` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `state` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_clients_email` (`email`),
  UNIQUE KEY `uq_clients_email` (`email`),
  UNIQUE KEY `uk_clients_rut` (`rut`),
  UNIQUE KEY `uq_clients_rut` (`rut`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `clients`
--

LOCK TABLES `clients` WRITE;
/*!40000 ALTER TABLE `clients` DISABLE KEYS */;
INSERT INTO `clients` VALUES (1,'Juan Pérez','12.345.678-9','+56911111112','juan@toolrent.cl','Restringido'),(2,'María Soto','9.876.543-2','+56922222222','maria@toolrent.cl','Restringido'),(3,'Pedro Rojas','7.654.321-0','+56933333333','pedro@toolrent.cl','Restringido'),(4,'Camila Fernández','15.234.567-1','+56944444444','camila@toolrent.cl','Restringido'),(5,'Diego Morales','18.765.432-5','+56955555555','diego@toolrent.cl','Restringido'),(6,'Sofía Navarro','16.789.012-3','+56966666666','sofia@toolrent.cl','Restringido'),(7,'Ignacio Vidal','13.210.987-6','+56977777777','ignacio@toolrent.cl','Restringido'),(8,'Valentina Campos','14.598.321-2','+56988888888','valentina@toolrent.cl','Restringido'),(9,'Felipe Arancibia','19.876.543-2','+56999999999','felipe@toolrent.cl','Restringido'),(10,'Constanza Fuentes','11.223.344-5','+56910101010','constanza@toolrent.cl','Restringido'),(13,'Luis Palma','22.222.222-2','+56999990000','luis.palma@toolrent.cl','Activo'),(15,'Carla Muñoz','23.333.333-3','+56988887771','carla.munoz@toolrent.cl','Restringido'),(16,'Pedro López','44.444.444-4','+56944444444','pedro.lopez@toolrent.cl','Restringido'),(17,'Pedro Riquelme','25.123.456-5','+56955550011','qa_1756499329605@toolrent.cl','Restringido'),(18,'Cliente Test cliente1756501569282@toolrent.cl','55.555.552-5','+56988887777','cliente1756501569282@toolrent.cl','Restringido'),(19,'Cliente 1756502053564','RUT175650205356','+56912345678','cliente1756502053564@mail.com','Activo'),(20,'Cliente 1756502752248','RUT175650275224','+56912345678','cliente1756502752248@mail.com','Activo'),(21,'Cliente 1756503601006','','+56919489170','','Activo'),(22,'diego manrquez','20123541-5','+5694152140','diegom@gmail.com','Restringido'),(23,'martin ovirdo','10.521.541-4','+56941520140','martin@gmail.com','Activo'),(24,'Daniel vilchez','12.459.187-k','+56997154230','danielv@toolrent.cl','Restringido');
/*!40000 ALTER TABLE `clients` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `kardex`
--

DROP TABLE IF EXISTS `kardex`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kardex` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `loan_id` bigint DEFAULT NULL,
  `movement_date` datetime(6) NOT NULL,
  `movement_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `observations` text COLLATE utf8mb4_unicode_ci,
  `quantity` int NOT NULL,
  `username` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tool_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkhcnqk73jv7wfjb0iv29juwm0` (`tool_id`),
  CONSTRAINT `FKkhcnqk73jv7wfjb0iv29juwm0` FOREIGN KEY (`tool_id`) REFERENCES `tools` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `kardex`
--

LOCK TABLES `kardex` WRITE;
/*!40000 ALTER TABLE `kardex` DISABLE KEYS */;
INSERT INTO `kardex` VALUES (1,NULL,'2025-09-10 00:15:55.683719','REGISTRO','Alta inicial de Rotomartillo',5,'ADMIN',1),(2,1,'2025-09-30 00:15:55.726191','PRESTAMO','Préstamo de Rotomartillo',-1,'USER',1),(3,1,'2025-10-07 00:15:55.731622','DEVOLUCION','Devolución en buen estado de Rotomartillo',1,'USER',1),(4,NULL,'2025-09-20 00:15:55.736633','REGISTRO','Alta inicial de Taladro Percutor',3,'ADMIN',2),(5,NULL,'2025-10-05 00:15:55.741699','BAJA','Baja por daño irreparable de Sierra Circular',-1,'ADMIN',3),(6,2,'2025-10-08 00:15:55.746644','REPARACION','Herramienta en reparación por daños leves',0,'USER',2),(7,12,'2025-10-10 00:52:16.933039','PRESTAMO','Préstamo a cliente: Felipe Arancibia',-1,'USER',7),(8,NULL,'2025-10-10 03:17:19.132152','REGISTRO','Alta de herramienta: Esmreal',5,'ADMIN',37),(9,13,'2025-10-10 03:18:58.296909','PRESTAMO','Préstamo a cliente: Valentina Campos',-1,'USER',9),(10,NULL,'2025-10-10 03:22:49.625793','BAJA','Baja de herramienta: Inactiva Pistola de Calor 1756501568597',-2,'ADMIN',28),(11,10,'2025-10-14 01:14:36.656760','DEVOLUCION','Devolución en buen estado de préstamo #10',1,'USER',1),(12,12,'2025-10-14 02:23:29.165756','DEVOLUCION','Devolución en buen estado de préstamo #12',1,'USER',7),(13,14,'2025-10-14 02:30:25.113071','PRESTAMO','Préstamo a cliente: Carla Muñoz',-1,'USER',11),(14,15,'2025-10-14 02:37:15.292460','PRESTAMO','Préstamo a cliente: Pedro López',-1,'USER',6),(15,3,'2025-10-14 03:54:42.252902','DEVOLUCION','Devolución normal (loan #3)',1,'USER',26),(16,11,'2025-10-14 03:55:09.998650','DEVOLUCION','Devolución normal (loan #11)',1,'USER',6),(17,13,'2025-10-14 03:56:44.750440','DEVOLUCION','Devolución normal (loan #13)',1,'USER',9),(18,9,'2025-10-15 21:47:09.685495','DEVOLUCION','Devolución normal (loan #9)',1,'USER',8),(19,14,'2025-10-15 21:47:29.601592','BAJA','Baja por daño irreparable (loan #14)',0,'USER',11),(20,16,'2025-10-16 01:06:20.098800','PRESTAMO','Préstamo a cliente: diego manrquez',-1,'USER',1),(21,NULL,'2025-10-17 22:12:11.152718','REGISTRO','Alta de herramienta: Talabro Brosh',2,'ADMIN',38),(22,17,'2025-10-17 22:39:46.647925','PRESTAMO','Préstamo a cliente: Ignacio Vidal',-1,'USER',38),(23,NULL,'2025-10-18 00:10:48.782152','BAJA','Baja de herramienta: Compresor de Aire 24L',-1,'diego',7),(24,18,'2025-10-18 00:15:48.409863','PRESTAMO','Préstamo a cliente: Sofía Navarro',-1,'diego',36),(25,19,'2025-10-18 01:25:42.199736','PRESTAMO','Préstamo a cliente: Ignacio Vidal',-1,'diego',36),(26,15,'2025-10-24 00:12:23.882673','BAJA','Baja por daño irreparable (préstamo #15)',0,'diego',6),(27,16,'2025-10-24 00:59:29.285298','DEVOLUCION','Devolución con daño reparable (préstamo #16)',1,'diego',1),(28,20,'2025-10-24 02:03:54.171608','PRESTAMO','Préstamo a cliente: Pedro Rojas',-1,'diego',4),(29,21,'2025-10-24 23:47:42.774255','PRESTAMO','Préstamo a cliente: Constanza Fuentes',-1,'diego',4),(30,22,'2025-10-25 02:06:40.226035','PRESTAMO','Préstamo a cliente: Daniel vilchez',-1,'diego',1),(31,23,'2025-10-25 02:54:23.793095','PRESTAMO','Préstamo a cliente: Constanza Fuentes',-1,'diego',5),(32,24,'2025-10-27 23:47:22.253871','PRESTAMO','Préstamo a cliente: Daniel vilchez',-1,'diego',5);
/*!40000 ALTER TABLE `kardex` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loans`
--

DROP TABLE IF EXISTS `loans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `damaged` bit(1) DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `fine` double DEFAULT NULL,
  `irreparable` bit(1) DEFAULT NULL,
  `return_date` date DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `client_id` bigint NOT NULL,
  `tool_id` bigint NOT NULL,
  `rental_cost` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_loans_client` (`client_id`),
  KEY `fk_loans_tool` (`tool_id`),
  CONSTRAINT `fk_loans_client` FOREIGN KEY (`client_id`) REFERENCES `clients` (`id`),
  CONSTRAINT `fk_loans_tool` FOREIGN KEY (`tool_id`) REFERENCES `tools` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loans`
--

LOCK TABLES `loans` WRITE;
/*!40000 ALTER TABLE `loans` DISABLE KEYS */;
INSERT INTO `loans` VALUES (1,NULL,'2025-09-02',36,NULL,'2025-10-08','2025-08-29','Atrasado',15,1,NULL),(2,NULL,'2025-09-08',55030,NULL,'2025-10-08','2025-08-29','Atrasado',17,22,NULL),(3,NULL,'2025-09-08',216000,NULL,'2025-10-14','2025-08-29','Atrasado',18,26,NULL),(4,NULL,'2025-09-05',294000,NULL,NULL,'2025-09-03','Atrasado',2,1,NULL),(5,NULL,'2025-09-11',21,NULL,'2025-10-02','2025-09-09','Atrasado',17,2,NULL),(6,NULL,'2025-09-12',130000,NULL,'2025-10-08','2025-09-09','Atrasado',1,1,NULL),(7,NULL,'2025-10-02',95006,NULL,'2025-10-08','2025-09-29','Atrasado',2,3,NULL),(8,NULL,'2025-10-03',0,NULL,'2025-10-02','2025-10-02','Devuelto',9,10,NULL),(9,_binary '\0','2025-10-05',60000,_binary '\0','2025-10-15','2025-10-02','Atrasado',5,8,NULL),(10,_binary '\0','2025-10-09',30000,_binary '\0','2025-10-14','2025-10-08','Atrasado',1,1,NULL),(11,_binary '\0','2025-10-11',18000,_binary '\0','2025-10-14','2025-10-09','Atrasado',4,6,16000),(12,_binary '\0','2025-10-12',12000,_binary '\0','2025-10-14','2025-10-10','Atrasado',9,7,8000),(13,_binary '\0','2025-10-12',12000,_binary '\0','2025-10-14','2025-10-10','Atrasado',8,9,8000),(14,_binary '','2025-10-15',90000,_binary '','2025-10-15','2025-10-14','Atrasado',15,11,8000),(15,_binary '','2025-10-16',108000,_binary '','2025-10-24','2025-10-14','Atrasado',16,6,8000),(16,_binary '','2025-10-17',72000,_binary '\0','2025-10-24','2025-10-16','Atrasado',22,1,8000),(17,_binary '\0','2025-10-19',30000,_binary '\0',NULL,'2025-10-17','Atrasado',7,38,16000),(18,_binary '\0','2025-10-19',30000,_binary '\0',NULL,'2025-10-18','Atrasado',6,36,8000),(19,_binary '\0','2025-10-19',30000,_binary '\0',NULL,'2025-10-18','Atrasado',7,36,8000),(20,_binary '\0','2025-10-26',6000,_binary '\0',NULL,'2025-10-24','Atrasado',3,4,16000),(21,_binary '\0','2025-10-26',6000,_binary '\0',NULL,'2025-10-24','Atrasado',10,4,16000),(22,_binary '\0','2025-10-27',6000,_binary '\0',NULL,'2025-10-25','Atrasado',24,1,16000),(23,_binary '\0','2025-10-30',6000,_binary '\0',NULL,'2025-10-25','Atrasado',10,5,40000),(24,_binary '\0','2025-10-30',6000,_binary '\0',NULL,'2025-10-27','Atrasado',24,5,24000);
/*!40000 ALTER TABLE `loans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_config`
--

DROP TABLE IF EXISTS `system_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` double NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_modified` datetime(6) NOT NULL,
  `modified_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnpsxm1erd0lbetjn5d3ayrsof` (`config_key`),
  CONSTRAINT `system_config_chk_1` CHECK ((`config_value` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_config`
--

LOCK TABLES `system_config` WRITE;
/*!40000 ALTER TABLE `system_config` DISABLE KEYS */;
INSERT INTO `system_config` VALUES (1,'TARIFA_ARRIENDO_DIARIA',8000,'Tarifa diaria de arriendo por herramienta','2025-10-09 03:05:40.043621','diego'),(2,'TARIFA_MULTA_DIARIA',6000,'Tarifa diaria de multa por atraso','2025-10-10 00:53:17.669430','diego'),(3,'CARGO_REPARACION',30000,'Cargo fijo por reparación de herramientas con daños leves','2025-10-17 22:13:06.207262','diego');
/*!40000 ALTER TABLE `system_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tools`
--

DROP TABLE IF EXISTS `tools`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tools` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `replacement_value` int DEFAULT NULL,
  `stock` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_tools_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tools`
--

LOCK TABLES `tools` WRITE;
/*!40000 ALTER TABLE `tools` DISABLE KEYS */;
INSERT INTO `tools` VALUES (1,'Rotomartillo','Eléctricas','En reparación',120000,2),(2,'Taladro Percutor','Eléctricas','Dada de baja',80000,0),(3,'Sierra Circular','Corte','Dada de baja',95000,0),(4,'Esmeril Angular 4.5\"','Eléctricas','Disponible',70000,2),(5,'Atornillador Inalámbrico','Eléctricas','Disponible',65000,5),(6,'Lijadora Orbital','Eléctricas','Dada de baja',60000,1),(7,'Compresor de Aire 24L','Aire','Dada de baja',180000,0),(8,'Generador 2kW','Energía','Disponible',350000,1),(9,'Soldadora Inverter','Soldadura','Disponible',160000,2),(10,'Cortacésped','Jardín','Disponible',220000,1),(11,'Escalera Extensible','Manuales','Dada de baja',90000,2),(12,'Carretilla','Manuales','Disponible',45000,6),(13,'Pistola de Calor','Eléctricas','Disponible',55000,2),(15,'','Eléctricas','Disponible',-1,-1),(22,'Taladro Percutor 800W 1756499329605','Eléctricas','Dada de baja',55000,2),(25,'Martillo Demoledor 10kg 1756499329605','Eléctricas','Inactivo',160000,1),(26,'Pistola de Calor 1756501568597','Eléctricas','Disponible',55000,2),(28,'Inactiva Pistola de Calor 1756501568597','Eléctricas','Dada de baja',15000,0),(29,'Pistola de Calor 1756502052926','Eléctricas','Disponible',55000,2),(31,'Taladro Inactivo 1756502053456','Eléctricas','En reparación',55000,2),(32,'Pistola de Calor 1756502751447','Eléctricas','Disponible',55000,2),(34,'Taladro Inactivo 1756502752157','Eléctricas','En reparación',55000,2),(35,'tool-inactive-1756503601006','Eléctricas','Inactivo',55000,1),(36,'Desatornillador','Electrica','Disponible',50001,6),(37,'Esmreal','Eléctricas','Disponible',120000,5),(38,'Talabro Brosh','Eléctricas','Disponible',120000,1);
/*!40000 ALTER TABLE `tools` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-11-04  2:32:41
