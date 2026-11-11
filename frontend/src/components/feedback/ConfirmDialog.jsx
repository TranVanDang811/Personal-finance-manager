import { ExclamationTriangleIcon } from '@heroicons/react/24/outline';
import Modal from '../common/Modal';

export default function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title = 'Xác nhận thao tác',
  message,
  confirmLabel = 'Xác nhận',
  danger = true,
  loading = false,
}) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} size="sm">
      <div className="flex items-start gap-3">
        <div
          className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full ${
            danger ? 'bg-rose-50 text-rose-600' : 'bg-amber-50 text-amber-600'
          }`}
        >
          <ExclamationTriangleIcon className="h-5 w-5" />
        </div>
        <p className="text-sm text-slate-600 leading-relaxed pt-1">{message}</p>
      </div>
      <div className="mt-6 flex items-center justify-end gap-2.5">
        <button
          type="button"
          onClick={onClose}
          disabled={loading}
          className="px-4 py-2 rounded-xl border border-slate-200 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
        >
          Hủy bỏ
        </button>
        <button
          type="button"
          onClick={onConfirm}
          disabled={loading}
          className={`px-4 py-2 rounded-xl text-sm font-medium text-white disabled:opacity-50 transition-colors ${
            danger ? 'bg-rose-600 hover:bg-rose-700' : 'bg-emerald-600 hover:bg-emerald-700'
          }`}
        >
          {loading ? 'Đang xử lý...' : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}

