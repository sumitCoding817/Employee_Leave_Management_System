import { useCallback, useEffect, useState } from 'react';
import { Check, Eye, X } from 'lucide-react';
import { departmentApi, leaveApi } from '../../api/services';
import { getErrorMessage, getFieldErrors } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import { Alert, Card, EmptyState, Field, PageHeader, PageLoader, Spinner, StatusBadge } from '../../components/ui';
import Pagination from '../../components/Pagination';
import Modal, { ConfirmDialog } from '../../components/Modal';
import LeaveDetailModal from '../../components/LeaveDetailModal';
import EmployeePicker from '../../components/EmployeePicker';
import { formatDate } from '../../utils/format';

const STATUSES = ['', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'];
const SORTS = [
  ['appliedAt,desc', 'Newest applied'],
  ['appliedAt,asc', 'Oldest applied'],
  ['startDate,desc', 'Start date (latest)'],
  ['startDate,asc', 'Start date (earliest)'],
];
const EMPTY_FILTERS = { status: 'PENDING', employee: null, department: '', startDate: '', endDate: '' };

export default function AdminLeaves() {
  const toast = useToast();
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [departments, setDepartments] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sort, setSort] = useState(SORTS[0][0]);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [detail, setDetail] = useState(null);
  const [toApprove, setToApprove] = useState(null);
  const [toReject, setToReject] = useState(null);
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    departmentApi.list().then(setDepartments).catch(() => setDepartments([]));
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setData(await leaveApi.all({
        status: filters.status,
        employeeId: filters.employee?.id,
        department: filters.department,
        startDate: filters.startDate,
        endDate: filters.endDate,
        page, size, sort,
      }));
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [filters, page, size, sort]);
  useEffect(() => { load(); }, [load]);

  const setFilter = (key, value) => {
    setFilters((f) => ({ ...f, [key]: value }));
    setPage(0);
  };

  const approve = async () => {
    setBusy(true);
    try {
      await leaveApi.approve(toApprove.id);
      toast.success(`Leave #${toApprove.id} approved. Balance updated.`);
      setToApprove(null);
      load();
    } catch (e) {
      toast.error(getErrorMessage(e));
      setToApprove(null);
      load();
    } finally {
      setBusy(false);
    }
  };

  const reject = async () => {
    if (!reason.trim()) {
      setReasonError('Rejection reason is required');
      return;
    }
    setBusy(true);
    try {
      await leaveApi.reject(toReject.id, reason.trim());
      toast.success(`Leave #${toReject.id} rejected.`);
      closeReject();
      load();
    } catch (e) {
      setReasonError(Object.values(getFieldErrors(e))[0] || getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const closeReject = () => {
    setToReject(null);
    setReason('');
    setReasonError('');
  };

  return (
    <>
      <PageHeader title="Leave requests" subtitle="Review, approve or reject employee leave requests." />

      <div className="mb-4 flex flex-wrap gap-2">
        {STATUSES.map((s) => (
          <button key={s || 'ALL'} onClick={() => setFilter('status', s)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium transition ${
              filters.status === s ? 'bg-brand-600 text-white' : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-50'
            }`}>
            {s || 'All'}
          </button>
        ))}
      </div>

      <Card className="mb-6">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
          <Field label="Employee"><EmployeePicker value={filters.employee} onChange={(v) => setFilter('employee', v)} placeholder="Any employee" /></Field>
          <Field label="Department">
            <select className="input" value={filters.department} onChange={(e) => setFilter('department', e.target.value)}>
              <option value="">All departments</option>
              {departments.map((d) => <option key={d.id} value={d.name}>{d.name}</option>)}
            </select>
          </Field>
          <Field label="From date"><input type="date" className="input" value={filters.startDate} onChange={(e) => setFilter('startDate', e.target.value)} /></Field>
          <Field label="To date"><input type="date" className="input" min={filters.startDate} value={filters.endDate} onChange={(e) => setFilter('endDate', e.target.value)} /></Field>
          <Field label="Sort by">
            <select className="input" value={sort} onChange={(e) => { setSort(e.target.value); setPage(0); }}>
              {SORTS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </select>
          </Field>
        </div>
        <div className="mt-4 flex justify-end">
          <button className="btn-secondary btn-sm" onClick={() => { setFilters({ ...EMPTY_FILTERS, status: '' }); setPage(0); }}>Reset filters</button>
        </div>
      </Card>

      {error && <Alert onRetry={load}>{error}</Alert>}
      <Card padded={false}>
        {loading && !data ? (
          <PageLoader />
        ) : !data || data.content.length === 0 ? (
          <EmptyState title="No leave requests found" message="Try changing the filters." />
        ) : (
          <>
            <div className={`overflow-x-auto ${loading ? 'opacity-60' : ''}`}>
              <table className="min-w-full divide-y divide-slate-100">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="table-th">Employee</th>
                    <th className="table-th">Department</th>
                    <th className="table-th">Type</th>
                    <th className="table-th">Dates</th>
                    <th className="table-th">Days</th>
                    <th className="table-th">Status</th>
                    <th className="table-th text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {data.content.map((l) => (
                    <tr key={l.id} className="hover:bg-slate-50">
                      <td className="table-td">
                        <p className="font-medium text-slate-900">{l.employeeName}</p>
                        <p className="text-xs text-slate-500">{l.employeeCode}</p>
                      </td>
                      <td className="table-td">{l.department}</td>
                      <td className="table-td">{l.leaveType}</td>
                      <td className="table-td whitespace-nowrap">{formatDate(l.startDate)} → {formatDate(l.endDate)}</td>
                      <td className="table-td">{l.numberOfDays}</td>
                      <td className="table-td"><StatusBadge status={l.status} /></td>
                      <td className="table-td">
                        <div className="flex justify-end gap-1">
                          <button className="btn-ghost btn-sm" onClick={() => setDetail(l)}><Eye className="h-4 w-4" /> View</button>
                          {l.status === 'PENDING' && (
                            <>
                              <button className="btn-success btn-sm" onClick={() => setToApprove(l)}><Check className="h-4 w-4" /> Approve</button>
                              <button className="btn-danger btn-sm" onClick={() => setToReject(l)}><X className="h-4 w-4" /> Reject</button>
                            </>
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

      <LeaveDetailModal leave={detail} showEmployee onClose={() => setDetail(null)} />

      <ConfirmDialog
        open={!!toApprove}
        title="Approve leave request"
        message={toApprove && `Approve ${toApprove.numberOfDays} day(s) of ${toApprove.leaveType} for ${toApprove.employeeName}? Their leave balance will be reduced.`}
        confirmText="Approve"
        tone="success"
        loading={busy}
        onConfirm={approve}
        onCancel={() => setToApprove(null)}
      />

      <Modal
        open={!!toReject}
        title="Reject leave request"
        onClose={closeReject}
        size="sm"
        footer={
          <>
            <button className="btn-secondary" onClick={closeReject} disabled={busy}>Cancel</button>
            <button className="btn-danger" onClick={reject} disabled={busy}>{busy && <Spinner className="h-4 w-4 text-white" />} Reject</button>
          </>
        }
      >
        {toReject && (
          <div className="space-y-4">
            <p className="text-sm text-slate-600">
              Rejecting <span className="font-medium">{toReject.leaveType}</span> leave for <span className="font-medium">{toReject.employeeName}</span>.
            </p>
            <Field label="Rejection reason" error={reasonError} required>
              <textarea rows={3} maxLength={500} className={`input ${reasonError ? 'input-error' : ''}`} value={reason}
                onChange={(e) => { setReason(e.target.value); setReasonError(''); }} placeholder="e.g. Project deadline is approaching." />
            </Field>
          </div>
        )}
      </Modal>
    </>
  );
}
