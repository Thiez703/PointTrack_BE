-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: pointtrack_db
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `attendance_audit_logs`
--

DROP TABLE IF EXISTS `attendance_audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attendance_record_id` bigint NOT NULL,
  `changed_at` datetime(6) NOT NULL,
  `changed_by_user_id` bigint NOT NULL,
  `field_name` varchar(100) NOT NULL,
  `new_value` varchar(500) DEFAULT NULL,
  `old_value` varchar(500) DEFAULT NULL,
  `reason` varchar(1000) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_aal_record` (`attendance_record_id`),
  KEY `idx_aal_changed_by` (`changed_by_user_id`),
  KEY `idx_aal_changed_at` (`changed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_audit_logs`
--

LOCK TABLES `attendance_audit_logs` WRITE;
/*!40000 ALTER TABLE `attendance_audit_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `attendance_audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_photos`
--

DROP TABLE IF EXISTS `attendance_photos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_photos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `captured_at` datetime(6) DEFAULT NULL,
  `captured_lat` double DEFAULT NULL,
  `captured_lng` double DEFAULT NULL,
  `file_size_bytes` bigint DEFAULT NULL,
  `mime_type` varchar(50) DEFAULT NULL,
  `original_file_name` varchar(255) DEFAULT NULL,
  `photo_url` varchar(1000) NOT NULL,
  `type` enum('CHECK_IN','CHECK_OUT') NOT NULL,
  `attendance_record_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKi4ibignexs6bhkhxdc33iytox` (`attendance_record_id`),
  CONSTRAINT `FKi4ibignexs6bhkhxdc33iytox` FOREIGN KEY (`attendance_record_id`) REFERENCES `attendance_records` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_photos`
--

LOCK TABLES `attendance_photos` WRITE;
/*!40000 ALTER TABLE `attendance_photos` DISABLE KEYS */;
/*!40000 ALTER TABLE `attendance_photos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_records`
--

DROP TABLE IF EXISTS `attendance_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `actual_minutes` int DEFAULT NULL,
  `check_in_distance_meters` double DEFAULT NULL,
  `check_in_lat` double DEFAULT NULL,
  `check_in_lng` double DEFAULT NULL,
  `check_in_time` datetime(6) DEFAULT NULL,
  `check_out_distance_meters` double DEFAULT NULL,
  `check_out_lat` double DEFAULT NULL,
  `check_out_lng` double DEFAULT NULL,
  `check_out_time` datetime(6) DEFAULT NULL,
  `early_leave_minutes` int NOT NULL,
  `late_minutes` int NOT NULL,
  `note` varchar(1000) DEFAULT NULL,
  `ot_multiplier` decimal(3,1) NOT NULL,
  `status` enum('ABSENT','EARLY_LEAVE','LATE','ON_TIME','PENDING_APPROVAL') NOT NULL,
  `user_id` bigint NOT NULL,
  `work_schedule_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKfh6uyplk8ru1ic9i070ey55vb` (`work_schedule_id`),
  KEY `idx_ar_user` (`user_id`),
  KEY `idx_ar_schedule` (`work_schedule_id`),
  KEY `idx_ar_status` (`status`),
  KEY `idx_ar_checkin` (`check_in_time`),
  CONSTRAINT `FK2yka8cp9l26e4kkyab5iyf3ef` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKi5qf20hxtohbj2fyx9uobqbic` FOREIGN KEY (`work_schedule_id`) REFERENCES `work_schedules` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_records`
--

LOCK TABLES `attendance_records` WRITE;
/*!40000 ALTER TABLE `attendance_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `attendance_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customers`
--

DROP TABLE IF EXISTS `customers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `district` varchar(100) DEFAULT NULL,
  `email` varchar(150) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `note` varchar(500) DEFAULT NULL,
  `phone_number` varchar(15) DEFAULT NULL,
  `street` varchar(255) DEFAULT NULL,
  `ward` varchar(100) DEFAULT NULL,
  `address` text,
  `phone` varchar(20) DEFAULT NULL,
  `preferred_time_note` varchar(255) DEFAULT NULL,
  `secondary_phone` varchar(20) DEFAULT NULL,
  `source` enum('ZALO','FACEBOOK','REFERRAL','HOTLINE','OTHER') NOT NULL,
  `special_notes` text,
  `status` enum('ACTIVE','INACTIVE','SUSPENDED') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_customer_status` (`status`),
  KEY `idx_customer_phone` (`phone`),
  KEY `idx_customer_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customers`
--

LOCK TABLES `customers` WRITE;
/*!40000 ALTER TABLE `customers` DISABLE KEYS */;
INSERT INTO `customers` VALUES (1,'2026-03-20 18:51:19.180402',9999,'2026-03-20 18:51:19.180402',9999,'TP.HCM',NULL,'Quận 1',NULL,_binary '',10.7782,106.7011,'Khách hàng Mẫu (Trung tâm Q1)',NULL,'0901234567','72 Lê Thánh Tôn','Bến Nghé',NULL,NULL,NULL,NULL,'ZALO',NULL,'ACTIVE'),(2,'2026-03-21 19:51:39.026023',1,'2026-03-21 19:51:39.026023',1,NULL,NULL,NULL,NULL,NULL,10.035002942421794,105.7840383052826,'Nguyen Chi Huynh',NULL,NULL,NULL,NULL,'Hẻm 18 Hòa Bình, Ninh Kiều, Phường Ninh Kiều, Thành phố Cần Thơ, 94111, Việt Nam','0987654679',NULL,NULL,'OTHER',NULL,'ACTIVE'),(3,'2026-03-22 12:40:09.387112',1,'2026-03-22 12:40:09.387112',1,NULL,NULL,NULL,NULL,NULL,10.6878576,105.096672,'Tran Quoc Thai',NULL,NULL,NULL,NULL,'Thành phố Châu Đốc, Châu Đốc, Phường Châu Đốc, Tỉnh An Giang, Việt Nam','0706710269',NULL,NULL,'ZALO',NULL,'ACTIVE'),(4,'2026-03-22 13:43:10.985598',1,'2026-03-22 13:43:10.985598',1,NULL,NULL,NULL,NULL,NULL,10.0362046,105.7872656,'Chí Thiện Huỳnh',NULL,NULL,NULL,NULL,'Thành phố Cần Thơ, Việt Nam','0865564566',NULL,NULL,'OTHER',NULL,'ACTIVE'),(5,'2026-03-22 14:47:03.614566',9999,'2026-03-22 14:47:03.614566',9999,NULL,NULL,NULL,NULL,NULL,10.7782,106.7011,'Khách hàng Mẫu (Trung tâm Q1)',NULL,NULL,NULL,NULL,'72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP.HCM','0901234567',NULL,NULL,'OTHER',NULL,'ACTIVE'),(6,'2026-03-23 06:20:05.605199',1,'2026-03-23 06:20:05.605199',1,NULL,NULL,NULL,NULL,NULL,10.850334306741532,106.52887781592476,'Thuộc',NULL,NULL,NULL,NULL,'An Hạ, Ấp 75, Xã Vĩnh Lộc, Thành phố Hồ Chí Minh, 71821, Việt Nam','0234566777',NULL,NULL,'OTHER',NULL,'ACTIVE');
/*!40000 ALTER TABLE `customers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `explanation_requests`
--

DROP TABLE IF EXISTS `explanation_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `explanation_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `reason` varchar(1000) DEFAULT NULL,
  `review_note` varchar(500) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') NOT NULL,
  `type` enum('GPS_INVALID','LATE_CHECKIN','LATE_CHECKOUT') NOT NULL,
  `attendance_record_id` bigint NOT NULL,
  `reviewed_by_user_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_er_record` (`attendance_record_id`),
  KEY `idx_er_user_status` (`user_id`,`status`),
  KEY `FKo65n9o7w0nbs5a4nejs3gcy7j` (`reviewed_by_user_id`),
  CONSTRAINT `FK6qvi1urdr2fe48vcht4f91kmn` FOREIGN KEY (`attendance_record_id`) REFERENCES `attendance_records` (`id`),
  CONSTRAINT `FKl5ueca3svmdy0hviqko7n3ywa` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKo65n9o7w0nbs5a4nejs3gcy7j` FOREIGN KEY (`reviewed_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `explanation_requests`
--

LOCK TABLES `explanation_requests` WRITE;
/*!40000 ALTER TABLE `explanation_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `explanation_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(100) NOT NULL,
  `group_name` enum('ADMINISTRATION','SETTING','SYSTEM') DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `type` enum('ACTION','MODULE') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK7lcb6glmvwlro3p2w2cewxtvd` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permissions`
--

LOCK TABLES `permissions` WRITE;
/*!40000 ALTER TABLE `permissions` DISABLE KEYS */;
INSERT INTO `permissions` VALUES (1,'USER_READ','ADMINISTRATION','Xem danh sách người dùng',NULL,'ACTION'),(2,'USER_MANAGE','ADMINISTRATION','Quản lý người dùng',NULL,'ACTION'),(3,'ROLE_READ','ADMINISTRATION','Xem danh sách vai trò',NULL,'ACTION'),(4,'ROLE_MANAGE','ADMINISTRATION','Quản lý vai trò',NULL,'ACTION');
/*!40000 ALTER TABLE `permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `display_name` varchar(100) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_system` bit(1) DEFAULT NULL,
  `slug` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKsx80rwev5en94r3jv7riyoh1y` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'2026-03-20 18:51:18.661431',9999,'2026-03-20 18:51:18.661431',9999,'Toàn quyền hệ thống','Quản trị viên',_binary '',_binary '','ADMIN'),(2,'2026-03-20 18:51:18.688843',9999,'2026-03-20 18:51:18.688843',9999,'Nhân viên phục vụ tại nhà khách hàng','Nhân viên',_binary '',_binary '','USER');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles_permissions`
--

DROP TABLE IF EXISTS `roles_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles_permissions` (
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `FKbx9r9uw77p58gsq4mus0mec0o` (`permission_id`),
  CONSTRAINT `FKbx9r9uw77p58gsq4mus0mec0o` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`),
  CONSTRAINT `FKqi9odri6c1o81vjox54eedwyh` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles_permissions`
--

LOCK TABLES `roles_permissions` WRITE;
/*!40000 ALTER TABLE `roles_permissions` DISABLE KEYS */;
INSERT INTO `roles_permissions` VALUES (1,1),(1,2),(1,3),(1,4);
/*!40000 ALTER TABLE `roles_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `salary_level_history`
--

DROP TABLE IF EXISTS `salary_level_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `salary_level_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `effective_date` date NOT NULL,
  `reason` text,
  `changed_by` bigint NOT NULL,
  `employee_id` bigint NOT NULL,
  `new_level_id` bigint NOT NULL,
  `old_level_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_slh_employee` (`employee_id`),
  KEY `idx_slh_effective` (`effective_date`),
  KEY `FK6liblwecg5q6xqt39m8p3xtuc` (`changed_by`),
  KEY `FKfh5vv2e4kogl13o8e3ipbgtn0` (`new_level_id`),
  KEY `FK2rd7muxnpmaqorq2wy693bt4p` (`old_level_id`),
  CONSTRAINT `FK2rd7muxnpmaqorq2wy693bt4p` FOREIGN KEY (`old_level_id`) REFERENCES `salary_levels` (`id`),
  CONSTRAINT `FK6liblwecg5q6xqt39m8p3xtuc` FOREIGN KEY (`changed_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKfh5vv2e4kogl13o8e3ipbgtn0` FOREIGN KEY (`new_level_id`) REFERENCES `salary_levels` (`id`),
  CONSTRAINT `FKms7yhwh5sdg2qdhelf5kdcpqy` FOREIGN KEY (`employee_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `salary_level_history`
--

LOCK TABLES `salary_level_history` WRITE;
/*!40000 ALTER TABLE `salary_level_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `salary_level_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `salary_levels`
--

DROP TABLE IF EXISTS `salary_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `salary_levels` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `base_salary` decimal(38,2) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe5v8cvu5qpiwmilomwkvmsq3` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `salary_levels`
--

LOCK TABLES `salary_levels` WRITE;
/*!40000 ALTER TABLE `salary_levels` DISABLE KEYS */;
INSERT INTO `salary_levels` VALUES (1,'2026-03-20 18:51:18.705796',9999,'2026-03-20 18:51:18.705796',9999,50000.00,NULL,'Lương cơ bản 50.000 VNĐ/giờ',_binary '','Cấp 1'),(2,'2026-03-20 18:51:18.715261',9999,'2026-03-20 18:51:18.715261',9999,70000.00,NULL,'Lương cơ bản 70.000 VNĐ/giờ',_binary '','Cấp 2'),(3,'2026-03-20 18:51:18.728722',9999,'2026-03-20 18:51:18.728722',9999,100000.00,NULL,'Lương cơ bản 100.000 VNĐ/giờ',_binary '','Cấp 3');
/*!40000 ALTER TABLE `salary_levels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_packages`
--

DROP TABLE IF EXISTS `service_packages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_packages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `completed_sessions` int NOT NULL,
  `notes` text,
  `recurrence_pattern` json NOT NULL,
  `status` enum('ACTIVE','COMPLETED','CANCELLED') NOT NULL,
  `total_sessions` int NOT NULL,
  `customer_id` bigint NOT NULL,
  `employee_id` bigint NOT NULL,
  `template_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pkg_customer` (`customer_id`),
  KEY `idx_pkg_employee` (`employee_id`),
  KEY `idx_pkg_status` (`status`),
  KEY `FKbvqsqrdilqy2txpxgj81vyj8i` (`template_id`),
  CONSTRAINT `FK7yyt2ccd13k6n4ah423t44pjl` FOREIGN KEY (`employee_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK8yg5snoo5kjicjvmd0vtbd8re` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKbvqsqrdilqy2txpxgj81vyj8i` FOREIGN KEY (`template_id`) REFERENCES `shift_templates` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_packages`
--

LOCK TABLES `service_packages` WRITE;
/*!40000 ALTER TABLE `service_packages` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_packages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shift_templates`
--

DROP TABLE IF EXISTS `shift_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shift_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `color` varchar(7) DEFAULT NULL,
  `default_end` time(6) NOT NULL,
  `default_start` time(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `duration_minutes` int NOT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `ot_multiplier` decimal(3,1) NOT NULL,
  `shift_type` enum('HOLIDAY','NORMAL','OT_EMERGENCY') NOT NULL,
  `notes` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKt6vkxf9392jt4ehmxnfeh2yrk` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shift_templates`
--

LOCK TABLES `shift_templates` WRITE;
/*!40000 ALTER TABLE `shift_templates` DISABLE KEYS */;
INSERT INTO `shift_templates` VALUES (1,'2026-03-20 18:51:19.196206',9999,'2026-03-20 18:51:19.196206',9999,'#4CAF50','12:00:00.000000','08:00:00.000000',NULL,240,_binary '','Ca Sáng (08:00 - 12:00)',1.0,'NORMAL',NULL);
/*!40000 ALTER TABLE `shift_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shifts`
--

DROP TABLE IF EXISTS `shifts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shifts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `duration_minutes` int NOT NULL,
  `end_time` time(6) NOT NULL,
  `notes` text,
  `ot_multiplier` decimal(3,1) NOT NULL,
  `shift_date` date NOT NULL,
  `shift_type` enum('NORMAL','HOLIDAY','OT_EMERGENCY') NOT NULL,
  `start_time` time(6) NOT NULL,
  `status` enum('DRAFT','PUBLISHED','ASSIGNED','SCHEDULED','CONFIRMED','IN_PROGRESS','COMPLETED','MISSED','MISSING_OUT','CANCELLED') NOT NULL DEFAULT 'ASSIGNED',
  `customer_id` bigint NOT NULL,
  `employee_id` bigint DEFAULT NULL,
  `package_id` bigint DEFAULT NULL,
  `template_id` bigint DEFAULT NULL,
  `check_in_distance_meters` double DEFAULT NULL,
  `check_in_lat` double DEFAULT NULL,
  `check_in_lng` double DEFAULT NULL,
  `check_in_time` datetime(6) DEFAULT NULL,
  `check_out_distance_meters` double DEFAULT NULL,
  `check_out_lat` double DEFAULT NULL,
  `check_out_lng` double DEFAULT NULL,
  `check_out_time` datetime(6) DEFAULT NULL,
  `actual_minutes` int DEFAULT NULL,
  `check_in_photo` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_shift_employee_date` (`employee_id`,`shift_date`),
  KEY `idx_shift_package` (`package_id`),
  KEY `FKlfuul8umjslilellec3mqt0wq` (`customer_id`),
  KEY `FK41wwftsnxuctqe05814okb6rm` (`template_id`),
  KEY `idx_shift_status` (`status`),
  CONSTRAINT `FK41wwftsnxuctqe05814okb6rm` FOREIGN KEY (`template_id`) REFERENCES `shift_templates` (`id`),
  CONSTRAINT `FKe6y6pfw391f15lxc9v6a2td1h` FOREIGN KEY (`package_id`) REFERENCES `service_packages` (`id`),
  CONSTRAINT `FKlfuul8umjslilellec3mqt0wq` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKsomapa4hyrpy4dgl13tm2oamt` FOREIGN KEY (`employee_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shifts`
--

LOCK TABLES `shifts` WRITE;
/*!40000 ALTER TABLE `shifts` DISABLE KEYS */;
INSERT INTO `shifts` VALUES (1,'2026-03-21 14:29:40.303318',1,'2026-03-21 14:29:40.303318',1,240,'12:00:00.000000',NULL,1.0,'2026-03-21','NORMAL','08:00:00.000000','SCHEDULED',1,2,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(2,'2026-03-30 05:20:42.772122',1,'2026-03-30 05:20:42.772122',1,240,'12:00:00.000000','',1.0,'2026-03-30','NORMAL','08:00:00.000000','ASSIGNED',1,4,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(3,'2026-03-30 05:46:18.448605',1,'2026-03-30 05:46:18.448605',1,240,'12:00:00.000000','',1.0,'2026-01-01','NORMAL','08:00:00.000000','ASSIGNED',5,4,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `shifts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_settings`
--

DROP TABLE IF EXISTS `system_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_settings` (
  `setting_key` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `setting_value` text NOT NULL,
  PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_settings`
--

LOCK TABLES `system_settings` WRITE;
/*!40000 ALTER TABLE `system_settings` DISABLE KEYS */;
INSERT INTO `system_settings` VALUES ('GPS_RADIUS_METERS','Bán kính GPS fencing cho phép check-in (meters) — BR-14',NULL,NULL,'50'),('GRACE_PERIOD_MINUTES','Số phút check-in muộn vẫn tính đúng giờ (BR-11)',NULL,NULL,'5'),('LATE_CHECKOUT_THRESHOLD_MINUTES','Số phút checkout quá giờ kết thúc ca bắt buộc nhập lý do — BR-16.2',NULL,NULL,'30'),('PENALTY_RULES','Bậc thang trừ công khi check-in muộn (BR-12)',NULL,NULL,'[{\"minLateMinutes\":15,\"penaltyShift\":0.5},{\"minLateMinutes\":30,\"penaltyShift\":1.0}]'),('TRAVEL_BUFFER_MINUTES','Thời gian đệm di chuyển tối thiểu giữa 2 ca (BR-09)',NULL,NULL,'15');
/*!40000 ALTER TABLE `system_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `email` varchar(150) DEFAULT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `gender` tinyint DEFAULT NULL,
  `is_first_login` bit(1) NOT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `last_login_ip` varchar(45) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone_number` varchar(15) DEFAULT NULL,
  `reset_password_token` varchar(100) DEFAULT NULL,
  `reset_token_expired_at` datetime(6) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `role_id` bigint DEFAULT NULL,
  `salary_level_id` bigint DEFAULT NULL,
  `area` varchar(100) DEFAULT NULL,
  `skills` json DEFAULT NULL,
  `otp_code` varchar(6) DEFAULT NULL,
  `otp_expired_at` datetime(6) DEFAULT NULL,
  `otp_verified` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK9q63snka3mdh91as4io72espi` (`phone_number`),
  KEY `idx_user_deleted_at` (`deleted_at`),
  KEY `idx_user_status` (`status`),
  KEY `FKp56c1712k691lhsyewcssf40f` (`role_id`),
  KEY `FK7ywdim6bdx6s72dqwhb1ohxj8` (`salary_level_id`),
  CONSTRAINT `FK7ywdim6bdx6s72dqwhb1ohxj8` FOREIGN KEY (`salary_level_id`) REFERENCES `salary_levels` (`id`),
  CONSTRAINT `FKp56c1712k691lhsyewcssf40f` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2026-03-20 18:51:19.015809',9999,'2026-03-30 07:40:53.709663',9999,NULL,NULL,NULL,'admin@pointtrack.com','PointTrack Admin',NULL,_binary '\0','2026-03-30 07:40:53.708664','0:0:0:0:0:0:0:1','$2a$10$RRmmKLDpfX8SIIPAR3uuWebsVrNnHB5MckQKzcSUBQjs78TfmJR1S','0987654321',NULL,NULL,NULL,'ACTIVE',1,NULL,NULL,NULL,NULL,NULL,NULL),(2,'2026-03-20 18:51:19.160876',9999,'2026-03-22 15:28:22.721592',9999,NULL,NULL,NULL,'employee@pointtrack.com','Employee Test',NULL,_binary '','2026-03-21 13:38:25.290481','0:0:0:0:0:0:0:1','$2a$10$g/PWgcpsMB7uHe7egmK4a.PPfeQqG4iqWKnY8viLgixqX9FhO3I0W','0123456789',NULL,NULL,NULL,'ACTIVE',2,NULL,NULL,NULL,NULL,NULL,NULL),(3,'2026-03-21 15:04:39.302681',1,'2026-03-25 08:04:39.874544',1,NULL,NULL,NULL,'tranthai16092020@gmail.com','Tran Quoc Thai',NULL,_binary '',NULL,NULL,'$2a$10$XsFUbksjjmCiqiwfzqqdMeuRpTHk443/nq4l.SiSoGzNw2JMElqZi','0706710269',NULL,NULL,'2026-03-25','ACTIVE',2,2,'An Giang','[\"tam_be\"]',NULL,NULL,NULL),(4,'2026-03-21 16:10:00.357945',1,'2026-03-30 08:33:49.275833',9999,NULL,NULL,NULL,'huynhchithien151@gmail.com','Chí Thiện Huỳnh',NULL,_binary '\0','2026-03-30 08:33:49.261841','0:0:0:0:0:0:0:1','$2a$10$1pmFBUtCHTZEMWksjtpkZOKN2DtXvSiaaT1t9APIAwaszU9sNXobK','0865809187',NULL,NULL,'2026-03-25','ACTIVE',2,1,'Cà Mau','[\"tam_be\"]',NULL,NULL,NULL),(5,'2026-03-21 16:14:00.726543',1,'2026-03-21 16:14:00.726543',1,NULL,NULL,NULL,'minhc2390@gmail.com','Cao Trần Duy Minh',NULL,_binary '',NULL,NULL,'$2a$10$HXlGa1G00wpAlU89L5GFBO9WlzfVIj44Eyd0MgiDcnifgxgWosKiy','0234566777',NULL,NULL,'2026-03-21','ACTIVE',2,3,'Cà Mau','[\"giat_ui\"]',NULL,NULL,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `work_schedules`
--

DROP TABLE IF EXISTS `work_schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by_user_id` bigint DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `end_time` time(6) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `note` varchar(500) DEFAULT NULL,
  `scheduled_end` datetime(6) DEFAULT NULL,
  `scheduled_start` datetime(6) DEFAULT NULL,
  `start_time` time(6) DEFAULT NULL,
  `status` enum('CANCELLED','CONFIRMED','SCHEDULED') NOT NULL,
  `work_date` date NOT NULL,
  `customer_id` bigint DEFAULT NULL,
  `shift_template_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ws_user_date` (`user_id`,`work_date`),
  KEY `idx_ws_status` (`status`),
  KEY `idx_ws_deleted_at` (`deleted_at`),
  KEY `FK17s84mcib9i45rfm4r1ybclj0` (`customer_id`),
  KEY `FKap0b7tjc5e0pstpmejxihgobq` (`shift_template_id`),
  CONSTRAINT `FK17s84mcib9i45rfm4r1ybclj0` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKap0b7tjc5e0pstpmejxihgobq` FOREIGN KEY (`shift_template_id`) REFERENCES `shift_templates` (`id`),
  CONSTRAINT `FKj81w5rs9r89mvwhvwm6vuqiln` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `work_schedules`
--

LOCK TABLES `work_schedules` WRITE;
/*!40000 ALTER TABLE `work_schedules` DISABLE KEYS */;
INSERT INTO `work_schedules` VALUES (1,'2026-03-20 18:51:19.213392',9999,'2026-03-20 18:51:19.213392',9999,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-21 05:00:00.000000','2026-03-21 01:00:00.000000',NULL,'SCHEDULED','2026-03-21',1,1,2),(2,'2026-03-21 17:01:09.146666',9999,'2026-03-21 17:01:09.146666',9999,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-22 05:00:00.000000','2026-03-22 01:00:00.000000',NULL,'SCHEDULED','2026-03-22',1,1,2),(3,'2026-03-22 17:27:15.036626',9999,'2026-03-22 17:27:15.036626',9999,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-23 05:00:00.000000','2026-03-23 01:00:00.000000',NULL,'SCHEDULED','2026-03-23',5,1,2),(4,'2026-03-24 03:23:24.670490',9999,'2026-03-24 03:23:24.670490',9999,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-24 05:00:00.000000','2026-03-24 01:00:00.000000',NULL,'SCHEDULED','2026-03-24',5,1,2),(5,'2026-03-25 04:56:01.232969',9999,'2026-03-25 04:56:01.232969',9999,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-25 05:00:00.000000','2026-03-25 01:00:00.000000',NULL,'SCHEDULED','2026-03-25',5,1,2),(6,'2026-03-29 03:38:09.296692',9999,'2026-03-29 03:38:09.296692',9999,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-29 05:00:00.000000','2026-03-29 01:00:00.000000',NULL,'SCHEDULED','2026-03-29',5,1,2),(7,'2026-03-30 00:39:47.964118',9999,'2026-03-30 00:39:47.964118',9999,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-30 05:00:00.000000','2026-03-30 01:00:00.000000',NULL,'SCHEDULED','2026-03-30',5,1,2);
/*!40000 ALTER TABLE `work_schedules` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-30  9:03:55
