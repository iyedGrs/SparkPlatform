# Classroom Management Application

A JavaFX-based classroom management system built with MVC architecture for managing students, courses, assignments, and classroom activities.

## Technology Stack

- **Java**: Core programming language
- **JavaFX**: UI framework for building the desktop application
- **XAMPP/MySQL**: Local database server for data persistence
- **JDBC**: Database connectivity

## Architecture

This application follows the **Model-View-Controller (MVC)** architectural pattern, which separates the application into three interconnected components:

### MVC Pattern Overview

- **Model**: Represents the data and business logic of the application
- **View**: Handles the presentation layer and user interface
- **Controller**: Acts as an intermediary between Model and View, handling user input and updating the model

### Client-Side Architecture

This is a **client-side only application** with direct database connectivity. All application logic runs on the client machine, and the application connects directly to a local or network SQL database via XAMPP. There is no separate backend server - the JavaFX application handles all business logic and communicates directly with the database.

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── classroom/
│   │           ├── models/          # Data entities
│   │           ├── controllers/     # Application controllers
│   │           ├── services/        # Business logic & database access
│   │           └── views/           # JavaFX view controllers
│   └── resources/
│       └── fxml/                    # FXML layout files
└── test/
    └── java/
        └── com/
            └── classroom/           # Test files
```

### Directory Purposes

#### `models/`
Contains Plain Old Java Objects (POJOs) that represent data entities:
- Data classes with properties, getters, and setters
- Represent database tables as Java objects
- No business logic - pure data containers
- Example: `Student.java`, `Course.java`, `Assignment.java`

#### `controllers/`
Contains application controllers that coordinate between views and services:
- Handle user interactions from the UI
- Invoke service methods to perform business operations
- Update views based on service responses
- Manage application flow and navigation
- Example: `StudentController.java`, `CourseController.java`

#### `services/`
Contains business logic and data access layer:
- Database connection management
- CRUD operations (Create, Read, Update, Delete)
- Business rules and validation logic
- DAO (Data Access Object) pattern implementation
- Example: `StudentService.java`, `DatabaseService.java`

#### `views/`
Contains JavaFX view controllers (FXML controllers):
- Classes annotated with `@FXML`
- Handle UI component initialization
- Bind UI events to controller methods
- Manage UI state and updates
- Example: `StudentViewController.java`, `DashboardViewController.java`

#### `resources/fxml/`
Contains FXML layout files:
- UI layouts designed in Scene Builder or hand-coded
- Separate presentation markup from Java logic
- Example: `student_view.fxml`, `dashboard.fxml`

## Code Placement Guidelines

When developing features, place your code in the appropriate directory:

1. **Creating a new data entity?** → Add a class to `models/`
2. **Building UI screens?** → Add FXML files to `resources/fxml/` and view controllers to `views/`
3. **Implementing business logic or database operations?** → Add classes to `services/`
4. **Coordinating between UI and services?** → Add controllers to `controllers/`
5. **Writing tests?** → Add test classes to `src/test/java/com/classroom/`

## Database Setup

1. Install XAMPP and start the MySQL service
2. Create a database for the application (e.g., `classroom_db`)
3. Configure database connection in your service layer
4. Create necessary tables for your entities

## Getting Started

1. Clone this repository
2. Set up XAMPP and create the database
3. Configure your IDE (IntelliJ IDEA, Eclipse, or NetBeans) for JavaFX development
4. Build and run the application

## Development Guidelines

- Follow Java naming conventions (PascalCase for classes, camelCase for methods/variables)
- Keep models simple - no business logic in POJOs
- Put all database operations in the services layer
- Use FXML for UI layouts when possible
- Write tests for business logic in the services layer
