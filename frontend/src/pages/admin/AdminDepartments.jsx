import { useCallback, useEffect, useState } from 'react';
import { Pencil, Plus, Trash2 } from 'lucide-react';
import { departmentApi } from '../../api/services';
import { getErrorMessage, getFieldErrors } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import { ActiveBadge, Alert, Card, EmptyState, Field, PageHeader, PageLoader, Spinner } from '../../components/ui';
import Modal, { ConfirmDialog } from '../../components/Modal';
import { formatDate } from '../../utils/format';

export default function AdminDepartments() {
  const toast = useToast();
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null); // {} for new, department for edit
  const [form, setForm] = useState({ name: '', description: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(async () => {
    setError('');
    try {
      setItems(await departmentApi.list());
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);
  useEffect(() => { load(); }, [load]);

  const openForm = (dept = {}) => {
    setEditing(dept);
    setForm({ name: dept.name || '', description: dept.description || '' });
    setErrors({});
    setFormError('');
  };

  const save = async () => {
    if (!form.name.trim()) {
      setErrors({ name: 'Department name is required' });
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      const body = { name: form.name.trim(), description: form.description.trim() };
      if (editing.id) await departmentApi.update(editing.id, body);
      else await departmentApi.create(body);
      toast.success(`Department ${editing.id ? 'updated' : 'created'}.`);
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
      await departmentApi.remove(toDelete.id);
      toast.success('Department deactivated.');
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
        title="Departments"
        subtitle="Departments cannot be deactivated while they still have active employees."
        actions={<button className="btn-primary" onClick={() => openForm()}><Plus className="h-4 w-4" /> Add department</button>}
      />
      {error && <Alert onRetry={load}>{error}</Alert>}
      {!items && !error && <PageLoader />}
      {items && (
        <Card padded={false}>
          {items.length === 0 ? (
            <EmptyState title="No departments" />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-100">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="table-th">Name</th>
                    <th className="table-th">Description</th>
                    <th className="table-th">Status</th>
                    <th className="table-th">Created</th>
                    <th className="table-th text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {items.map((d) => (
                    <tr key={d.id} className="hover:bg-slate-50">
                      <td className="table-td font-medium text-slate-900">{d.name}</td>
                      <td className="table-td">{d.description || '—'}</td>
                      <td className="table-td"><ActiveBadge active={d.active} /></td>
                      <td className="table-td">{formatDate(d.createdAt)}</td>
                      <td className="table-td">
                        <div className="flex justify-end gap-1">
                          <button className="btn-ghost btn-sm" onClick={() => openForm(d)}><Pencil className="h-4 w-4" /> Edit</button>
                          {d.active && (
                            <button className="btn-ghost btn-sm text-red-600 hover:bg-red-50" onClick={() => setToDelete(d)}>
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
        title={editing?.id ? 'Edit department' : 'Add department'}
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
            <input className={`input ${errors.name ? 'input-error' : ''}`} value={form.name} maxLength={100} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </Field>
          <Field label="Description" error={errors.description}>
            <textarea rows={3} maxLength={255} className="input" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </Field>
        </div>
      </Modal>

      <ConfirmDialog
        open={!!toDelete}
        title="Deactivate department"
        message={toDelete && `Deactivate "${toDelete.name}"? Existing employees are not affected, but new employees cannot be added to it.`}
        confirmText="Deactivate"
        tone="danger"
        loading={deleting}
        onConfirm={remove}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
