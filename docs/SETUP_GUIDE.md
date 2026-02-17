# 🚀 SparkPlatform — Team Setup Guide (Option B: Local MySQL)

Follow these steps **exactly** to get your dev environment running.

---

## 📋 Prerequisites

Before starting, make sure you have:

- [ ] **Java 17+** (JDK, not just JRE)
- [ ] **Maven** (or use the IntelliJ/VS Code built-in)
- [ ] **MySQL 8.0+**
- [ ] **Git**
- [ ] **IDE**: IntelliJ IDEA or VS Code with Java extensions

---

## Step 1 — Install MySQL 8.0

### Download
Go to: https://dev.mysql.com/downloads/installer/

Choose **MySQL Installer for Windows** → **Full** or **Custom** (select at minimum):
- MySQL Server 8.0
- MySQL Workbench (optional but helpful)

### During Installation
- **Root password**: Choose something simple for local dev, e.g. `root123` or `spark2026`
- **Port**: Leave as `3306`
- **Windows Service**: Yes, start on boot

### Verify Installation
Open **Command Prompt** (cmd) and run:
```bash
mysql --version
```
Expected output:
```
mysql  Ver 8.0.xx for Win64 on x86_64
```

If `mysql` is not recognized, add it to your PATH:
```
C:\Program Files\MySQL\MySQL Server 8.0\bin
```

**How to add to PATH:**
1. Search "Environment Variables" in Windows Start
2. Click "Environment Variables..."
3. Under "System variables", find `Path`, click Edit
4. Click "New" → paste: `C:\Program Files\MySQL\MySQL Server 8.0\bin`
5. Click OK → OK → OK
6. **Close and reopen** Command Prompt

---

## Step 2 — Clone the Repository

```bash
cd C:\Users\YourName\Desktop
git clone https://github.com/YOUR_TEAM/SparkPlatform.git
cd SparkPlatform
```

---

## Step 3 — Create the Database

Open Command Prompt and run:

```bash
mysql -u root -p
```

Enter your root password, then run:

```sql
-- Create the database
SOURCE C:/Users/YourName/Desktop/SparkPlatform/database/schema.sql;

-- Verify tables were created
USE sparkplatform;
SHOW TABLES;
```

You should see **18+ tables** listed.

Now load the test data:

```sql
SOURCE C:/Users/YourName/Desktop/SparkPlatform/database/seed-data.sql;

-- Verify data
SELECT * FROM users;
```

You should see the admin, teachers, and student accounts.

**⚠️ Use forward slashes `/` in the path, not backslashes `\`**

### Alternative (from Command Prompt directly):
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p sparkplatform < database/seed-data.sql
```

---

## Step 4 — Configure Database Connection

```bash
cd src\main\resources
copy db.properties.example db.properties
```

Now open `db.properties` in your editor and fill in:

```properties
db.url=jdbc:mysql://localhost:3306/sparkplatform
db.username=root
db.password=YOUR_ROOT_PASSWORD
db.driver=com.mysql.cj.jdbc.Driver
```

**⚠️ NEVER commit this file. It's already in `.gitignore`.**

---

## Step 5 — Build & Run

### With Maven (Command Line):
```bash
mvn clean install
mvn javafx:run
```

### With IntelliJ IDEA:
1. Open the project folder
2. Wait for Maven to import dependencies
3. Right-click `MainApp.java` → Run

### With VS Code:
1. Open the project folder
2. Install "Extension Pack for Java" if not installed
3. Open `MainApp.java` → Click "Run" above `main()`

---

## Step 6 — Test Login

Use these credentials from the seed data:

| Role | Email | Password |
|---|---|---|
| Admin | `admin@spark.tn` | `password123` |
| Teacher | `ahmed.benali@spark.tn` | `password123` |
| Student (Louay) | `louay@spark.tn` | `password123` |
| Student (Iyed) | `iyed@spark.tn` | `password123` |
| Student (Maram) | `maram@spark.tn` | `password123` |
| Student (Emna) | `emna@spark.tn` | `password123` |
| Student (Aziz) | `aziz@spark.tn` | `password123` |

---

## 🔄 Daily Workflow

### When you pull new changes:
```bash
git pull origin main
```

### If schema.sql changed:
```bash
# Re-run schema (WARNING: drops all data)
mysql -u root -p < database/schema.sql
mysql -u root -p sparkplatform < database/seed-data.sql
```

### Git branching strategy:
```bash
# Create your feature branch
git checkout -b feature/your-module-name

# Work on your code...
git add .
git commit -m "feat(module): description"

# Push your branch
git push origin feature/your-module-name

# Create a Pull Request on GitHub to merge into main
```

### Branch naming convention:
```
feature/ai-chat          → IYED
feature/project-board     → LOUAY
feature/grade-engine      → MARAM
feature/job-feed          → EMNA
feature/scheduler         → AZIZ
feature/auth              → whoever is doing login
```

---

## 🧩 Who Owns What

### Table Ownership (DO NOT modify tables outside your module)

| Person | Module | Your Tables | Your Packages |
|---|---|---|---|
| **IYED** | AI Transformer | `materials`, `chat_sessions`, `chat_messages` | `models/Material,ChatSession,ChatMessage` + `dao/MaterialDAO,ChatSessionDAO,ChatMessageDAO` + `services/AIService,MindMapService,QuizService,FlashcardService` + `controllers/AIChatController` |
| **LOUAY** | Project Mgmt | `projects`, `project_members`, `sprints`, `tasks`, `commits`, `meetings` | `models/Project,Sprint,Task,Commit,Meeting` + `dao/ProjectDAO,SprintDAO,TaskDAO,CommitDAO,MeetingDAO` + `services/ProjectService,GitHubService` + `controllers/ProjectBoardController` |
| **MARAM** | Classroom | `courses`, `teacher_courses`, `student_courses`, `gradebooks` | `models/Course,Gradebook` + `dao/CourseDAO,GradebookDAO` + `services/CourseService,GradeService` + `controllers/GradeController` |
| **EMNA** | Opportunities | `job_opportunities`, `applications` | `models/JobOpportunity,Application` + `dao/JobOpportunityDAO,ApplicationDAO` + `services/OpportunityService,MatchingService` + `controllers/OpportunityController` |
| **AZIZ** | Scheduler | `sessions`, `exam_cards`, `notifications` | `models/Session,ExamCard,Notification` + `dao/SessionDAO,ExamCardDAO,NotificationDAO` + `services/SchedulerService` + `controllers/SchedulerController` |

### Shared (touch carefully, coordinate):
- `models/User.java`, `models/Classroom.java`
- `dao/UserDAO.java`, `dao/ClassroomDAO.java`
- `config/DatabaseConfig.java`
- `utils/*`
- `controllers/LoginController.java`, `controllers/DashboardController.java`
- `database/schema.sql`, `database/seed-data.sql`

---

## ❓ Troubleshooting

### No MySQL installed (local demo mode)
If you don't install MySQL locally, the **Classroom Materials** module now works with an automatic in-memory fallback:

- Start app normally with `mvn javafx:run`
- If DB config/connection is unavailable, Materials CRUD uses memory storage
- You can still create, edit, and archive materials in the same UI
- Data is not persisted after app restart in this mode

### "Access denied for user 'root'"
→ Wrong password in `db.properties`. Reset MySQL root password:
```bash
# Stop MySQL
net stop MySQL80
# Start in safe mode
mysqld --skip-grant-tables --shared-memory
# In another terminal:
mysql -u root
ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';
FLUSH PRIVILEGES;
```

### "Unknown database 'sparkplatform'"
→ Run `schema.sql` first. It creates the database.

### "Table doesn't exist"
→ Schema wasn't loaded properly. Run again:
```bash
mysql -u root -p < database/schema.sql
```

### "mysql is not recognized"
→ Add MySQL to PATH (see Step 1).

### "Cannot connect to MySQL server"
→ MySQL service not running:
```bash
net start MySQL80
```

### JavaFX errors on startup
→ Make sure `pom.xml` has the JavaFX plugin and dependencies configured.

---

## 📞 Quick Reference

| What | Command |
|---|---|
| Start MySQL | `net start MySQL80` |
| Stop MySQL | `net stop MySQL80` |
| MySQL shell | `mysql -u root -p` |
| Run schema | `mysql -u root -p < database/schema.sql` |
| Run seed data | `mysql -u root -p sparkplatform < database/seed-data.sql` |
| Build project | `mvn clean install` |
| Run app | `mvn javafx:run` |
| Git pull | `git pull origin main` |
| Your branch | `git checkout -b feature/your-module` |