# ELMS Frontend (React + Tailwind CSS)

React 18 + Vite + Tailwind CSS 3 single-page app for the Employee Leave Management System backend.

## Run
```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # production build in dist/
```
The backend URL is configured in `.env`:
```
VITE_API_URL=http://localhost:8080
```
The backend must allow your frontend origin (`app.cors.allowed-origins`, default `http://localhost:3000,http://localhost:5173`).

## Structure
```
src/
 ├── api/          client.js (axios, JWT header, 401 handling) + services.js (every endpoint)
 ├── context/      AuthContext (login / logout / session), ToastContext
 ├── components/   Layout, ProtectedRoute, Modal, Pagination, EmployeePicker, LeaveDetailModal, ui
 ├── pages/        Login, Register, Dashboard, ApplyLeave, MyLeaves, MyBalances, Profile
 │   └── admin/    AdminLeaves, AdminEmployees, AdminDepartments, AdminLeaveTypes, AdminBalances
 └── utils/        date / validation helpers
```
