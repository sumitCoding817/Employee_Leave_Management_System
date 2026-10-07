import { useEffect, useRef, useState } from 'react';
import { Search, X } from 'lucide-react';
import { employeeApi } from '../api/services';
import useDebounce from '../hooks/useDebounce';
import { fullName } from '../utils/format';

/** Search-as-you-type employee selector. value = { id, name } | null */
export default function EmployeePicker({ value, onChange, placeholder = 'Search employee by name, email or code' }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const boxRef = useRef(null);
  const debounced = useDebounce(query, 300);

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setLoading(true);
    employeeApi
      .list({ keyword: debounced, size: 8, sort: 'firstName,asc' })
      .then((page) => !cancelled && setResults(page.content))
      .catch(() => !cancelled && setResults([]))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [debounced, open]);

  useEffect(() => {
    const close = (e) => boxRef.current && !boxRef.current.contains(e.target) && setOpen(false);
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  if (value) {
    return (
      <div className="input flex items-center justify-between">
        <span className="truncate">{value.name}</span>
        <button type="button" onClick={() => onChange(null)} className="text-slate-400 hover:text-slate-600" aria-label="Clear">
          <X className="h-4 w-4" />
        </button>
      </div>
    );
  }

  return (
    <div className="relative" ref={boxRef}>
      <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
      <input
        className="input pl-9"
        placeholder={placeholder}
        value={query}
        onFocus={() => setOpen(true)}
        onChange={(e) => {
          setQuery(e.target.value);
          setOpen(true);
        }}
      />
      {open && (
        <ul className="absolute z-20 mt-1 max-h-60 w-full overflow-auto rounded-lg border border-slate-200 bg-white py-1 shadow-lg">
          {loading && <li className="px-3 py-2 text-sm text-slate-500">Searching...</li>}
          {!loading && results.length === 0 && <li className="px-3 py-2 text-sm text-slate-500">No employees found</li>}
          {results.map((e) => (
            <li key={e.id}>
              <button
                type="button"
                className="block w-full px-3 py-2 text-left text-sm hover:bg-brand-50"
                onClick={() => {
                  onChange({ id: e.id, name: `${fullName(e)} (${e.employeeCode})` });
                  setOpen(false);
                  setQuery('');
                }}
              >
                <span className="font-medium text-slate-900">{fullName(e)}</span>
                <span className="ml-2 text-xs text-slate-500">{e.employeeCode} · {e.email}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
