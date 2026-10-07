# Employee Leave Management System - Full Stack

| Part | Tech | Folder | Default URL |
|---|---|---|---|
| Backend REST API | Java 21, Spring Boot 3.3, Spring Security 6 + JWT, JPA/Hibernate, MySQL | `backend/` | http://localhost:8080 |
| Frontend SPA | React 18, Vite, Tailwind CSS 3, React Router 6, Axios | `frontend/` | http://localhost:5173 |
| API tests | Postman collection (211 requests, 530+ assertions) | `postman/` | |

## 1. Prerequisites
JDK 21, Maven 3.9+, MySQL 8, Node.js 18+ (npm).

## 2. Start the backend
```sql
CREATE DATABASE employee_leave_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
```bash
cd backend
export DB_PASSWORD=your_mysql_password                       # Windows PowerShell: $env:DB_PASSWORD="..."
export JWT_SECRET=replace-with-a-long-random-secret-at-least-32-chars
mvn spring-boot:run
```
Tables, departments, leave types and the admin user are created automatically on first start.
Swagger UI: http://localhost:8080/swagger-ui.html

## 3. Start the frontend
```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:5173 and sign in:

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@gmail.com | Admin@123 |
| EMPLOYEE | register at /register | your own |

To create an HR user: register a normal account, then sign in as ADMIN > Employees > Edit > Role = HR.

## 4. How frontend and backend are connected
* `frontend/.env` -> `VITE_API_URL` points at the backend (default `http://localhost:8080`).
* `src/api/client.js` is an Axios instance that adds `Authorization: Bearer <JWT>` to every request and signs the user out on `401`.
* Login stores the JWT in `localStorage`; `AuthContext` restores it on refresh and drops it when expired.
* CORS: the backend allows `http://localhost:3000` and `http://localhost:5173` (`app.cors.allowed-origins` / env `CORS_ALLOWED_ORIGINS`). For another frontend URL:
  `export CORS_ALLOWED_ORIGINS=https://leave.mycompany.com`
* Role based UI: employees see their own pages; ADMIN/HR also get the Administration menu. The backend enforces the same rules, so the UI can never grant extra access.

## 5. Screens and the API they use
| Screen | Who | Endpoints |
|---|---|---|
| Login / Register | everyone | `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/departments` |
| Dashboard (employee) | all | `GET /api/dashboard/employee`, `GET /api/leave-balances/my` |
| Dashboard (company overview) | ADMIN, HR | `GET /api/dashboard/admin` |
| Apply Leave | all | `GET /api/leave-types`, `GET /api/leave-balances/my`, `POST /api/leaves` |
| My Leaves (view / cancel) | all | `GET /api/leaves/my`, `PUT /api/leaves/{id}/cancel` |
| My Balances | all | `GET /api/leave-balances/my?year=` |
| My Profile | all | `GET /api/employees/me`, `PUT /api/employees/{id}` |
| Leave Requests (approve / reject / filter) | ADMIN, HR | `GET /api/admin/leaves`, `PUT /api/leaves/{id}/approve`, `PUT /api/leaves/{id}/reject` |
| Employees (search / edit / activate / deactivate / delete) | ADMIN, HR (delete: ADMIN) | `GET /api/admin/employees`, `PUT /api/employees/{id}`, `PUT /api/admin/employees/{id}/activate`, `.../deactivate`, `DELETE /api/employees/{id}` |
| Departments | ADMIN, HR | `GET/POST /api/departments`, `PUT/DELETE /api/departments/{id}` |
| Leave Types | ADMIN, HR | `GET/POST /api/leave-types`, `PUT/DELETE /api/leave-types/{id}` |
| Leave Balances | ADMIN, HR | `GET /api/leave-balances/employee/{id}`, `POST /api/leave-balances`, `PUT /api/leave-balances/{id}` |

## 6. Business rules visible in the UI
* A new request is always PENDING; the balance is deducted only when HR/Admin approves.
* Weekends are excluded from the day count; requests cannot overlap another PENDING/APPROVED request, cannot start in the past and cannot span two calendar years.
* Employees can cancel PENDING requests, and APPROVED requests that have not started yet (balance is restored).
* HR cannot modify ADMIN accounts; only ADMIN can change employee code, email or role; nobody can deactivate or delete their own account.

## 7. Postman
Import `postman/ELMS.postman_collection.json` (+ environment), then run the whole collection in order with the Collection Runner.

## 8. Troubleshooting
| Problem | Fix |
|---|---|
| "Cannot reach the server" in the UI | Backend not running, or `VITE_API_URL` is wrong. Restart `npm run dev` after editing `.env`. |
| CORS error in the browser console | Add your frontend URL to `CORS_ALLOWED_ORIGINS` and restart the backend. |
| Backend fails to start with `Access denied for user` | Set `DB_PASSWORD` (or edit `spring.datasource.password`). |
| `jwt.secret must be at least 32 characters` | Set `JWT_SECRET` to a longer value. |
| Lombok errors in the IDE | Enable annotation processing. |
