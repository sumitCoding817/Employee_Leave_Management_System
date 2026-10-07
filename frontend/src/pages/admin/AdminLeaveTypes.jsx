import { useCallback, useEffect, useState } from 'react';
import { Pencil, Plus, Trash2 } from 'lucide-react';
import { leaveTypeApi } from '../../api/services';
import { getErrorMessage, getFieldErrors } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import { ActiveBadge, Alert, Card, EmptyState, Field, PageHeader, PageLoader, Spinner } from '../../components/ui';
import Modal, { ConfirmDialog } from '../../components/Modal';

export default function AdminLeaveTypes() {
  const toast = useToast();
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ name: '', description: '', totalDays: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(async () => {
    setError('');
    try {
      setItems(await leaveTypeApi.list());
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);
  useEffect(() => { load(); }, [load]);

  const openForm = (item = {}) => {
    setEditing(item);
    setForm({ name: item.name || '', description: item.description || '', totalDays: item.totalDays ?? '' });
    setErrors({});
    setFormError('');
  };

  const save = async () => {
    const v = {};
    if (!form.name.trim()) v.name = 'Leave type name is required';
    const days = Number(form.totalDays);
    if (form.totalDays === '' || Number.isNaN(days)) v.totalDays = 'Total days is required';
    else if (days < 0 || days > 366) v.totalDays = 'Total days must be between 0 and 366';
    setErrors(v);
    if (Object.keys(v).length) return;

    setSaving(true);
    setFormError('');
    try {
      const body = { name: form.name.trim(), description: form.description.trim(), totalDays: days };
      if (editing.id) await leaveTypeApi.update(editing.id, body);
      else await leaveTypeApi.create(body);
      toast.success(`Leave type ${editing.id ? 'updated' : 'created'}.`);
      setEditing(null);
      load();
    } catch (e) {
      setFormError(getErrorMessage(e));
      setErrors(getFieldErrors(e));
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    setDeleting(true);
    try {
      await leaveTypeApi.remove(toDelete.id);
      toast.success('Leave type deactivated.');
      setToDelete(null);
      load();
    } catch (e) {
      toast.error(getErrorMessage(e));
      setToDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Leave types"
        subtitle="Default yearly allocation used when balances are created for employees."
        actions={<button className="btn-primary" onClick={() => openForm()}><Plus className="h-4 w-4" /> Add leave type</button>}
      />
      {error && <Alert onRetry={load}>{error}</Alert>}
      {!items && !error && <PageLoader />}
      {items && (
        <Card padded={false}>
          {items.length === 0 ? (
            <EmptyState title="No leave types" />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-100">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="table-th">Name</th>
                    <th className="table-th">Description</th>
                    <th className="table-th">Days / year</th>
                    <th className="table-th">Status</th>
                    <th className="table-th text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {items.map((t) => (
                    <tr key={t.id} className="hover:bg-slate-50">
                      <td className="table-td font-medium text-slate-900">{t.name}</td>
                      <td className="table-td">{t.description || '—'}</td>
                      <td className="table-td">{t.totalDays}</td>
                      <td className="table-td"><ActiveBadge active={t.active} /></td>
                      <td className="table-td">
                        <div className="flex justify-end gap-1">
                          <button className="btn-ghost btn-sm" onClick={() => openForm(t)}><Pencil className="h-4 w-4" /> Edit</button>
                          {t.active && (
                            <button className="btn-ghost btn-sm text-red-600 hover:bg-red-50" onClick={() => setToDelete(t)}>
                              <Trash2 className="h-4 w-4" /> Deactivate
                            </button>
                          )}
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
        title={editing?.id ? 'Edit leave type' : 'Add leave type'}
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
          <Field label="Name" error={errors.name} required>
            <input className={`input ${errors.name ? 'input-error' : ''}`} value={form.name} maxLength={50} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </Field>
          <Field label="Total days per year" error={errors.totalDays} required>
            <input type="number" min={0} max={366} className={`input ${errors.totalDays ? 'input-error' : ''}`} value={form.totalDays} onChange={(e) => setForm({ ...form, totalDays: e.target.value })} />
          </Field>
          <Field label="Description" error={errors.description}>
            <textarea rows={3} maxLength={255} className="input" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </Field>
        </div>
      </Modal>

      <ConfirmDialog
        open={!!toDelete}
        title="Deactivate leave type"
        message={toDelete && `Deactivate "${toDelete.name}"? Employees will no longer be able to apply for it. Existing requests are kept.`}
        confirmText="Deactivate"
        tone="danger"
        loading={deleting}
        onConfirm={remove}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
