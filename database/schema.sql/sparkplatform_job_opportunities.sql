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
-- Table structure for table `job_opportunities`
--

DROP TABLE IF EXISTS `job_opportunities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `job_opportunities` (
  `job_id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `company` varchar(200) DEFAULT NULL,
  `description` text,
  `location` varchar(200) DEFAULT NULL,
  `specialization` enum('SOFTWARE','DATA_SCIENCE','IOT','NETWORK','SECURITY','OTHER') DEFAULT 'OTHER',
  `type` enum('INTERNSHIP','PFE','SUMMER','JUNIOR','FREELANCE','OTHER') DEFAULT 'OTHER',
  `required_skills_json` json DEFAULT NULL,
  `salary_range` varchar(100) DEFAULT NULL,
  `deadline` date DEFAULT NULL,
  `posted_by` int DEFAULT NULL,
  `source` enum('MANUAL','ADZUNA','LINKEDIN','OTHER') DEFAULT 'MANUAL',
  `external_url` varchar(500) DEFAULT NULL,
  `status` varchar(50) NOT NULL DEFAULT 'OPEN',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`job_id`),
  KEY `posted_by` (`posted_by`),
  KEY `idx_jobs_status` (`status`),
  KEY `idx_jobs_spec` (`specialization`),
  KEY `idx_jobs_type` (`type`),
  CONSTRAINT `job_opportunities_ibfk_1` FOREIGN KEY (`posted_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job_opportunities`
--

LOCK TABLES `job_opportunities` WRITE;
/*!40000 ALTER TABLE `job_opportunities` DISABLE KEYS */;
INSERT INTO `job_opportunities` VALUES (1,'Stage PFE - Développeur Java','Sofrecom','Stage de fin d\'études en développement Java/Spring','Tunis','SOFTWARE','PFE','[\"Java\", \"Spring\", \"MySQL\"]',NULL,'2026-06-01',1,'MANUAL',NULL,'OPEN','2026-02-16 21:38:30'),(2,'Internship - Data Science','Vermeg','Summer internship in data science team','Tunis','DATA_SCIENCE','SUMMER','[\"Python\", \"Machine Learning\", \"SQL\"]',NULL,'2026-05-01',1,'MANUAL',NULL,'OPEN','2026-02-16 21:38:30'),(3,'Junior IoT Engineer','Telnet','Junior position for embedded/IoT development','Ben Arous','IOT','JUNIOR','[\"C\", \"IoT\", \"Embedded Systems\"]',NULL,'2026-07-01',1,'MANUAL',NULL,'OPEN','2026-02-16 21:38:30');
/*!40000 ALTER TABLE `job_opportunities` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-16 22:39:33
