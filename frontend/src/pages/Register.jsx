import { useEffect, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { CalendarDays } from 'lucide-react';
import { authApi, departmentApi } from '../api/services';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { getErrorMessage, getFieldErrors } from '../api/client';
import { Alert, Field, Spinner } from '../components/ui';
import { PASSWORD_REGEX, PHONE_REGEX, todayISO } from '../utils/format';

const INITIAL = {
  employeeCode: '', firstName: '', lastName: '', email: '', password: '', confirmPassword: '',
  phone: '', departmentId: '', designation: '', joiningDate: '',
};

function validate(f) {
  const e = {};
  if (!f.employeeCode.trim()) e.employeeCode = 'Employee code is required';
  else if (!/^[A-Za-z0-9-]+$/.test(f.employeeCode)) e.employeeCode = 'Only letters, digits and hyphens';
  if (!f.firstName.trim()) e.firstName = 'First name is required';
  if (!f.lastName.trim()) e.lastName = 'Last name is required';
  if (!f.email.trim()) e.email = 'Email is required';
  else if (!/^\S+@\S+\.\S+$/.test(f.email)) e.email = 'Please enter a valid email';
  if (!PASSWORD_REGEX.test(f.password)) e.password = '8-64 chars with upper-case, lower-case, digit and special character';
  if (f.confirmPassword !== f.password) e.confirmPassword = 'Passwords do not match';
  if (!PHONE_REGEX.test(f.phone)) e.phone = 'Phone must contain 10 to 15 digits';
  if (!f.departmentId) e.departmentId = 'Department is required';
  if (!f.designation.trim()) e.designation = 'Designation is required';
  if (!f.joiningDate) e.joiningDate = 'Joining date is required';
  return e;
}

export default function Register() {
  const { auth } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState(INITIAL);
  const [departments, setDepartments] = useState([]);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    departmentApi.list().then((list) => setDepartments(list.filter((d) => d.active))).catch((e) => setError(getErrorMessage(e)));
  }, []);

  if (auth) return <Navigate to="/" replace />;

  const handleChange = (e) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
    setErrors((er) => ({ ...er, [e.target.name]: undefined }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const clientErrors = validate(form);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length) return;

    setLoading(true);
    try {
      const { confirmPassword, ...payload } = form;
      await authApi.register({
        ...payload,
        employeeCode: payload.employeeCode.trim(),
        email: payload.email.trim(),
        departmentId: Number(payload.departmentId),
      });
      toast.success('Registration successful. Please sign in.');
      navigate('/login');
    } catch (err) {
      setError(getErrorMessage(err));
      setErrors(getFieldErrors(err));
    } finally {
      setLoading(false);
    }
  };

  const input = (name, props = {}) => (
    <input className={`input ${errors[name] ? 'input-error' : ''}`} name={name} value={form[name]} onChange={handleChange} {...props} />
  );

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-6">
      <div className="w-full max-w-3xl">
        <div className="mb-6 flex items-center justify-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600">
            <CalendarDays className="h-6 w-6 text-white" />
          </div>
          <span className="text-xl font-bold text-slate-900">LeaveManager</span>
        </div>
        <div className="card p-6 sm:p-8">
          <h1 className="text-2xl font-bold text-slate-900">Create your account</h1>
          <p className="mt-1 text-sm text-slate-500">Register as an employee. HR can adjust your role later.</p>

          <form onSubmit={handleSubmit} className="mt-6 grid gap-5 sm:grid-cols-2" noValidate>
            {error && <div className="sm:col-span-2"><Alert>{error}</Alert></div>}
            <Field label="First name" error={errors.firstName} required>{input('firstName')}</Field>
            <Field label="Last name" error={errors.lastName} required>{input('lastName')}</Field>
            <Field label="Employee code" error={errors.employeeCode} required hint="e.g. EMP1001">{input('employeeCode')}</Field>
            <Field label="Email" error={errors.email} required>{input('email', { type: 'email', autoComplete: 'username' })}</Field>
            <Field label="Password" error={errors.password} required>{input('password', { type: 'password', autoComplete: 'new-password' })}</Field>
            <Field label="Confirm password" error={errors.confirmPassword} required>{input('confirmPassword', { type: 'password', autoComplete: 'new-password' })}</Field>
            <Field label="Phone" error={errors.phone} required hint="10-15 digits, optional leading +">{input('phone', { type: 'tel' })}</Field>
            <Field label="Department" error={errors.departmentId} required>
              <select className={`input ${errors.departmentId ? 'input-error' : ''}`} name="departmentId" value={form.departmentId} onChange={handleChange}>
                <option value="">Select department</option>
                {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
              </select>
            </Field>
            <Field label="Designation" error={errors.designation} required>{input('designation')}</Field>
            <Field label="Joining date" error={errors.joiningDate} required>{input('joiningDate', { type: 'date', max: todayISO() })}</Field>

            <div className="flex items-center justify-between sm:col-span-2">
              <p className="text-sm text-slate-600">
                Already registered? <Link to="/login" className="font-medium text-brand-600 hover:text-brand-700">Sign in</Link>
              </p>
              <button className="btn-primary" disabled={loading}>
                {loading && <Spinner className="h-4 w-4 text-white" />} Create account
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
