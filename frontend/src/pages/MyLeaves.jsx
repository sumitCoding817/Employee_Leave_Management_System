import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { CalendarPlus, Eye, XCircle } from 'lucide-react';
import { leaveApi } from '../api/services';
import { getErrorMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import { Alert, Card, EmptyState, PageHeader, PageLoader, StatusBadge } from '../components/ui';
import Pagination from '../components/Pagination';
import { ConfirmDialog } from '../components/Modal';
import LeaveDetailModal from '../components/LeaveDetailModal';
import { formatDate, formatDateTime, todayISO } from '../utils/format';

const SORTS = [
  ['appliedAt,desc', 'Newest applied'],
  ['appliedAt,asc', 'Oldest applied'],
  ['startDate,desc', 'Start date (latest)'],
  ['startDate,asc', 'Start date (earliest)'],
];

/** PENDING can always be cancelled; APPROVED only before it starts (the server enforces this too). */
const canCancel = (l) => l.status === 'PENDING' || (l.status === 'APPROVED' && l.startDate > todayISO());

export default function MyLeaves() {
  const toast = useToast();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sort, setSort] = useState(SORTS[0][0]);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState(null);
  const [toCancel, setToCancel] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setData(await leaveApi.mine({ page, size, sort }));
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [page, size, sort]);
  useEffect(() => { load(); }, [load]);

  const confirmCancel = async () => {
    setCancelling(true);
    try {
      await leaveApi.cancel(toCancel.id);
      toast.success('Leave request cancelled.');
      setToCancel(null);
      load();
    } catch (e) {
      toast.error(getErrorMessage(e));
      setToCancel(null);
    } finally {
      setCancelling(false);
    }
  };

  return (
    <>
      <PageHeader
        title="My leaves"
        subtitle="Track the status of every request you have submitted."
        actions={
          <>
            <select className="input !w-auto" value={sort} onChange={(e) => { setSort(e.target.value); setPage(0); }}>
              {SORTS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </select>
            <Link to="/leaves/apply" className="btn-primary"><CalendarPlus className="h-4 w-4" /> Apply leave</Link>
          </>
        }
      />
      {error && <Alert onRetry={load}>{error}</Alert>}
      <Card padded={false}>
        {loading && !data ? (
          <PageLoader />
        ) : !data || data.content.length === 0 ? (
          <EmptyState title="No leave requests" message="Apply for leave to see it here." />
        ) : (
          <>
            <div className={`overflow-x-auto ${loading ? 'opacity-60' : ''}`}>
              <table className="min-w-full divide-y divide-slate-100">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="table-th">Type</th>
                    <th className="table-th">Dates</th>
                    <th className="table-th">Days</th>
                    <th className="table-th">Reason</th>
                    <th className="table-th">Applied</th>
                    <th className="table-th">Status</th>
                    <th className="table-th text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {data.content.map((l) => (
                    <tr key={l.id} className="hover:bg-slate-50">
                      <td className="table-td font-medium">{l.leaveType}</td>
                      <td className="table-td whitespace-nowrap">{formatDate(l.startDate)} → {formatDate(l.endDate)}</td>
                      <td className="table-td">{l.numberOfDays}</td>
                      <td className="table-td max-w-[220px] truncate" title={l.reason}>{l.reason}</td>
                      <td className="table-td whitespace-nowrap">{formatDateTime(l.appliedAt)}</td>
                      <td className="table-td"><StatusBadge status={l.status} /></td>
                      <td className="table-td">
                        <div className="flex justify-end gap-1">
                          <button className="btn-ghost btn-sm" onClick={() => setDetail(l)}><Eye className="h-4 w-4" /> View</button>
                          {canCancel(l) && (
                            <button className="btn-ghost btn-sm text-red-600 hover:bg-red-50" onClick={() => setToCancel(l)}>
                              <XCircle className="h-4 w-4" /> Cancel
                            </button>
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

      <LeaveDetailModal leave={detail} onClose={() => setDetail(null)} />
      <ConfirmDialog
        open={!!toCancel}
        title="Cancel leave request"
        message={toCancel?.status === 'APPROVED'
          ? 'This leave is already approved. Cancelling it will restore your leave balance. Continue?'
          : 'Are you sure you want to cancel this pending leave request?'}
        confirmText="Yes, cancel it"
        tone="danger"
        loading={cancelling}
        onConfirm={confirmCancel}
        onCancel={() => setToCancel(null)}
      />
    </>
  );
}
