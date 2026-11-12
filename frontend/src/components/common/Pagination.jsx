import { ChevronLeftIcon, ChevronRightIcon } from '@heroicons/react/24/outline';
import { useTranslation } from '../../i18n/I18nContext';

export default function Pagination({
  pageNumber = 0,
  totalPages = 0,
  totalElements = 0,
  pageSize = 20,
  onPageChange,
}) {
  const { t } = useTranslation();

  if (totalPages <= 1) {
    return totalElements > 0 ? (
      <div className="flex items-center justify-between px-4 py-3 text-xs text-slate-500 dark:text-slate-400 border-t border-slate-100 dark:border-zinc-800">
        <span>{t('common.totalRecords', { total: totalElements })}</span>
      </div>
    ) : null;
  }

  const startItem = pageNumber * pageSize + 1;
  const endItem = Math.min((pageNumber + 1) * pageSize, totalElements);

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 border-t border-slate-100 dark:border-zinc-800 text-xs text-slate-600 dark:text-slate-400">
      <span>{t('common.showingRecords', { start: startItem, end: endItem, total: totalElements })}</span>
      <div className="flex items-center gap-1.5">
        <button
          type="button"
          onClick={() => onPageChange(pageNumber - 1)}
          disabled={pageNumber <= 0}
          className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-zinc-800 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <ChevronLeftIcon className="h-4 w-4" />
          {t('common.previous')}
        </button>
        <span className="px-2.5 py-1 font-medium text-slate-700 dark:text-slate-200">
          {t('common.pageOf', { current: pageNumber + 1, total: totalPages })}
        </span>
        <button
          type="button"
          onClick={() => onPageChange(pageNumber + 1)}
          disabled={pageNumber + 1 >= totalPages}
          className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-zinc-800 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          {t('common.next')}
          <ChevronRightIcon className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
