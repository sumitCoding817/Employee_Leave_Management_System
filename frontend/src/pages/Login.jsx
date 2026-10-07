import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { CalendarDays, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage, getFieldErrors } from '../api/client';
import { Alert, Field, Spinner } from '../components/ui';

export default function Login() {
  const { auth, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);

  if (auth) return <Navigate to="/" replace />;

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setFieldErrors({});
    setLoading(true);
    try {
      await login(form.email.trim(), form.password);
      navigate(location.state?.from?.pathname || '/', { replace: true });
    } catch (err) {
      setError(getErrorMessage(err));
      setFieldErrors(getFieldErrors(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen">
      <div className="hidden flex-1 flex-col justify-between bg-gradient-to-br from-brand-700 via-brand-600 to-indigo-500 p-12 text-white lg:flex">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/20">
            <CalendarDays className="h-6 w-6" />
          </div>
          <span className="text-xl font-bold">LeaveManager</span>
        </div>
        <div>
          <h2 className="text-4xl font-bold leading-tight">Plan time off,<br />without the paperwork.</h2>
          <p className="mt-4 max-w-md text-indigo-100">
            Apply for leave, track balances and get approvals from HR in one place.
          </p>
        </div>
        <p className="text-sm text-indigo-200">Employee Leave Management System</p>
      </div>

      <div className="flex flex-1 items-center justify-center p-6">
        <div className="w-full max-w-md">
          <h1 className="text-2xl font-bold text-slate-900">Welcome back</h1>
          <p className="mt-1 text-sm text-slate-500">Sign in to your account to continue.</p>

          <form onSubmit={handleSubmit} className="mt-8 space-y-5">
            {error && <Alert>{error}</Alert>}
            <Field label="Email" error={fieldErrors.email} required>
              <input className="input" type="email" name="email" autoComplete="username" placeholder="you@company.com"
                value={form.email} onChange={handleChange} required />
            </Field>
            <Field label="Password" error={fieldErrors.password} required>
              <div className="relative">
                <input className="input pr-10" type={showPassword ? 'text' : 'password'} name="password" autoComplete="current-password"
                  placeholder="••••••••" value={form.password} onChange={handleChange} required />
                <button type="button" className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600"
                  onClick={() => setShowPassword((v) => !v)} aria-label="Toggle password visibility">
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </Field>
            <button className="btn-primary w-full" disabled={loading}>
              {loading && <Spinner className="h-4 w-4 text-white" />} Sign in
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-600">
            New employee?{' '}
            <Link to="/register" className="font-medium text-brand-600 hover:text-brand-700">Create an account</Link>
          </p>
{/*           <div className="mt-6 rounded-lg bg-slate-100 p-3 text-xs text-slate-600"> */}
{/*             Demo admin: <span className="font-mono">admin@gmail.com</span> / <span className="font-mono">Admin@123</span> */}
{/*           </div> */}
        </div>
      </div>
    </div>
  );
}
