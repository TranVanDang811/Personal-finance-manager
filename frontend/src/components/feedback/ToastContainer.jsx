import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  InformationCircleIcon,
  XMarkIcon,
} from '@heroicons/react/24/outline';
import { dismissToast } from '../../features/feedback/feedbackSlice';
import { useTranslation } from '../../i18n/I18nContext';

function ToastItem({ toast }) {
  const dispatch = useDispatch();
  const { t } = useTranslation();

  useEffect(() => {
    const timer = setTimeout(() => {
      dispatch(dismissToast(toast.id));
    }, 4500);
    return () => clearTimeout(timer);
  }, [dispatch, toast.id]);

  const styles = {
    success: 'bg-emerald-950 text-white border-emerald-700',
    error: 'bg-rose-950 text-white border-rose-700',
    info: 'bg-slate-900 text-white border-slate-700',
  }[toast.type] || 'bg-slate-900 text-white border-slate-700';

  const Icon = {
    success: CheckCircleIcon,
    error: ExclamationCircleIcon,
    info: InformationCircleIcon,
  }[toast.type] || InformationCircleIcon;

  return (
    <div
      className={`flex items-start gap-3 px-4 py-3 rounded-xl border shadow-lg max-w-sm w-full ${styles}`}
      role="alert"
    >
      <Icon className="h-5 w-5 shrink-0 mt-0.5 text-emerald-300" />
      <p className="text-sm font-medium flex-1 leading-snug">{toast.message}</p>
      <button
        type="button"
        onClick={() => dispatch(dismissToast(toast.id))}
        className="text-white/70 hover:text-white"
        aria-label={t('common.closeToast')}
      >
        <XMarkIcon className="h-4 w-4" />
      </button>
    </div>
  );
}

export default function ToastContainer() {
  const toasts = useSelector((state) => state.feedback.toasts);
  if (!toasts.length) return null;

  return (
    <div className="fixed bottom-5 right-5 z-[60] flex flex-col gap-2.5 max-w-sm w-full pointer-events-none px-4 sm:px-0">
      {toasts.map((toast) => (
        <div key={toast.id} className="pointer-events-auto">
          <ToastItem toast={toast} />
        </div>
      ))}
    </div>
  );
}

