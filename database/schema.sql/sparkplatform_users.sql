-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: sparkplatform
-- ------------------------------------------------------
-- Server version	8.0.35

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
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  `user_type` enum('STUDENT','TEACHER','ADMINISTRATOR') NOT NULL,
  `classroom_id` int DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `skills_json` json DEFAULT NULL,
  `profile_image` varchar(500) DEFAULT NULL,
  `status` varchar(50) NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_users_email` (`email`),
  KEY `idx_users_type` (`user_type`),
  KEY `idx_users_classroom` (`classroom_id`),
  KEY `idx_users_status` (`status`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`classroom_id`) REFERENCES `classrooms` (`classroom_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Admin Principal','admin@spark.tn','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','ADMINISTRATOR',NULL,'+21612345678',NULL,NULL,'ACTIVE','2026-02-16 21:38:29','2026-02-16 21:38:29'),(2,'Dr. Ahmed Ben Ali','ahmed.benali@spark.tn','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','TEACHER',NULL,'+21698765432','[\"Java\", \"Spring\", \"Microservices\"]',NULL,'ACTIVE','2026-02-16 21:38:29','2026-02-16 21:38:29'),(3,'Dr. Fatma Trabelsi','fatma.trabelsi@spark.tn','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','TEACHER',NULL,'+21655443322','[\"Python\", \"Machine Learning\", \"AI\"]',NULL,'ACTIVE','2026-02-16 21:38:29','2026-02-16 21:38:29'),(4,'Dr. Mohamed Sassi','mohamed.sassi@spark.tn','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','TEACHER',NULL,'+21699887766','[\"IoT\", \"Embedded Systems\", \"C\"]',NULL,'ACTIVE','2026-02-16 21:38:29','2026-02-16 21:38:29'),(5,'Louay Hamdi','louay@spark.tn','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','STUDENT',1,'+21650000001','[\"Java\", \"JavaFX\", \"Git\", \"MySQL\"]',NULL,'ACTIVE','2026-02-16 21:38:29','2026-02-16 21:38:29'),(6,'Iyed Mahjoub','iyed@spark.tn','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','STUDENT',1,'+21650000002','[\"Python\", \"AI\", \"NLP\", \"FastAPI\"]',NULL,'ACTIVE','2026-02-16 21:38:29','2026-02-16 21:38:29'),(7,'Maram Gharbi','maram@spark.tn','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','STUDENT',1,'+21650000003','[\"Java\", \"SQL\", \"Data Analysis\"]',NULL,'ACTIVE','2026-02-16 21:38:29','2026-02-16 21:38:29'),(8,'Emna Bouaziz','emna@spark.tn','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','STUDENT',1,'+21650000004','[\"Java\", \"Web Scraping\", \"REST APIs\"]',NULL,'ACTIVE','2026-02-16 21:38:29','2026-02-16 21:38:29'),(9,'Aziz Jlassi','aziz@spark.tn','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','STUDENT',1,'+21650000005','[\"Java\", \"JavaFX\", \"Scheduling\"]',NULL,'ACTIVE','2026-02-16 21:38:29','2026-02-16 21:38:29'),(10,'Sara Meddeb','sara@spark.tn','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','STUDENT',2,'+21650000006','[\"Python\", \"Data Science\"]',NULL,'ACTIVE','2026-02-16 21:38:29','2026-02-16 21:38:29'),(11,'Yassine Khelifi','yassine@spark.tn','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','STUDENT',2,'+21650000007','[\"JavaScript\", \"React\"]',NULL,'ACTIVE','2026-02-16 21:38:29','2026-02-16 21:38:29');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-16 22:39:34
