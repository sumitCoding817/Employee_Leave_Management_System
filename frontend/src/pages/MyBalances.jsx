import { useCallback, useEffect, useState } from 'react';
import { balanceApi } from '../api/services';
import { getErrorMessage } from '../api/client';
import { Alert, Card, EmptyState, PageHeader, PageLoader, ProgressBar } from '../components/ui';

export default function MyBalances() {
  const thisYear = new Date().getFullYear();
  const [year, setYear] = useState(thisYear);
  const [balances, setBalances] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    setBalances(null);
    try {
      setBalances(await balanceApi.mine(year));
    } catch (e) {
      setError(getErrorMessage(e));
    }
  }, [year]);
  useEffect(() => { load(); }, [load]);

  const total = (key) => (balances || []).reduce((sum, b) => sum + b[key], 0);

  return (
    <>
      <PageHeader
        title="My leave balances"
        subtitle="Balances are deducted when a request is approved."
        actions={
          <select className="input !w-auto" value={year} onChange={(e) => setYear(Number(e.target.value))}>
            {[thisYear + 1, thisYear, thisYear - 1, thisYear - 2].map((y) => <option key={y} value={y}>{y}</option>)}
          </select>
        }
      />
      {error && <Alert onRetry={load}>{error}</Alert>}
      {!balances && !error && <PageLoader />}
      {balances && (
        <Card padded={false}>
          {balances.length === 0 ? (
            <EmptyState title={`No balances for ${year}`} />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-100">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="table-th">Leave type</th>
                    <th className="table-th">Allocated</th>
                    <th className="table-th">Used</th>
                    <th className="table-th">Remaining</th>
                    <th className="table-th w-56">Usage</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {balances.map((b) => (
                    <tr key={b.id}>
                      <td className="table-td font-medium">{b.leaveTypeName}</td>
                      <td className="table-td">{b.totalAllocated}</td>
                      <td className="table-td">{b.usedDays}</td>
                      <td className="table-td font-semibold text-slate-900">{b.remainingDays}</td>
                      <td className="table-td"><ProgressBar used={b.usedDays} total={b.totalAllocated} /></td>
                    </tr>
                  ))}
                </tbody>
                <tfoot className="bg-slate-50">
                  <tr>
                    <td className="table-td font-semibold">Total</td>
                    <td className="table-td font-semibold">{total('totalAllocated')}</td>
                    <td className="table-td font-semibold">{total('usedDays')}</td>
                    <td className="table-td font-semibold">{total('remainingDays')}</td>
                    <td />
                  </tr>
                </tfoot>
              </table>
            </div>
          )}
        </Card>
      )}
    </>
  );
}
