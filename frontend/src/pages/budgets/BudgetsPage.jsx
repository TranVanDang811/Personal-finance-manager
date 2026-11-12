import { useCallback, useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useForm } from 'react-hook-form';
import { ChartPieIcon, PencilSquareIcon, PlusIcon, TrashIcon } from '@heroicons/react/24/outline';
import { budgetApi } from '../../services/budgetApi';
import { categoryApi } from '../../services/categoryApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import CategoryIcon from '../../components/common/CategoryIcon';
import EmptyState from '../../components/common/EmptyState';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/feedback/ConfirmDialog';
import LoadingSpinner from '../../components/feedback/LoadingSpinner';
import { BUDGET_ALERT_MAP } from '../../constants/enums';
import { useTranslation } from '../../i18n/I18nContext';
import { extractErrorMessage, formatCurrency } from '../../utils/formatters';

export default function BudgetsPage() {
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const tRef = useRef(t);
  tRef.current = t;

  const currency = useSelector((state) => state.auth.user?.currency || 'VND');

  const now = new Date();
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [year, setYear] = useState(now.getFullYear());
  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState(null);
  const [expenseCategories, setExpenseCategories] = useState([]);

  const [modalOpen, setModalOpen] = useState(false);
  const [editingBudget, setEditingBudget] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const {
    register,
    reset,
    handleSubmit,
    formState: { errors },
  } = useForm();

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [summaryRes, catRes] = await Promise.all([
        budgetApi.getBudgetSummary({ month, year }),
        categoryApi.getCategories('EXPENSE'),
      ]);
      setSummary(summaryRes.data || null);
      setExpenseCategories(catRes.data || []);
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, tRef.current('budgets.loadError')),
        })
      );
    } finally {
      setLoading(false);
    }
  }, [dispatch, month, year]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const budgets = summary?.budgets || [];
  const usedCategoryIds = new Set(budgets.map((b) => b.categoryId));

  const openCreateModal = () => {
    setEditingBudget(null);
    const available = expenseCategories.find((c) => !usedCategoryIds.has(c.id));
    reset({
      categoryId: available?.id || expenseCategories[0]?.id || '',
      limitAmount: '',
    });
    setModalOpen(true);
  };

  const openEditModal = (budget) => {
    setEditingBudget(budget);
    reset({
      categoryId: budget.categoryId,
      limitAmount: budget.limitAmount,
    });
    setModalOpen(true);
  };

  const onSubmit = async (values) => {
    setSubmitting(true);
    try {
      if (editingBudget) {
        await budgetApi.updateBudget(editingBudget.id, {
          limitAmount: Number(values.limitAmount),
        });
        dispatch(showToast({ type: 'success', message: t('budgets.updateSuccess') }));
      } else {
        await budgetApi.createBudget({
          categoryId: values.categoryId,
          month,
          year,
          limitAmount: Number(values.limitAmount),
        });
        dispatch(showToast({ type: 'success', message: t('budgets.createSuccess') }));
      }
      setModalOpen(false);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('budgets.saveError')),
        })
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await budgetApi.deleteBudget(deleteTarget.id);
      dispatch(showToast({ type: 'success', message: t('budgets.deleteSuccess') }));
      setDeleteTarget(null);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('budgets.deleteError')),
        })
      );
    } finally {
      setDeleting(false);
    }
  };

  const years = [now.getFullYear() - 1, now.getFullYear(), now.getFullYear() + 1];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <select
            value={month}
            onChange={(e) => setMonth(Number(e.target.value))}
            className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2 text-xs font-medium text-slate-700 dark:text-slate-200 outline-none"
          >
            {Array.from({ length: 12 }, (_, idx) => idx + 1).map((m) => (
              <option key={m} value={m}>
                {t('dashboard.monthOption', { m })}
              </option>
            ))}
          </select>

          <select
            value={year}
            onChange={(e) => setYear(Number(e.target.value))}
            className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2 text-xs font-medium text-slate-700 dark:text-slate-200 outline-none"
          >
            {years.map((y) => (
              <option key={y} value={y}>
                {t('dashboard.yearOption', { y })}
              </option>
            ))}
          </select>
        </div>

        <button
          type="button"
          onClick={openCreateModal}
          className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 text-xs font-semibold transition-colors"
        >
          <PlusIcon className="h-4 w-4" />
          {t('budgets.addBudget')}
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('budgets.totalLimit')}</p>
          <p className="mt-2 text-xl font-bold text-slate-900 dark:text-white tabular-nums">
            {formatCurrency(summary?.totalLimitAmount, currency)}
          </p>
        </div>
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('budgets.totalSpent')}</p>
          <p className="mt-2 text-xl font-bold text-rose-600 dark:text-rose-400 tabular-nums">
            {formatCurrency(summary?.totalSpentAmount, currency)}
          </p>
        </div>
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('budgets.remaining')}</p>
          <p
            className={`mt-2 text-xl font-bold tabular-nums ${
              Number(summary?.totalRemainingAmount ?? 0) < 0 ? 'text-rose-600 dark:text-rose-400' : 'text-emerald-600 dark:text-emerald-400'
            }`}
          >
            {formatCurrency(summary?.totalRemainingAmount, currency)}
          </p>
        </div>
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('budgets.usageRate')}</p>
          <p className="mt-2 text-xl font-bold text-slate-900 dark:text-white tabular-nums">
            {Number(summary?.overallUsagePercentage ?? 0).toFixed(1)}%
          </p>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner label={t('budgets.loading')} />
      ) : !budgets.length ? (
        <div className="card-surface">
          <EmptyState
            icon={ChartPieIcon}
            title={t('budgets.emptyTitle', { month, year })}
            description={t('budgets.emptyDesc')}
            action={
              <button
                type="button"
                onClick={openCreateModal}
                className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 text-xs font-semibold"
              >
                <PlusIcon className="h-4 w-4" />
                {t('budgets.createForMonth', { month })}
              </button>
            }
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {budgets.map((b) => {
            const alert = BUDGET_ALERT_MAP[b.alertLevel] || BUDGET_ALERT_MAP.NORMAL;
            const pct = Math.min(100, Number(b.usagePercentage ?? 0));
            return (
              <div key={b.id} className="card-surface p-5 space-y-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3 min-w-0">
                    <div
                      className="h-10 w-10 rounded-xl flex items-center justify-center shrink-0"
                      style={{ backgroundColor: `${b.categoryColor || '#64748B'}18` }}
                    >
                      <CategoryIcon
                        icon={b.categoryIcon}
                        color={b.categoryColor || '#64748B'}
                        className="h-5 w-5"
                      />
                    </div>
                    <div className="min-w-0">
                      <h3 className="text-sm font-bold text-slate-900 dark:text-white truncate">{b.categoryName}</h3>
                      <span
                        className={`inline-block mt-1 px-2 py-0.5 rounded-full text-[11px] font-medium border ${alert.badgeClass}`}
                      >
                        {t(alert.labelKey)}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-1 shrink-0">
                    <button
                      type="button"
                      onClick={() => openEditModal(b)}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-slate-800 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-zinc-800"
                      title={t('budgets.editLimit')}
                    >
                      <PencilSquareIcon className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => setDeleteTarget(b)}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/50"
                      title={t('common.delete')}
                    >
                      <TrashIcon className="h-4 w-4" />
                    </button>
                  </div>
                </div>

                <div>
                  <div className="flex justify-between text-xs mb-1.5 tabular-nums">
                    <span className="text-slate-600 dark:text-slate-400">
                      {t('budgets.spent')}:{' '}
                      <strong className="text-slate-900 dark:text-white">{formatCurrency(b.spentAmount, currency)}</strong>
                    </span>
                    <span className="text-slate-600 dark:text-slate-400">
                      {t('budgets.limit')}:{' '}
                      <strong className="text-slate-900 dark:text-white">{formatCurrency(b.limitAmount, currency)}</strong>
                    </span>
                  </div>
                  <div className="h-2.5 w-full rounded-full bg-slate-100 dark:bg-zinc-800 overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all ${alert.barClass}`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                  <div className="flex justify-between text-[11px] text-slate-500 dark:text-slate-400 mt-1.5 tabular-nums">
                    <span>
                      {t('budgets.usage')}: {Number(b.usagePercentage ?? 0).toFixed(1)}%
                    </span>
                    <span>
                      {Number(b.remainingAmount ?? 0) >= 0
                        ? `${t('budgets.remainingLabel')}: `
                        : `${t('budgets.exceededLabel')}: `}
                      <strong
                        className={
                          Number(b.remainingAmount ?? 0) >= 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'
                        }
                      >
                        {formatCurrency(Math.abs(Number(b.remainingAmount ?? 0)), currency)}
                      </strong>
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={
          editingBudget
            ? t('budgets.modalEditTitle', { name: editingBudget.categoryName })
            : t('budgets.modalCreateTitle', { month, year })
        }
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {!editingBudget && (
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('budgets.expenseCategory')} <span className="text-rose-500">*</span>
              </label>
              <select
                {...register('categoryId', { required: t('budgets.categoryRequired') })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 px-3 py-2.5 text-sm bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 outline-none focus:border-emerald-500"
              >
                {expenseCategories.map((c) => (
                  <option key={c.id} value={c.id} disabled={usedCategoryIds.has(c.id)}>
                    {c.name} {usedCategoryIds.has(c.id) ? t('budgets.alreadyBudgeted') : ''}
                  </option>
                ))}
              </select>
              {errors.categoryId && (
                <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.categoryId.message}</p>
              )}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('budgets.limitLabel')} <span className="text-rose-500">*</span>
            </label>
            <input
              type="number"
              step="0.01"
              placeholder={t('budgets.limitPlaceholder')}
              {...register('limitAmount', {
                required: t('budgets.limitRequired'),
                min: { value: 0.01, message: t('budgets.limitMin') },
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm tabular-nums outline-none focus:border-emerald-500"
            />
            {errors.limitAmount && (
              <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.limitAmount.message}</p>
            )}
          </div>

          <div className="flex justify-end gap-2.5 pt-2">
            <button
              type="button"
              onClick={() => setModalOpen(false)}
              className="px-4 py-2 rounded-xl border border-slate-200 dark:border-zinc-700 text-xs font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-zinc-800"
            >
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold disabled:opacity-60"
            >
              {submitting
                ? t('common.saving')
                : editingBudget
                  ? t('common.update')
                  : t('budgets.createBtn')}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        loading={deleting}
        title={t('budgets.deleteTitle')}
        message={t('budgets.deleteConfirmMsg', { name: deleteTarget?.categoryName || '' })}
        confirmLabel={t('common.delete')}
      />
    </div>
  );
}
