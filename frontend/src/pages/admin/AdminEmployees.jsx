import { useCallback, useEffect, useState } from 'react';
import { Pencil, Search, Trash2, UserCheck, UserX } from 'lucide-react';
import { departmentApi, employeeApi } from '../../api/services';
import { getErrorMessage, getFieldErrors } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import useDebounce from '../../hooks/useDebounce';
import { ActiveBadge, Alert, Card, EmptyState, Field, PageHeader, PageLoader, RoleBadge, Spinner } from '../../components/ui';
import Pagination from '../../components/Pagination';
import Modal, { ConfirmDialog } from '../../components/Modal';
import { PHONE_REGEX, fullName, todayISO } from '../../utils/format';

const SORTS = [
  ['firstName,asc', 'Name (A-Z)'],
  ['firstName,desc', 'Name (Z-A)'],
  ['employeeCode,asc', 'Employee code'],
  ['joiningDate,desc', 'Joined (newest)'],
  ['joiningDate,asc', 'Joined (oldest)'],
];

function EditEmployeeModal({ employee, departments, viewer, onClose, onSaved }) {
  const toast = useToast();
  const isAdmin = viewer.role === 'ADMIN';
  const isSelf = viewer.userId === employee.id;
  const [form, setForm] = useState({
    firstName: employee.firstName,
    lastName: employee.lastName,
    phone: employee.phone,
    departmentId: String(employee.departmentId),
    designation: employee.designation,
    joiningDate: employee.joiningDate,
    employeeCode: employee.employeeCode,
    email: employee.email,
    role: employee.role,
  });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const handleChange = (e) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
    setErrors((er) => ({ ...er, [e.target.name]: undefined }));
  };

  const submit = async () => {
    const v = {};
    if (!form.firstName.trim()) v.firstName = 'First name is required';
    if (!form.lastName.trim()) v.lastName = 'Last name is required';
    if (!PHONE_REGEX.test(form.phone)) v.phone = 'Phone must contain 10 to 15 digits';
    if (!form.designation.trim()) v.designation = 'Designation is required';
    if (isAdmin && !form.employeeCode.trim()) v.employeeCode = 'Employee code is required';
    if (isAdmin && !isSelf && !/^\S+@\S+\.\S+$/.test(form.email)) v.email = 'Please enter a valid email';
    setErrors(v);
    if (Object.keys(v).length) return;

    const body = {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      phone: form.phone.trim(),
      departmentId: Number(form.departmentId),
      designation: form.designation.trim(),
      joiningDate: form.joiningDate,
    };
    // Only ADMIN may change these. Your own email/role are never sent (changing them would break your session / is forbidden).
    if (isAdmin) {
      body.employeeCode = form.employeeCode.trim();
      if (!isSelf) {
        body.email = form.email.trim();
        body.role = form.role;
      }
    }

    setSaving(true);
    try {
      await employeeApi.update(employee.id, body);
      toast.success('Employee updated.');
      onSaved();
    } catch (e) {
      setError(getErrorMessage(e));
      setErrors(getFieldErrors(e));
    } finally {
      setSaving(false);
    }
  };

  const text = (name, props = {}) => (
    <input className={`input ${errors[name] ? 'input-error' : ''}`} name={name} value={form[name]} onChange={handleChange} {...props} />
  );

  return (
    <Modal
      open
      size="lg"
      title={`Edit ${fullName(employee)}`}
      onClose={onClose}
      footer={
        <>
          <button className="btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
          <button className="btn-primary" onClick={submit} disabled={saving}>{saving && <Spinner className="h-4 w-4 text-white" />} Save changes</button>
        </>
      }
    >
      <div className="grid gap-4 sm:grid-cols-2">
        {error && <div className="sm:col-span-2"><Alert>{error}</Alert></div>}
        <Field label="First name" error={errors.firstName} required>{text('firstName')}</Field>
        <Field label="Last name" error={errors.lastName} required>{text('lastName')}</Field>
        <Field label="Employee code" error={errors.employeeCode} hint={isAdmin ? undefined : 'Only ADMIN can change this'}>
          {text('employeeCode', { disabled: !isAdmin })}
        </Field>
        <Field label="Email" error={errors.email} hint={isSelf ? 'You cannot change your own email here' : !isAdmin ? 'Only ADMIN can change this' : undefined}>
          {text('email', { type: 'email', disabled: !isAdmin || isSelf })}
        </Field>
        <Field label="Phone" error={errors.phone} required>{text('phone')}</Field>
        <Field label="Designation" error={errors.designation} required>{text('designation')}</Field>
        <Field label="Department" error={errors.departmentId}>
          <select className="input" name="departmentId" value={form.departmentId} onChange={handleChange}>
            {departments.filter((d) => d.active || String(d.id) === form.departmentId).map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select>
        </Field>
        <Field label="Joining date" error={errors.joiningDate}>{text('joiningDate', { type: 'date', max: todayISO() })}</Field>
        <Field label="Role" error={errors.role} hint={!isAdmin ? 'Only ADMIN can change roles' : isSelf ? 'You cannot change your own role' : undefined}>
          <select className="input" name="role" value={form.role} onChange={handleChange} disabled={!isAdmin || isSelf}>
            <option value="EMPLOYEE">EMPLOYEE</option>
            <option value="HR">HR</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </Field>
      </div>
    </Modal>
  );
}

export default function AdminEmployees() {
  const { auth, isAdmin } = useAuth();
  const toast = useToast();
  const [keyword, setKeyword] = useState('');
  const debouncedKeyword = useDebounce(keyword, 400);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sort, setSort] = useState(SORTS[0][0]);
  const [data, setData] = useState(null);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [editing, setEditing] = useState(null);
  const [action, setAction] = useState(null); // { type: 'activate' | 'deactivate' | 'delete', employee }
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    departmentApi.list().then(setDepartments).catch(() => setDepartments([]));
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setData(await employeeApi.list({ keyword: debouncedKeyword.trim(), page, size, sort }));
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [debouncedKeyword, page, size, sort]);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { setPage(0); }, [debouncedKeyword]);

  const runAction = async () => {
    setBusy(true);
    const { type, employee } = action;
    try {
      if (type === 'activate') await employeeApi.activate(employee.id);
      if (type === 'deactivate') await employeeApi.deactivate(employee.id);
      if (type === 'delete') await employeeApi.remove(employee.id);
      toast.success(`${fullName(employee)} ${type === 'activate' ? 'activated' : type === 'deactivate' ? 'deactivated' : 'deleted (deactivated)'}.`);
      setAction(null);
      load();
    } catch (e) {
      toast.error(getErrorMessage(e));
      setAction(null);
    } finally {
      setBusy(false);
    }
  };

  const messages = {
    activate: (e) => `Re-activate ${fullName(e)}? They will be able to sign in again.`,
    deactivate: (e) => `Deactivate ${fullName(e)}? They will no longer be able to sign in.`,
    delete: (e) => `Delete ${fullName(e)}? This is a soft delete - the account is deactivated and leave history is kept.`,
  };

  // HR cannot modify ADMIN accounts, and nobody can deactivate/delete themselves
  const locked = (e) => (auth.role === 'HR' && e.role === 'ADMIN');
  const isSelf = (e) => e.id === auth.userId;

  return (
    <>
      <PageHeader title="Employees" subtitle="Search, edit and manage employee accounts." />

      <Card className="mb-6">
        <div className="grid gap-4 md:grid-cols-3">
          <div className="relative md:col-span-2">
            <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input className="input pl-9" placeholder="Search by name, email or employee code..." value={keyword} onChange={(e) => setKeyword(e.target.value)} />
          </div>
          <select className="input" value={sort} onChange={(e) => { setSort(e.target.value); setPage(0); }}>
            {SORTS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </div>
      </Card>

      {error && <Alert onRetry={load}>{error}</Alert>}
      <Card padded={false}>
        {loading && !data ? (
          <PageLoader />
        ) : !data || data.content.length === 0 ? (
          <EmptyState title="No employees found" message="Try a different search term." />
        ) : (
          <>
            <div className={`overflow-x-auto ${loading ? 'opacity-60' : ''}`}>
              <table className="min-w-full divide-y divide-slate-100">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="table-th">Employee</th>
                    <th className="table-th">Code</th>
                    <th className="table-th">Department</th>
                    <th className="table-th">Designation</th>
                    <th className="table-th">Role</th>
                    <th className="table-th">Status</th>
                    <th className="table-th text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {data.content.map((e) => (
                    <tr key={e.id} className="hover:bg-slate-50">
                      <td className="table-td">
                        <p className="font-medium text-slate-900">{fullName(e)}{isSelf(e) && <span className="ml-2 text-xs text-slate-400">(you)</span>}</p>
                        <p className="text-xs text-slate-500">{e.email}</p>
                      </td>
                      <td className="table-td">{e.employeeCode}</td>
                      <td className="table-td">{e.departmentName}</td>
                      <td className="table-td">{e.designation}</td>
                      <td className="table-td"><RoleBadge role={e.role} /></td>
                      <td className="table-td"><ActiveBadge active={e.active} /></td>
                      <td className="table-td">
                        <div className="flex justify-end gap-1">
                          <button className="btn-ghost btn-sm" disabled={locked(e)} onClick={() => setEditing(e)} title="Edit"><Pencil className="h-4 w-4" /></button>
                          {e.active ? (
                            <button className="btn-ghost btn-sm text-amber-600 hover:bg-amber-50" disabled={locked(e) || isSelf(e)} title="Deactivate"
                              onClick={() => setAction({ type: 'deactivate', employee: e })}><UserX className="h-4 w-4" /></button>
                          ) : (
                            <button className="btn-ghost btn-sm text-emerald-600 hover:bg-emerald-50" disabled={locked(e) || isSelf(e)} title="Activate"
                              onClick={() => setAction({ type: 'activate', employee: e })}><UserCheck className="h-4 w-4" /></button>
                          )}
                          {isAdmin && (
                            <button className="btn-ghost btn-sm text-red-600 hover:bg-red-50" disabled={isSelf(e)} title="Delete"
                              onClick={() => setAction({ type: 'delete', employee: e })}><Trash2 className="h-4 w-4" /></button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={data.page} size={data.size} totalPages={data.totalPages} totalElements={data.totalElements}
              onPage={setPage} onSize={(s) => { setSize(s); setPage(0); }} />
          </>
        )}
      </Card>

      {editing && (
        <EditEmployeeModal
          employee={editing}
          departments={departments}
          viewer={auth}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); load(); }}
        />
      )}

      <ConfirmDialog
        open={!!action}
        title={action ? { activate: 'Activate employee', deactivate: 'Deactivate employee', delete: 'Delete employee' }[action.type] : ''}
        message={action && messages[action.type](action.employee)}
        confirmText={action ? { activate: 'Activate', deactivate: 'Deactivate', delete: 'Delete' }[action.type] : ''}
        tone={action?.type === 'activate' ? 'success' : 'danger'}
        loading={busy}
        onConfirm={runAction}
        onCancel={() => setAction(null)}
      />
    </>
  );
}
