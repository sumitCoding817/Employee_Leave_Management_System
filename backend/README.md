# Employee Leave Management System (Backend)

## 1. Project overview
A REST backend where employees apply for leave and HR / Admin approve or reject it.
Leave balances are deducted **only on approval**, atomically, inside one transaction.

## 2. Features
- Registration, login, logout (JWT, BCrypt, stateless Spring Security 6)
- Roles: ADMIN, HR, EMPLOYEE with URL-level and `@PreAuthorize` method-level authorization
- Employee, Department, Leave Type, Leave Balance CRUD
- Leave workflow: apply -> PENDING -> approve / reject / cancel
- Overlap detection, balance checks, weekday-based day calculation
- Admin search, pagination, sorting and leave filtering (status, employee, department, dates)
- Employee and Admin dashboards
- Swagger UI with JWT support, Spring Data auditing, CORS, SLF4J logging, idempotent seed data

## 3. Technologies
Java 21, Spring Boot 3.3, Spring Data JPA / Hibernate, MySQL 8, Spring Security 6, JJWT 0.12,
Bean Validation, Lombok, Springdoc OpenAPI 2.6, Maven, JUnit 5, Mockito, MockMvc, H2 (tests only).

## 4. Project structure
```
com.example.EmployeeLeaveManagementSystem
 ├── Controller   (thin REST controllers)
 ├── Service      (interfaces + AuthService)   └── Impl (business logic, @Transactional)
 ├── Repository   (Spring Data JPA + Specifications)
 ├── Entity       (JPA entities)
 ├── Enums        (Role, LeaveStatus)
 ├── DTO          (request / response records)
 ├── Security     (JwtService, filter, SecurityConfig, UserDetailsService, handlers)
 ├── Exception    (custom exceptions + GlobalExceptionHandler)
 ├── Config       (CORS, OpenAPI, auditing, DataSeeder, properties)
 └── Util         (mapper, date util, pageable util, current-user util)
```

## 5. Database setup
```sql
CREATE DATABASE employee_leave_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Tables are created automatically by Hibernate (`spring.jpa.hibernate.ddl-auto=update`).
For production switch to `validate` and manage schema with Flyway or Liquibase.

## 6. Configuration
Edit `src/main/resources/application.properties` or set environment variables:

| Variable | Purpose | Default |
|---|---|---|
| `DB_PASSWORD` | MySQL password | `YOUR_PASSWORD` |
| `JWT_SECRET` | JWT signing key (min 32 chars) | dev-only value, **change it** |
| `CORS_ALLOWED_ORIGINS` | Comma separated frontend URLs | `http://localhost:3000,http://localhost:5173` |
| `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` | Initial admin | `admin@gmail.com` / `Admin@123` |

Other properties: `jwt.expiration` (ms, default 86400000), `app.leave.exclude-weekends` (default `true`).

## 7. How to run
```bash
mvn clean verify          # compiles and runs all tests (H2, no MySQL needed)
mvn spring-boot:run       # needs MySQL running and the database created
```
The API starts on http://localhost:8080.

## 8. JWT authentication
1. `POST /api/auth/login` validates the email and BCrypt password and returns a signed JWT
   (claims: subject = email, `userId`, `role`, `iat`, `exp`).
2. The client sends `Authorization: Bearer <token>` on every request.
3. `JwtAuthenticationFilter` verifies the signature and expiry, rejects logged-out tokens,
   loads the user from the database and fills the SecurityContext. The role is read from the DB on every
   request, so role changes and deactivation take effect immediately.
4. No session is kept (stateless). `POST /api/auth/logout` blacklists the token until it expires
   (in-memory; use Redis if you run several instances).
5. Missing / invalid token -> 401. Authenticated but not permitted -> 403.

## 9. Roles and permissions
| Capability | EMPLOYEE | HR | ADMIN |
|---|---|---|---|
| Register / login | yes | yes | yes |
| Apply / view / cancel own leave, own balances, own profile | yes | yes | yes |
| Update own first name, last name, phone | yes | yes | yes |
| View / search / update employees, activate / deactivate | no | yes (not ADMIN accounts) | yes |
| Change employee code, email or role | no | no | yes (never your own role) |
| Delete (deactivate) employee | no | no | yes |
| Departments, leave types, balances: create / update / delete | no | yes | yes |
| Approve / reject leave, view all leaves, admin dashboard | no | yes | yes |

## 10. API endpoints
| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `/login`, `/logout` |
| Employees | `GET /api/employees`, `GET /api/employees/me`, `GET/PUT/DELETE /api/employees/{id}` |
| Departments | `POST/GET /api/departments`, `GET/PUT/DELETE /api/departments/{id}` |
| Leave types | `POST/GET /api/leave-types`, `GET/PUT/DELETE /api/leave-types/{id}` |
| Balances | `GET /api/leave-balances/my`, `GET /api/leave-balances/employee/{id}`, `POST /api/leave-balances`, `PUT /api/leave-balances/{id}` |
| Leaves | `POST /api/leaves`, `GET /api/leaves/my`, `GET /api/leaves/my/{id}`, `PUT /api/leaves/{id}/cancel`, `/approve`, `/reject` |
| Admin | `GET /api/admin/employees`, `GET/PUT /api/admin/employees/{id}`, `PUT .../{id}/activate`, `.../deactivate`, `GET /api/admin/leaves`, `/leaves/status/{status}`, `/leaves/employee/{employeeId}` |
| Dashboard | `GET /api/dashboard/employee`, `GET /api/dashboard/admin` |

Paged endpoints accept `page`, `size` (max 100) and `sort` (e.g. `sort=firstName,asc`) and return
`{ content, page, size, totalElements, totalPages }` inside `data`.
Filters: `/api/admin/leaves?status=PENDING&employeeId=1&department=IT&startDate=2026-10-01&endDate=2026-10-31`.
DELETE endpoints are soft deletes (`active=false`).

## 11. Swagger
http://localhost:8080/swagger-ui.html. Click **Authorize**, paste the JWT from `/api/auth/login`
(only the token, without `Bearer `), then **Authorize** and **Close**.

## 12. Sample login request
```json
POST /api/auth/login
{ "email": "admin@gmail.com", "password": "Admin@123" }
```

## 13. Sample leave application
```json
POST /api/leaves
{ "leaveTypeId": 1, "startDate": "2026-10-20", "endDate": "2026-10-24", "reason": "Family function" }
```

## 14. Sample approval / rejection
```
PUT /api/leaves/1/approve            (no body)
PUT /api/leaves/1/reject
{ "rejectionReason": "Project deadline is approaching." }
```

## 15. Error handling
```json
{ "success": false, "timestamp": "2026-10-05T10:30:00", "status": 404, "error": "Not Found",
  "message": "Employee not found with id: 10", "path": "/api/employees/10" }
```
Validation errors add `"errors": { "email": "Please enter a valid email" }`.
Status codes: 400 validation / business, 401 unauthenticated, 403 forbidden, 404 not found, 409 conflict, 500 unexpected.

## 16. Testing
`mvn test` runs Mockito unit tests (auth, employee, leave service) and MockMvc integration tests on H2
(registration, login, duplicate email, invalid login, validation, apply, insufficient balance, overlap,
approve, reject, cancel, role authorization, logout).
