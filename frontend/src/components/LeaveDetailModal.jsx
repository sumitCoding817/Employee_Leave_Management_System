import Modal from './Modal';
import { StatusBadge } from './ui';
import { formatDate, formatDateTime } from '../utils/format';

function Row({ label, children }) {
  return (
    <div className="grid grid-cols-3 gap-3 border-b border-slate-100 py-2 text-sm last:border-0">
      <dt className="text-slate-500">{label}</dt>
      <dd className="col-span-2 break-words text-slate-900">{children || '—'}</dd>
    </div>
  );
}

export default function LeaveDetailModal({ leave, onClose, showEmployee = false }) {
  return (
    <Modal open={!!leave} title={`Leave request #${leave?.id ?? ''}`} onClose={onClose} footer={<button className="btn-secondary" onClick={onClose}>Close</button>}>
      {leave && (
        <dl>
          {showEmployee && <Row label="Employee">{`${leave.employeeName} (${leave.employeeCode})`}</Row>}
          {showEmployee && <Row label="Department">{leave.department}</Row>}
          <Row label="Leave type">{leave.leaveType}</Row>
          <Row label="Status"><StatusBadge status={leave.status} /></Row>
          <Row label="From">{formatDate(leave.startDate)}</Row>
          <Row label="To">{formatDate(leave.endDate)}</Row>
          <Row label="Working days">{leave.numberOfDays}</Row>
          <Row label="Reason">{leave.reason}</Row>
          <Row label="Applied at">{formatDateTime(leave.appliedAt)}</Row>
          {leave.approvedAt && <Row label="Approved at">{formatDateTime(leave.approvedAt)}</Row>}
          {leave.approvedByName && <Row label="Approved by">{leave.approvedByName}</Row>}
          {leave.rejectedAt && <Row label="Rejected at">{formatDateTime(leave.rejectedAt)}</Row>}
          {leave.rejectionReason && <Row label="Rejection reason">{leave.rejectionReason}</Row>}
        </dl>
      )}
    </Modal>
  );
}
