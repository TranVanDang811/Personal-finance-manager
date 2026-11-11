import { ChevronLeftIcon, ChevronRightIcon } from '@heroicons/react/24/outline';

export default function Pagination({ pageNumber = 0, totalPages = 0, totalElements = 0, pageSize = 20, onPageChange }) {
  if (totalPages <= 1) {
    return totalElements > 0 ? (
      <div className="flex items-center justify-between px-4 py-3 text-xs text-slate-500 border-t border-slate-100">
        <span>Tổng cộng {totalElements} bản ghi</span>
      </div>
    ) : null;
  }

  const startItem = pageNumber * pageSize + 1;
  const endItem = Math.min((pageNumber + 1) * pageSize, totalElements);

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 border-t border-slate-100 text-xs text-slate-600">
      <span>
        Hiển thị <strong className="text-slate-900">{startItem}–{endItem}</strong> trong{' '}
        <strong className="text-slate-900">{totalElements}</strong> bản ghi
      </span>
      <div className="flex items-center gap-1.5">
        <button
          type="button"
          onClick={() => onPageChange(pageNumber - 1)}
          disabled={pageNumber <= 0}
          className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <ChevronLeftIcon className="h-4 w-4" />
          Trước
        </button>
        <span className="px-2.5 py-1 font-medium text-slate-700">
          Trang {pageNumber + 1} / {totalPages}
        </span>
        <button
          type="button"
          onClick={() => onPageChange(pageNumber + 1)}
          disabled={pageNumber + 1 >= totalPages}
          className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          Sau
          <ChevronRightIcon className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

