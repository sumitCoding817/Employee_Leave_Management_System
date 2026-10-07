import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { balanceApi, leaveApi, leaveTypeApi } from '../api/services';
import { getErrorMessage, getFieldErrors } from '../api/client';
import { useToast } from '../context/ToastContext';
import { Alert, Card, Field, PageHeader, PageLoader, ProgressBar, Spinner } from '../components/ui';
import { countWeekdays, todayISO } from '../utils/format';

export default function ApplyLeave() {
  const toast = useToast();
  const navigate = useNavigate();
  const [leaveTypes, setLeaveTypes] = useState([]);
  const [balances, setBalances] = useState([]);
  const [loadError, setLoadError] = useState('');
  const [ready, setReady] = useState(false);
  const [form, setForm] = useState({ leaveTypeId: '', startDate: '', endDate: '', reason: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    Promise.all([leaveTypeApi.list(), balanceApi.mine()])
      .then(([types, bal]) => {
        setLeaveTypes(types.filter((t) => t.active));
        setBalances(bal);
      })
      .catch((e) => setLoadError(getErrorMessage(e)))
      .finally(() => setReady(true));
  }, []);

  const balance = useMemo(
    () => balances.find((b) => b.leaveTypeId === Number(form.leaveTypeId)),
    [balances, form.leaveTypeId],
  );
  const estimatedDays = countWeekdays(form.startDate, form.endDate);

  const handleChange = (e) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
    setErrors((er) => ({ ...er, [e.target.name]: undefined }));
  };

  const validate = () => {
    const e = {};
    if (!form.leaveTypeId) e.leaveTypeId = 'Leave type is required';
    if (!form.startDate) e.startDate = 'Start date is required';
    else if (form.startDate < todayISO()) e.startDate = 'Start date cannot be in the past';
    if (!form.endDate) e.endDate = 'End date is required';
    if (form.startDate && form.endDate) {
      if (form.startDate > form.endDate) e.endDate = 'End date cannot be before start date';
      else if (form.startDate.slice(0, 4) !== form.endDate.slice(0, 4)) e.endDate = 'A request cannot span two calendar years - submit separate requests';
      else if (estimatedDays === 0) e.endDate = 'The selected dates contain no working days';
    }
    if (form.reason.trim().length < 5 || form.reason.trim().length > 500) e.reason = 'Reason must be between 5 and 500 characters';
    if (!e.endDate && balance && estimatedDays > balance.remainingDays) e.endDate = `Only ${balance.remainingDays} day(s) remaining for ${balance.leaveTypeName}`;
    return e;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const v = validate();
    setErrors(v);
    if (Object.keys(v).length) return;

    setSubmitting(true);
    try {
      await leaveApi.apply({
        leaveTypeId: Number(form.leaveTypeId),
        startDate: form.startDate,
        endDate: form.endDate,
        reason: form.reason.trim(),
      });
      toast.success('Leave request submitted. It is now pending approval.');
      navigate('/leaves/my');
    } catch (err) {
      setError(getErrorMessage(err));
      setErrors(getFieldErrors(err));
    } finally {
      setSubmitting(false);
    }
  };

  if (!ready) return <PageLoader />;

  return (
    <>
      <PageHeader title="Apply for leave" subtitle="Your balance is deducted only after HR approves the request." />
      {loadError && <Alert>{loadError}</Alert>}
      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            {error && <Alert>{error}</Alert>}
            <Field label="Leave type" error={errors.leaveTypeId} required>
              <select className={`input ${errors.leaveTypeId ? 'input-error' : ''}`} name="leaveTypeId" value={form.leaveTypeId} onChange={handleChange}>
                <option value="">Select leave type</option>
                {leaveTypes.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
              </select>
            </Field>
            <div className="grid gap-5 sm:grid-cols-2">
              <Field label="Start date" error={errors.startDate} required>
                <input type="date" className={`input ${errors.startDate ? 'input-error' : ''}`} name="startDate" min={todayISO()} value={form.startDate} onChange={handleChange} />
              </Field>
              <Field label="End date" error={errors.endDate} required>
                <input type="date" className={`input ${errors.endDate ? 'input-error' : ''}`} name="endDate" min={form.startDate || todayISO()} value={form.endDate} onChange={handleChange} />
              </Field>
            </div>
            <Field label="Reason" error={errors.reason} required hint={`${form.reason.trim().length}/500`}>
              <textarea rows={4} className={`input ${errors.reason ? 'input-error' : ''}`} name="reason" maxLength={500} value={form.reason}
                onChange={handleChange} placeholder="Briefly describe why you need leave" />
            </Field>
            <div className="flex justify-end gap-2">
              <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>Cancel</button>
              <button className="btn-primary" disabled={submitting}>
                {submitting && <Spinner className="h-4 w-4 text-white" />} Submit request
              </button>
            </div>
          </form>
        </Card>

        <div className="space-y-4">
          <Card title="Summary">
            <dl className="space-y-3 text-sm">
              <div className="flex justify-between"><dt className="text-slate-500">Working days (Mon-Fri)</dt><dd className="font-semibold text-slate-900">{estimatedDays || '—'}</dd></div>
              <div className="flex justify-between"><dt className="text-slate-500">Remaining balance</dt><dd className="font-semibold text-slate-900">{balance ? balance.remainingDays : '—'}</dd></div>
              <div className="flex justify-between"><dt className="text-slate-500">Balance after approval</dt>
                <dd className={`font-semibold ${balance && balance.remainingDays - estimatedDays < 0 ? 'text-red-600' : 'text-slate-900'}`}>
                  {balance && estimatedDays ? balance.remainingDays - estimatedDays : '—'}
                </dd>
              </div>
            </dl>
            {balance && <div className="mt-4"><ProgressBar used={balance.usedDays} total={balance.totalAllocated} /></div>}
            <p className="mt-4 text-xs text-slate-500">Weekends are excluded. The server calculates the final number of days and checks for overlapping requests.</p>
          </Card>
        </div>
      </div>
    </>
  );
}
