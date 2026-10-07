import { Link, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import { STAFF_ROLES } from './context/AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import ApplyLeave from './pages/ApplyLeave';
import MyLeaves from './pages/MyLeaves';
import MyBalances from './pages/MyBalances';
import Profile from './pages/Profile';
import AdminLeaves from './pages/admin/AdminLeaves';
import AdminEmployees from './pages/admin/AdminEmployees';
import AdminDepartments from './pages/admin/AdminDepartments';
import AdminLeaveTypes from './pages/admin/AdminLeaveTypes';
import AdminBalances from './pages/admin/AdminBalances';

function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center text-center">
      <p className="text-6xl font-bold text-brand-600">404</p>
      <p className="mt-3 text-slate-500">The page you are looking for does not exist.</p>
      <Link to="/" className="btn-primary mt-6">Go to dashboard</Link>
    </div>
  );
}

const staff = (element) => <ProtectedRoute roles={STAFF_ROLES}>{element}</ProtectedRoute>;

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route index element={<Dashboard />} />
        <Route path="leaves/apply" element={<ApplyLeave />} />
        <Route path="leaves/my" element={<MyLeaves />} />
        <Route path="balances/my" element={<MyBalances />} />
        <Route path="profile" element={<Profile />} />

        <Route path="admin/leaves" element={staff(<AdminLeaves />)} />
        <Route path="admin/employees" element={staff(<AdminEmployees />)} />
        <Route path="admin/departments" element={staff(<AdminDepartments />)} />
        <Route path="admin/leave-types" element={staff(<AdminLeaveTypes />)} />
        <Route path="admin/balances" element={staff(<AdminBalances />)} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
