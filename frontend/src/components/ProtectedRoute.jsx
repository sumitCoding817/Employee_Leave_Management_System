import { Navigate, useLocation } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export function Forbidden() {
  return (
    <div className="flex flex-col items-center justify-center py-24 text-center">
      <ShieldAlert className="mb-4 h-14 w-14 text-red-400" />
      <h1 className="text-2xl font-bold text-slate-900">403 - Access denied</h1>
      <p className="mt-2 text-slate-500">You do not have permission to view this page.</p>
    </div>
  );
}

export default function ProtectedRoute({ roles, children }) {
  const { auth } = useAuth();
  const location = useLocation();
  if (!auth) return <Navigate to="/login" replace state={{ from: location }} />;
  if (roles && !roles.includes(auth.role)) return <Forbidden />;
  return children;
}
