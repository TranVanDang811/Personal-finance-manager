import { useTranslation } from '../../i18n/I18nContext';

export default function LoadingSpinner({ label }) {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col items-center justify-center py-12 gap-3 text-slate-500 dark:text-slate-400">
      <div className="h-8 w-8 rounded-full border-[3px] border-slate-200 dark:border-zinc-800 border-t-emerald-600 animate-spin" />
      <span className="text-sm font-medium">{label || t('common.loading')}</span>
    </div>
  );
}
