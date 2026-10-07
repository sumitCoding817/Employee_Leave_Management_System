import { ChevronLeft, ChevronRight } from 'lucide-react';

export default function Pagination({ page, size, totalPages, totalElements, onPage, onSize }) {
  if (!totalElements) return null;
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  return (
    <div className="flex flex-col items-center justify-between gap-3 border-t border-slate-100 px-4 py-3 text-sm text-slate-600 sm:flex-row">
      <p>
        Showing <span className="font-medium">{from}</span>–<span className="font-medium">{to}</span> of{' '}
        <span className="font-medium">{totalElements}</span>
      </p>
      <div className="flex items-center gap-3">
        {onSize && (
          <select className="input !w-auto !py-1" value={size} onChange={(e) => onSize(Number(e.target.value))}>
            {[5, 10, 20, 50].map((n) => (
              <option key={n} value={n}>{n} / page</option>
            ))}
          </select>
        )}
        <div className="flex items-center gap-1">
          <button className="btn-secondary btn-sm" disabled={page === 0} onClick={() => onPage(page - 1)}>
            <ChevronLeft className="h-4 w-4" /> Prev
          </button>
          <span className="px-2">Page {page + 1} of {Math.max(totalPages, 1)}</span>
          <button className="btn-secondary btn-sm" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
            Next <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
