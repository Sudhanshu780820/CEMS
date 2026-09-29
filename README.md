# College Event Management System (CEMS)

A full-stack enterprise web platform engineered for colleges and universities to manage institutional events, student registrations, venue allocations, and real-time QR attendance tracking.

---

## 🏛️ System Architecture

The application is structured into two completely decoupled layers communicating over RESTful APIs:

```
┌─────────────────────────────────────────────────────────────┐
│                 Frontend (React 19 + Vite)                  │
│       Tailwind CSS v4 • Lucide Icons • Recharts • QR        │
│          Port: 3000  (Proxies /api -> localhost:8080)       │
└──────────────────────────────┬──────────────────────────────┘
                               │ JSON / REST + Bearer JWT
┌──────────────────────────────▼──────────────────────────────┐
│                Backend (Spring Boot 3.3.4)                  │
│  Spring Security 6 • Spring Data JPA • Hibernate • JJWT     │
│          Port: 8080 (Stateless JWT Authentication)          │
└──────────────────────────────┬──────────────────────────────┘
                               │ JDBC / HikariCP
┌──────────────────────────────▼──────────────────────────────┐
│                    Database (MySQL 8.0)                     │
│        event_management_db • Relational Foreign Keys        │
│          Unique Constraints • Transaction Isolation         │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 Key Features

### 1. Role-Based Access Control (RBAC)
- **Admin**:
  - Full system oversight: approve/reject student registrations, manage venues, create/assign organizers, view global event calendars, and monitor platform analytics (attendance rates, popular categories, branch participation).
- **Organizer**:
  - Event lifecycle management: submit events with venue conflict validation, manage participant rosters, initiate real-time dynamic QR attendance sessions, record manual attendance fallbacks, and export roster data in CSV format.
- **Student**:
  - Discovery & Registration: browse upcoming campus events with branch/year eligibility verification, track registered events, generate personal check-in QR codes / scan session QRs, and view complete attendance history.

### 2. Student Approval & Account Security
- Self-registered students start in `PENDING` approval status.
- Only verified college students approved by an Admin can log in and participate.
- Suspended accounts are immediately blocked at authentication.
- Dual login identifier support: students can authenticate via either **College Email** or **Enrollment ID** along with their secure BCrypt-hashed password.

### 3. Venue Allocation & Conflict Prevention
- Real-time venue collision detection checks both date and operating hours (`startTime` to `endTime`).
- Prevents overlapping event bookings in the same auditorium, lab, or seminar hall.
- Strict venue capacity caps: event registration limits cannot exceed physical venue capacity.

### 4. Concurrency-Safe Atomic Registrations
- Prevents overbooking even under high concurrency using atomic database-level capacity reservation:
  `UPDATE events SET registered_count = registered_count + 1 WHERE id = :eventId AND registered_count < max_capacity;`
- Combined with a compound unique key `(event_id, student_id)` on registrations to prevent duplicate submissions.

### 5. Dual-Mode QR Attendance Tracking
- **Organizer-Driven Dynamic QR**:
  - Organizer starts an attendance session generating a time-sensitive session token with visual QR code on the projector.
  - Students scan the QR code via their device camera or enter the alphanumeric token to instantly register attendance (`PRESENT`, `QR_SCAN`).
- **Manual Attendance Fallback**:
  - Organizers can manually mark students as `PRESENT` or `ABSENT` directly from the live participant roster.

### 6. Export & Real-Time Analytics
- Instant one-click CSV export of participant rosters with full student details, registration status, and attendance timestamps.
- Admin analytics dashboard powered by Recharts with interactive bar and pie charts showing event breakdown by category and branch engagement.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Frontend** | React 19, Vite, Tailwind CSS v4, React Router DOM v7, Axios, Lucide React, Recharts, `html5-qrcode`, `qrcode.react` |
| **Backend** | Java 21, Spring Boot 3.3.4, Spring Security 6, Spring Data JPA, Hibernate, JJWT 0.12.6, Maven |
| **Database** | MySQL 8.0, InnoDB storage engine, HikariCP connection pooling |
| **Security** | Stateless JWT (HS256), BCrypt (strength 12), Role guards, CORS configuration |

---

## 🔑 Pre-Seeded Demo Credentials

All test accounts come pre-configured out of the box via `DataInitializer.java`:

| Role | Email | Enrollment ID | Password | Notes |
|---|---|---|---|---|
| **Admin** | `admin@college.edu` | *N/A* | `Admin@123` | Full administrative privileges |
| **Organizer** | `cs.dept@college.edu` | *N/A* | `Organizer@123` | CS Department Organizer |
| **Organizer** | `robotics.club@college.edu` | *N/A* | `Organizer@123` | Robotics Club Organizer |
| **Student** | `rahul.cse@college.edu` | `EN2023CSE001` | `Student@123` | CSE, 3rd Year, Section A (Approved) |
| **Student** | `priya.it@college.edu` | `EN2023IT042` | `Student@123` | IT, 3rd Year, Section B (Approved) |
| **Student** | `arjun.aiml@college.edu` | `EN2024AIML015` | `Student@123` | AI/ML, 2nd Year, Section A (Approved) |
| **Student** | `sneha.ece@college.edu` | `EN2024ECE089` | `Student@123` | ECE, 2nd Year (Approved via Admin Test) |
| **Student** | `ananya.civ@college.edu` | `EN2025CIV012` | `Student@123` | Civil, 1st Year (Pending Approval) |
| **Student** | `vikram.me@college.edu` | `EN2022ME033` | `Student@123` | Mechanical, 4th Year (Suspended) |

> 💡 **Quick Login Tip**: The Login screen features 1-click preset buttons for Admin, Organizer, and Student roles for rapid demonstration.

---

## ⚙️ Setup & Installation

### Prerequisites
- **Java Development Kit (JDK)**: 17 or 21+
- **Apache Maven**: 3.8+
- **Node.js**: 18+ and npm 9+
- **MySQL Server**: 8.0+ running on port `3306`

### 1. Database Configuration
1. Start MySQL and ensure the credentials match `backend/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/event_management_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=root
   ```
2. The database `event_management_db` will automatically be created on first launch, and tables will be auto-generated with pre-seeded test data.

### 2. Backend Startup (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```
The backend will launch at `http://localhost:8080`.

### 3. Frontend Startup (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
The frontend will launch at `http://localhost:3000`.

---

## 📡 REST API Reference

### Authentication (`/api/auth`)
- `POST /api/auth/login`: Authenticate with `usernameOrEnrollmentId` and `password`. Returns JWT token and role profile.
- `POST /api/auth/register`: Student self-registration (creates account in `PENDING` state).
- `GET /api/auth/me`: Fetch currently authenticated user profile.

### Student Endpoints (`/api/student`, `/api/events`)
- `GET /api/events`: Browse published events with branch/year eligibility flags computed per student.
- `GET /api/events/{id}`: Detailed event information, remaining seats, and registration status.
- `POST /api/events/{id}/register`: Register for an event (atomic seat decrement).
- `DELETE /api/events/{id}/register`: Cancel existing registration.
- `GET /api/student/registrations`: View personal registrations.
- `GET /api/student/attendance`: View student attendance records.
- `POST /api/events/{id}/attendance/mark`: Check in to event by submitting dynamic session token.

### Organizer Endpoints (`/api/organizer`)
- `GET /api/organizer/dashboard`: Organizer metrics and recent events summary.
- `GET /api/organizer/events`: Events created by current organizer.
- `POST /api/organizer/events`: Create new event with venue conflict validation.
- `PUT /api/organizer/events/{id}`: Edit event details and capacity.
- `DELETE /api/organizer/events/{id}`: Cancel event and broadcast notification.
- `GET /api/organizer/events/{id}/participants`: Filterable roster of registered attendees.
- `GET /api/organizer/events/{id}/participants/export`: Download participants roster as CSV file.
- `POST /api/organizer/events/{id}/attendance/start`: Open live QR attendance session.
- `POST /api/organizer/events/{id}/attendance/stop`: Close attendance session.
- `POST /api/organizer/events/{id}/attendance/manual`: Mark individual student attendance manually.

### Admin Endpoints (`/api/admin`)
- `GET /api/admin/dashboard`: Global stats (total events, students, attendance rate, active venues).
- `GET /api/admin/reports`: Aggregated analytics data for charts.
- `GET /api/admin/students/pending`: Retrieve student accounts awaiting verification.
- `PUT /api/admin/students/{id}/status`: Approve, reject, or suspend a student.
- `GET /api/admin/organizers`: List all faculty/club organizers.
- `POST /api/admin/organizers`: Provision a new organizer account.
- `GET /api/venues`: List all campus venues and availability.
- `POST /api/venues`: Add new auditorium, seminar hall, or laboratory venue.
- `PUT /api/venues/{id}`: Update venue capacity and active state.
