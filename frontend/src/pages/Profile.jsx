import { useEffect, useState } from 'react';
import { employeeApi } from '../api/services';
import { getErrorMessage, getFieldErrors } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { ActiveBadge, Alert, Card, Field, PageHeader, PageLoader, RoleBadge, Spinner } from '../components/ui';
import { PHONE_REGEX, formatDate, formatDateTime } from '../utils/format';

function ReadOnly({ label, children }) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <div className="mt-1 text-sm text-slate-900">{children || '—'}</div>
    </div>
  );
}

export default function Profile() {
  const { updateUser } = useAuth();
  const toast = useToast();
  const [me, setMe] = useState(null);
  const [form, setForm] = useState({ firstName: '', lastName: '', phone: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const fill = (e) => {
    setMe(e);
    setForm({ firstName: e.firstName, lastName: e.lastName, phone: e.phone });
  };

  useEffect(() => {
    employeeApi.me().then(fill).catch((e) => setError(getErrorMessage(e)));
  }, []);

  const handleChange = (e) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
    setErrors((er) => ({ ...er, [e.target.name]: undefined }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const v = {};
    if (!form.firstName.trim()) v.firstName = 'First name is required';
    if (!form.lastName.trim()) v.lastName = 'Last name is required';
    if (!PHONE_REGEX.test(form.phone)) v.phone = 'Phone must contain 10 to 15 digits';
    setErrors(v);
    if (Object.keys(v).length) return;

    setSaving(true);
    try {
      const updated = await employeeApi.update(me.id, {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        phone: form.phone.trim(),
      });
      fill(updated);
      updateUser({ name: `${updated.firstName} ${updated.lastName}` });
      toast.success('Profile updated.');
    } catch (err) {
      setError(getErrorMessage(err));
      setErrors(getFieldErrors(err));
    } finally {
      setSaving(false);
    }
  };

  if (!me && !error) return <PageLoader />;
  if (!me) return <Alert>{error}</Alert>;

  return (
    <>
      <PageHeader title="My profile" subtitle="You can update your name and phone number. Other fields are managed by HR." />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Employment details" className="lg:col-span-1">
          <div className="space-y-4">
            <ReadOnly label="Employee code">{me.employeeCode}</ReadOnly>
            <ReadOnly label="Email">{me.email}</ReadOnly>
            <ReadOnly label="Department">{me.departmentName}</ReadOnly>
            <ReadOnly label="Designation">{me.designation}</ReadOnly>
            <ReadOnly label="Joining date">{formatDate(me.joiningDate)}</ReadOnly>
            <ReadOnly label="Role"><RoleBadge role={me.role} /></ReadOnly>
            <ReadOnly label="Status"><ActiveBadge active={me.active} /></ReadOnly>
            <ReadOnly label="Last updated">{formatDateTime(me.updatedAt || me.createdAt)}</ReadOnly>
          </div>
        </Card>

        <Card title="Edit contact details" className="lg:col-span-2">
          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            {error && <Alert>{error}</Alert>}
            <div className="grid gap-5 sm:grid-cols-2">
              <Field label="First name" error={errors.firstName} required>
                <input className={`input ${errors.firstName ? 'input-error' : ''}`} name="firstName" value={form.firstName} onChange={handleChange} />
              </Field>
              <Field label="Last name" error={errors.lastName} required>
                <input className={`input ${errors.lastName ? 'input-error' : ''}`} name="lastName" value={form.lastName} onChange={handleChange} />
              </Field>
            </div>
            <Field label="Phone" error={errors.phone} required hint="10-15 digits, optional leading +">
              <input className={`input ${errors.phone ? 'input-error' : ''}`} name="phone" value={form.phone} onChange={handleChange} />
            </Field>
            <div className="flex justify-end">
              <button className="btn-primary" disabled={saving}>
                {saving && <Spinner className="h-4 w-4 text-white" />} Save changes
              </button>
            </div>
          </form>
        </Card>
      </div>
    </>
  );
}
