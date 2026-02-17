# 🎓 SparkPlatform — Smart Academic Management & Pedagogical Innovation Platform

![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-Desktop-blue?style=flat-square)
![Python](https://img.shields.io/badge/Python-AI%20Engine-yellow?style=flat-square&logo=python)
![MySQL](https://img.shields.io/badge/MySQL-Database-4479A1?style=flat-square&logo=mysql&logoColor=white)

---

## 🌍 General Vision

SparkPlatform is a **smart academic ecosystem** designed to transform the university experience for engineering students. It combines **Artificial Intelligence**, **Agile project management**, **academic automation**, and **professional networking** into a unified solution.

The objective is to move from a passive academic model (file storage, static grades, rigid planning) to a **dynamic, predictive, and interactive system** focused on **performance, active understanding, and employability**.

---

## 🧩 Platform Modules

### 🧠 1️⃣ AI Knowledge Transformer

> **Concept:** An advanced academic content processing pipeline that converts static materials (PDFs, lectures, handouts) into active and intelligent learning resources. Unlike simple keyword searches, the system leverages **Vector Embeddings via HuggingFace** to understand document context.

**Key Features:**

| Feature | Description |
|---|---|
| ✅ **AI Chat (RAG System)** | Retrieval-Augmented Generation system. Simultaneous querying of multiple PDFs with contextualized, precise answers generated via the **Google Gemini API**. |
| ✅ **Contextual Linking** | Each response includes a deep link to the exact page of the source PDF, ensuring academic transparency and rapid verification. |
| ✅ **The Transformer** | Automatic content conversion into: 🗺 Mind Maps (JavaFX Canvas) · 🔊 Text-to-Speech · 📝 Dynamic Quizzes · 🧠 Smart Flashcards |

🔗 **Integrated APIs:** Google Gemini API, HuggingFace (Embeddings)

---

### 📊 2️⃣ Project Management Ecosystem

> **Concept:** An academic project management system tailored for engineering students, integrating real technical evidence (GitHub) with academic requirements (Rubrics).

**Key Features:**

| Feature | Description |
|---|---|
| ✅ **Agile Board** | Kanban interface with Sprint management adapted to 6-week PI (Program Increment) cycles. |
| ✅ **Git-Audit** | Integration with the **GitHub REST API** to visualize commits and correlate assigned tasks with actual developer activity. |
| ✅ **Meeting Manager** | Scrum meeting logs with digital signatures and traceable history. |
| ✅ **Template Engine** | Pre-loaded checklists for PI-Dev, PI-IoT, and standard academic projects. |

🔗 **Integrated API:** GitHub REST API

---

### 🏫 3️⃣ Academic Core

> **Concept:** The platform's academic engine. It replaces basic file repositories with a dynamic calculation engine capable of managing complex university weighting and coefficients.

**Key Features:**

| Feature | Description |
|---|---|
| ✅ **Material Repository** | Teacher uploads with student preview and download capabilities in a structured organization. |
| ✅ **The Grade Spot** | Instructor interface for entering CC, TP, and exams, featuring automated weighted calculation and success prediction for specific modules (UE). |
| ✅ **Student Result Cards** | Dashboard showing real-time progress, minimum grades required to pass, and "what-if" scenario simulations. |

🔗 **Integrated API:** Google Drive API

---

### 💼 4️⃣ Opportunities Gateway

> **Concept:** A smart bridge between university and industry, acting as a specialized aggregator for internships (summer/thesis) and junior engineering roles.

**Key Features:**

| Feature | Description |
|---|---|
| ✅ **Live Job Feed** | Offers filtered by specialization (Software, Data Science, IoT). |
| ✅ **User Profiles** | Role management (Student/Teacher) with full CRUD capabilities. |
| ✅ **Resume-to-Job Matcher** | Skill analysis and comparison with job descriptions to calculate a **Match %**. |

🔗 **Integrated APIs:** Adzuna API or LinkedIn Talent API

---

### 📅 5️⃣ SAMI — Smart Academic Management Interface

> **Concept:** A scheduling engine designed for the Rolling Exam model. It replaces static calendars with a dynamic system based on real-time hour tracking.

**Key Features:**

| Feature | Description |
|---|---|
| ✅ **Draggable Timetable** | JavaFX Drag & Drop interface for moving "Subject Cards" into time slots. |
| ✅ **Duration Tracking** | Automatic decrement of hour quotas (e.g., 42h) with a real-time "Hours Left" counter. |
| ✅ **Exam Trigger System** | When hours reach zero, slots automatically transform into "Exam Cards" and are added to the "Pending Exams" queue. |
| ✅ **Role Assignment** | Linking teachers to classes and students to groups. |

🔗 **Integrated API:** Twilio API (automatic SMS notifications for scheduled exams)

---

## 🚀 Global Added Value

- 🎯 Transforms academic content into an **interactive experience**.
- ⚙️ Automates complex university **administrative calculations**.
- 📋 Introduces professional **project management** into the classroom.
- 🌐 Directly connects students to the **job market**.
- 📅 Fully digitalizes **pedagogical planning**.

---

## 🏗 Architecture Overview

SparkPlatform is a **desktop-first application** — the JavaFX client connects **directly** to a shared MySQL database. There is no backend server layer.

```
┌─────────────────────────────────────────────────────────┐
│                    JAVAFX APPLICATION                     │
│  Login · Dashboard · Grades · Kanban · Scheduler · Chat │
│                                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │  Controllers │  │  Services   │  │    Models    │     │
│  │  (FXML)      │  │  (Logic)    │  │  (Entities)  │     │
│  └──────┬───────┘  └──────┬──────┘  └─────────────┘     │
│         │                 │                               │
│         │    ┌────────────┴────────────┐                 │
│         │    │     DAO / Repository     │                 │
│         │    │      (JDBC / JPA)        │                 │
│         │    └────────────┬────────────┘                 │
└─────────│────────────────│──────────────────────────────┘
          │                │
          │    ┌───────────┴───────────┐
          │    │   SHARED MYSQL DB     │
          │    │   (Cloud-hosted)      │
          │    └───────────────────────┘
          │
   ┌──────┴──────────────────┐
   │   AI ENGINE (Python)     │
   │   RAG · Embeddings ·     │
   │   Quiz · MindMap · TTS   │
   └──────────────────────────┘
```

### Technology Stack

| Layer | Technology |
|---|---|
| **Application** | JavaFX 17+ (Desktop) |
| **Database Access** | JDBC / JPA (Hibernate) |
| **Database** | MySQL 8+ (shared cloud instance) |
| **AI Microservice** | Python (FastAPI) — runs locally or shared |
| **External APIs** | GitHub, Google Gemini, HuggingFace, Twilio, Adzuna, Google Drive |

---

## 📁 Project Structure

```
SparkPlatform/
│
├── src/main/java/com/spark/platform/
│   │
│   ├── MainApp.java                  # JavaFX Application entry point
│   │
│   ├── config/
│   │   └── DatabaseConfig.java       # MySQL connection manager
│   │
│   ├── models/                       # Entity classes (POJOs)
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── Course.java
│   │   ├── Subject.java
│   │   ├── Grade.java
│   │   ├── Project.java
│   │   ├── Sprint.java
│   │   ├── Task.java
│   │   ├── Meeting.java
│   │   ├── Opportunity.java
│   │   ├── Application.java
│   │   ├── Exam.java
│   │   └── Notification.java
│   │
│   ├── dao/                          # Data Access Objects (JDBC queries)
│   │   ├── UserDAO.java
│   │   ├── CourseDAO.java
│   │   ├── GradeDAO.java
│   │   ├── ProjectDAO.java
│   │   ├── SubjectDAO.java
│   │   ├── OpportunityDAO.java
│   │   └── ExamDAO.java
│   │
│   ├── services/                     # Business logic layer
│   │   ├── AuthService.java
│   │   ├── UserService.java
│   │   ├── CourseService.java
│   │   ├── GradeService.java
│   │   ├── ProjectService.java
│   │   ├── GitHubService.java
│   │   ├── OpportunityService.java
│   │   ├── SchedulerService.java
│   │   └── AIService.java
│   │
│   ├── controllers/                  # JavaFX FXML controllers
│   │   ├── LoginController.java
│   │   ├── DashboardController.java
│   │   ├── GradeController.java
│   │   ├── ProjectBoardController.java
│   │   ├── SchedulerController.java
│   │   └── AIChatController.java
│   │
│   ├── components/                   # Reusable JavaFX components
│   │   ├── SubjectCard.java
│   │   ├── ExamCard.java
│   │   └── MindMapCanvas.java
│   │
│   └── utils/                        # Helpers & utilities
│       ├── SessionManager.java       # Current user session
│       ├── PasswordUtils.java        # Hashing
│       └── ApiClient.java            # HTTP client for external APIs
│
├── src/main/resources/
│   ├── views/                        # FXML files
│   │   ├── login.fxml
│   │   ├── dashboard.fxml
│   │   ├── grade-view.fxml
│   │   ├── kanban-board.fxml
│   │   ├── scheduler.fxml
│   │   └── ai-chat.fxml
│   ├── css/                          # Stylesheets
│   │   └── styles.css
│   ├── images/                       # Assets
│   └── db.properties                 # Database connection config
│
├── ai-engine/                        # Python AI Microservice
│   ├── app.py
│   ├── routes/
│   │   └── ai_routes.py
│   ├── services/
│   │   ├── embedding_service.py
│   │   ├── rag_service.py
│   │   ├── quiz_generator.py
│   │   ├── mindmap_generator.py
│   │   └── tts_service.py
│   ├── models/
│   │   └── vector_store.py
│   ├── utils/
│   │   ├── pdf_parser.py
│   │   └── chunking.py
│   ├── requirements.txt
│   └── config.py
│
├── database/
│   ├── schema.sql                    # Full DDL
│   └── seed-data.sql                 # Test data
│
├── docs/
├── pom.xml
└── README.md
```

---

## 🗄 Database Schema

**Core Tables:**

| Table | Purpose |
|---|---|
| `users` | Student, Teacher, Admin accounts |
| `roles` | ADMIN, TEACHER, STUDENT |
| `courses` | Course catalog |
| `subjects` | Subject definitions with hour quotas |
| `grades` | CC, TP, Exam scores with weights |
| `projects` | Academic project records |
| `sprints` | Agile sprint tracking |
| `tasks` | Kanban task items |
| `meetings` | Scrum meeting logs |
| `opportunities` | Job/internship listings |
| `applications` | Student applications |
| `exams` | Scheduled/triggered exams |
| `notifications` | SMS/in-app alerts |

---

## 🔁 Example MVC Flow — AI Chat

```
1. User sends question              → JavaFX Controller
2. AIChatController calls AIService → Service Layer
3. AIService sends HTTP request     → AI Engine (Python/FastAPI)
4. Embedding + RAG pipeline runs    → HuggingFace + Gemini
5. Response + PDF deep link returned→ AIService → Controller
6. Controller updates FXML view     → JavaFX UI
```

---

## 🔐 Role-Based Access Control

```
┌─────────┐     ┌─────────┐     ┌─────────┐
│  ADMIN  │     │ TEACHER │     │ STUDENT │
└────┬────┘     └────┬────┘     └────┬────┘
     │               │               │
     ├─ All access   ├─ Grades CRUD  ├─ View grades
     ├─ User mgmt    ├─ Materials    ├─ AI Chat
     ├─ Scheduling   ├─ Meetings     ├─ Projects
     └─ System cfg   └─ Projects     └─ Opportunities
```

**Security:** Session-based authentication managed via `SessionManager`. Password hashing with BCrypt. Role checks enforced at the service/controller layer.

---

## ⚙️ Getting Started

### Prerequisites

- **Java 17+**
- **Maven**
- **Python 3.10+**
- **MySQL 8+**

### 1. Database Setup

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed-data.sql
```

### 2. Configure Database Connection

Edit `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://YOUR_HOST:3306/sparkplatform
db.username=your_username
db.password=your_password
db.driver=com.mysql.cj.jdbc.Driver
```

### 3. Run the Application

```bash
mvn clean javafx:run
```

### 4. AI Engine (optional — for AI module)

```bash
cd ai-engine
pip install -r requirements.txt
uvicorn app:app --reload --port 8001
```

---

## 🔑 API Keys Required

| Service | Environment Variable |
|---|---|
| Google Gemini | `GEMINI_API_KEY` |
| HuggingFace | `HF_API_TOKEN` |
| GitHub | `GITHUB_TOKEN` |
| Twilio | `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` |
| Adzuna | `ADZUNA_APP_ID`, `ADZUNA_API_KEY` |
| Google Drive | `GOOGLE_DRIVE_CREDENTIALS` |

---

## 👥 Team Shared Database Setup

Since all 5 team members need to work against the **same MySQL database**, we use a **free cloud-hosted MySQL instance**.

### ☁️ Recommended: [Aiven for MySQL](https://aiven.io/) (Free Tier)

**Other free options:**

| Provider | Free Tier | Notes |
|---|---|---|
| **Aiven** | 1 DB, 1 GB | Best free MySQL hosting, no credit card |
| **Railway.app** | 1 GB, 500 hours/month | Easy setup, auto-sleep |
| **PlanetScale** | 1 DB, 1 GB (row-based) | MySQL-compatible (Vitess), branching |
| **FreeSQLDatabase.com** | 5 MB | Tiny, but instant and zero setup |
| **TiDB Cloud** | 5 GB | MySQL-compatible, generous free tier |

### Setup Steps (Aiven Example)

1. **One team member** creates the Aiven account at [https://aiven.io](https://aiven.io)
2. Create a **MySQL service** (free plan)
3. Note the connection details:
   - Host: `mysql-xxxxx.aiven.io`
   - Port: `3306` (or assigned port)
   - Database: `sparkplatform`
   - Username & Password
4. Run `schema.sql` against the cloud DB:
   ```bash
   mysql -h mysql-xxxxx.aiven.io -P 3306 -u avnadmin -p sparkplatform < database/schema.sql
   ```
5. Share the `db.properties` values with all 5 team members (via **private** channel, never commit credentials)

### ⚠️ Important Rules

- **Never commit `db.properties` to Git** — add it to `.gitignore`
- Each member copies `db.properties.example` → `db.properties` and fills in credentials
- Use a **single shared schema** — coordinate migrations via `database/schema.sql`
- If offline, each member can run a **local MySQL** for testing and sync later

### db.properties.example (commit this)

```properties
# Copy this file to db.properties and fill in your credentials
# DO NOT commit db.properties to Git
db.url=jdbc:mysql://HOST:PORT/sparkplatform
db.username=
db.password=
db.driver=com.mysql.cj.jdbc.Driver
```

---

## 👥 Team — Module Ownership

| Member | Module | Package Focus |
|---|---|---|
| Member 1 | 🧠 AI Knowledge Transformer | `ai-engine/`, `AIService`, `AIChatController` |
| Member 2 | 📊 Project Management | `ProjectDAO`, `ProjectService`, `ProjectBoardController` |
| Member 3 | 🏫 Academic Core | `GradeDAO`, `CourseDAO`, `GradeService`, `GradeController` |
| Member 4 | 💼 Opportunities Gateway | `OpportunityDAO`, `OpportunityService`, related controllers |
| Member 5 | 📅 SAMI Scheduler | `ExamDAO`, `SchedulerService`, `SchedulerController` |

**Shared responsibilities:** `models/`, `config/`, `database/schema.sql`, `LoginController`

---

## 📄 License

This project is developed for academic purposes.

---

## 📝 Implementation Notes

- Materials CRUD implementation summary: [docs/MATERIALS_CRUD.md](docs/MATERIALS_CRUD.md)
