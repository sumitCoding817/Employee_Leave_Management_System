import { useCallback, useEffect, useState } from 'react';
import { Pencil, Plus } from 'lucide-react';
import { balanceApi, leaveTypeApi } from '../../api/services';
import { getErrorMessage, getFieldErrors } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import { Alert, Card, EmptyState, Field, PageHeader, PageLoader, ProgressBar, Spinner } from '../../components/ui';
import Modal from '../../components/Modal';
import EmployeePicker from '../../components/EmployeePicker';

export default function AdminBalances() {
  const toast = useToast();
  const thisYear = new Date().getFullYear();
  const [employee, setEmployee] = useState(null);
  const [year, setYear] = useState(thisYear);
  const [balances, setBalances] = useState(null);
  const [leaveTypes, setLeaveTypes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [editing, setEditing] = useState(null); // {} = new, balance = edit
  const [form, setForm] = useState({ leaveTypeId: '', totalAllocated: '', usedDays: '0' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    leaveTypeApi.list().then(setLeaveTypes).catch(() => setLeaveTypes([]));
  }, []);

  const load = useCallback(async () => {
    if (!employee) {
      setBalances(null);
      return;
    }
    setLoading(true);
    setError('');
    try {
      setBalances(await balanceApi.ofEmployee(employee.id, year));
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [employee, year]);
  useEffect(() => { load(); }, [load]);

  const openForm = (balance = {}) => {
    setEditing(balance);
    setForm({
      leaveTypeId: balance.leaveTypeId ? String(balance.leaveTypeId) : '',
      totalAllocated: balance.totalAllocated ?? '',
      usedDays: balance.usedDays ?? '0',
    });
    setErrors({});
    setFormError('');
  };

  const save = async () => {
    const v = {};
    const total = Number(form.totalAllocated);
    const used = Number(form.usedDays);
    if (!form.leaveTypeId) v.leaveTypeId = 'Leave type is required';
    if (form.totalAllocated === '' || total < 0) v.totalAllocated = 'Enter a valid number of days (0 or more)';
    if (form.usedDays === '' || used < 0) v.usedDays = 'Used days cannot be negative';
    else if (!v.totalAllocated && used > total) v.usedDays = 'Used days cannot exceed total allocated days';
    setErrors(v);
    if (Object.keys(v).length) return;

    setSaving(true);
    setFormError('');
    try {
      const body = {
        employeeId: employee.id,
        leaveTypeId: Number(form.leaveTypeId),
        totalAllocated: total,
        usedDays: used,
        year,
      };
      if (editing.id) await balanceApi.update(editing.id, body);
      else await balanceApi.create(body);
      toast.success(`Balance ${editing.id ? 'updated' : 'created'}.`);
      setEditing(null);
      load();
    } catch (e) {
      setFormError(getErrorMessage(e));
      setErrors(getFieldErrors(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Leave balances"
        subtitle="View and adjust the yearly leave allocation of any employee."
        actions={employee && <button className="btn-primary" onClick={() => openForm()}><Plus className="h-4 w-4" /> Add balance</button>}
      />

      <Card className="mb-6">
        <div className="grid gap-4 md:grid-cols-3">
          <Field label="Employee" className="md:col-span-2"><EmployeePicker value={employee} onChange={setEmployee} /></Field>
          <Field label="Year">
            <select className="input" value={year} onChange={(e) => setYear(Number(e.target.value))}>
              {[thisYear + 1, thisYear, thisYear - 1, thisYear - 2].map((y) => <option key={y} value={y}>{y}</option>)}
            </select>
          </Field>
        </div>
      </Card>

      {error && <Alert onRetry={load}>{error}</Alert>}
      {!employee && <Card><EmptyState title="Select an employee" message="Search for an employee above to see their balances." /></Card>}
      {employee && loading && !balances && <PageLoader />}
      {employee && balances && (
        <Card padded={false}>
          {balances.length === 0 ? (
            <EmptyState title={`No balances for ${year}`} message="Use “Add balance” to create one." />
          ) : (
            <div className={`overflow-x-auto ${loading ? 'opacity-60' : ''}`}>
              <table className="min-w-full divide-y divide-slate-100">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="table-th">Leave type</th>
                    <th className="table-th">Allocated</th>
                    <th className="table-th">Used</th>
                    <th className="table-th">Remaining</th>
                    <th className="table-th w-48">Usage</th>
                    <th className="table-th text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {balances.map((b) => (
                    <tr key={b.id} className="hover:bg-slate-50">
                      <td className="table-td font-medium text-slate-900">{b.leaveTypeName}</td>
                      <td className="table-td">{b.totalAllocated}</td>
                      <td className="table-td">{b.usedDays}</td>
                      <td className="table-td font-semibold">{b.remainingDays}</td>
                      <td className="table-td"><ProgressBar used={b.usedDays} total={b.totalAllocated} /></td>
                      <td className="table-td">
                        <div className="flex justify-end">
                          <button className="btn-ghost btn-sm" onClick={() => openForm(b)}><Pencil className="h-4 w-4" /> Edit</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      )}

      <Modal
        open={!!editing}
        title={editing?.id ? `Edit ${editing.leaveTypeName} balance` : 'Add balance'}
        onClose={() => setEditing(null)}
        size="sm"
        footer={
          <>
            <button className="btn-secondary" onClick={() => setEditing(null)} disabled={saving}>Cancel</button>
            <button className="btn-primary" onClick={save} disabled={saving}>{saving && <Spinner className="h-4 w-4 text-white" />} Save</button>
          </>
        }
      >
        <div className="space-y-4">
          {formError && <Alert>{formError}</Alert>}
          <p className="text-sm text-slate-600">{employee?.name} · {year}</p>
          <Field label="Leave type" error={errors.leaveTypeId} required>
            <select className={`input ${errors.leaveTypeId ? 'input-error' : ''}`} value={form.leaveTypeId} disabled={!!editing?.id}
              onChange={(e) => setForm({ ...form, leaveTypeId: e.target.value })}>
              <option value="">Select leave type</option>
              {leaveTypes.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Total allocated" error={errors.totalAllocated} required>
              <input type="number" min={0} className={`input ${errors.totalAllocated ? 'input-error' : ''}`} value={form.totalAllocated}
                onChange={(e) => setForm({ ...form, totalAllocated: e.target.value })} />
            </Field>
            <Field label="Used days" error={errors.usedDays}>
              <input type="number" min={0} className={`input ${errors.usedDays ? 'input-error' : ''}`} value={form.usedDays}
                onChange={(e) => setForm({ ...form, usedDays: e.target.value })} />
            </Field>
          </div>
          <p className="text-xs text-slate-500">Remaining days are calculated automatically (allocated − used).</p>
        </div>
      </Modal>
    </>
  );
}
