import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Briefcase, CalendarPlus, CheckCircle2, Clock, Hourglass, Layers, UserCheck, UserX, Users, XCircle,
} from 'lucide-react';
import { balanceApi, dashboardApi } from '../api/services';
import { getErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Alert, Card, EmptyState, PageHeader, PageLoader, ProgressBar, StatCard } from '../components/ui';

function EmployeeDashboard() {
  const [stats, setStats] = useState(null);
  const [balances, setBalances] = useState([]);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      const [s, b] = await Promise.all([dashboardApi.employee(), balanceApi.mine()]);
      setStats(s);
      setBalances(b);
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);
  useEffect(() => { load(); }, [load]);

  if (error) return <Alert onRetry={load}>{error}</Alert>;
  if (!stats) return <PageLoader />;

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Leave types" value={stats.totalLeaveTypes} icon={Layers} tone="indigo" />
        <StatCard label="Total allocated" value={stats.totalAllocatedLeaves} icon={Briefcase} tone="sky" />
        <StatCard label="Used" value={stats.usedLeaves} icon={Clock} tone="amber" />
        <StatCard label="Remaining" value={stats.remainingLeaves} icon={CheckCircle2} tone="emerald" />
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label="Pending requests" value={stats.pendingRequests} icon={Hourglass} tone="amber" />
        <StatCard label="Approved requests" value={stats.approvedRequests} icon={CheckCircle2} tone="emerald" />
        <StatCard label="Rejected requests" value={stats.rejectedRequests} icon={XCircle} tone="red" />
      </div>

      <Card title={`Leave balances (${new Date().getFullYear()})`} actions={<Link to="/leaves/apply" className="btn-primary btn-sm"><CalendarPlus className="h-4 w-4" /> Apply leave</Link>}>
        {balances.length === 0 ? (
          <EmptyState title="No balances yet" />
        ) : (
          <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
            {balances.map((b) => (
              <div key={b.id} className="rounded-lg border border-slate-100 p-4">
                <div className="mb-2 flex items-baseline justify-between">
                  <p className="font-medium text-slate-900">{b.leaveTypeName}</p>
                  <p className="text-sm text-slate-500"><span className="font-semibold text-slate-900">{b.remainingDays}</span> / {b.totalAllocated} left</p>
                </div>
                <ProgressBar used={b.usedDays} total={b.totalAllocated} />
                <p className="mt-2 text-xs text-slate-500">{b.usedDays} day(s) used</p>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}

function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      setStats(await dashboardApi.admin());
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, []);
  useEffect(() => { load(); }, [load]);

  if (error) return <Alert onRetry={load}>{error}</Alert>;
  if (!stats) return <PageLoader />;

  const departments = Object.entries(stats.departmentWiseEmployeeCount || {});
  const maxCount = Math.max(1, ...departments.map(([, c]) => c));

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <StatCard label="Total employees" value={stats.totalEmployees} icon={Users} tone="indigo" />
        <StatCard label="Active employees" value={stats.activeEmployees} icon={UserCheck} tone="emerald" />
        <StatCard label="Inactive employees" value={stats.inactiveEmployees} icon={UserX} tone="red" />
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        <Link to="/admin/leaves"><StatCard label="Pending leaves" value={stats.totalPendingLeaves} icon={Hourglass} tone="amber" /></Link>
        <StatCard label="Approved leaves" value={stats.totalApprovedLeaves} icon={CheckCircle2} tone="emerald" />
        <StatCard label="Rejected leaves" value={stats.totalRejectedLeaves} icon={XCircle} tone="red" />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card title="Employees by department">
          {departments.length === 0 ? (
            <EmptyState title="No employees" />
          ) : (
            <div className="space-y-4">
              {departments.map(([name, count]) => (
                <div key={name}>
                  <div className="mb-1 flex justify-between text-sm">
                    <span className="font-medium text-slate-700">{name}</span>
                    <span className="text-slate-500">{count}</span>
                  </div>
                  <div className="h-2.5 overflow-hidden rounded-full bg-slate-100">
                    <div className="h-full rounded-full bg-brand-500" style={{ width: `${(count / maxCount) * 100}%` }} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card title="Leave statistics by type" padded={false}>
          {stats.leaveStatistics.length === 0 ? (
            <EmptyState title="No leave requests yet" />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-100">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="table-th">Type</th>
                    <th className="table-th">Pending</th>
                    <th className="table-th">Approved</th>
                    <th className="table-th">Rejected</th>
                    <th className="table-th">Cancelled</th>
                    <th className="table-th">Days taken</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {stats.leaveStatistics.map((s) => (
                    <tr key={s.leaveType}>
                      <td className="table-td font-medium">{s.leaveType}</td>
                      <td className="table-td">{s.pending}</td>
                      <td className="table-td">{s.approved}</td>
                      <td className="table-td">{s.rejected}</td>
                      <td className="table-td">{s.cancelled}</td>
                      <td className="table-td">{s.approvedDays}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}

export default function Dashboard() {
  const { auth, isStaff } = useAuth();
  const [tab, setTab] = useState('overview');

  return (
    <>
      <PageHeader
        title={`Hello, ${auth.name.split(' ')[0]} 👋`}
        subtitle={isStaff ? 'Company overview and your own leave summary.' : 'Your leave summary at a glance.'}
      />
      {isStaff && (
        <div className="mb-6 inline-flex rounded-lg bg-slate-200 p-1 text-sm font-medium">
          {[['overview', 'Company overview'], ['mine', 'My leave']].map(([key, label]) => (
            <button key={key} onClick={() => setTab(key)}
              className={`rounded-md px-4 py-1.5 transition ${tab === key ? 'bg-white text-slate-900 shadow' : 'text-slate-600 hover:text-slate-900'}`}>
              {label}
            </button>
          ))}
        </div>
      )}
      {isStaff && tab === 'overview' ? <AdminDashboard /> : <EmployeeDashboard />}
    </>
  );
}
