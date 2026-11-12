import { ExclamationTriangleIcon } from '@heroicons/react/24/outline';
import Modal from '../common/Modal';
import { useTranslation } from '../../i18n/I18nContext';

export default function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel,
  danger = true,
  loading = false,
}) {
  const { t } = useTranslation();

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title || t('common.confirmAction')} size="sm">
      <div className="flex items-start gap-3">
        <div
          className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full ${
            danger ? 'bg-rose-50 dark:bg-rose-950/60 text-rose-600 dark:text-rose-400' : 'bg-amber-50 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400'
          }`}
        >
          <ExclamationTriangleIcon className="h-5 w-5" />
        </div>
        <p className="text-sm text-slate-600 dark:text-slate-300 leading-relaxed pt-1">{message}</p>
      </div>
      <div className="mt-6 flex items-center justify-end gap-2.5">
        <button
          type="button"
          onClick={onClose}
          disabled={loading}
          className="px-4 py-2 rounded-xl border border-slate-200 dark:border-zinc-700 text-sm font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-zinc-800 disabled:opacity-50"
        >
          {t('common.cancelAction')}
        </button>
        <button
          type="button"
          onClick={onConfirm}
          disabled={loading}
          className={`px-4 py-2 rounded-xl text-sm font-medium text-white disabled:opacity-50 transition-colors ${
            danger ? 'bg-rose-600 hover:bg-rose-700' : 'bg-emerald-600 hover:bg-emerald-700'
          }`}
        >
          {loading ? t('common.processing') : confirmLabel || t('common.confirm')}
        </button>
      </div>
    </Modal>
  );
}
