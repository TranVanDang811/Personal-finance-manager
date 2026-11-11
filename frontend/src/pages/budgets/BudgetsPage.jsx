import { useCallback, useEffect, useState } from 'react';
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
import { extractErrorMessage, formatCurrency } from '../../utils/formatters';

export default function BudgetsPage() {
  const dispatch = useDispatch();
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
          message: extractErrorMessage(err, 'Không thể tải ngân sách tháng.'),
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
        dispatch(showToast({ type: 'success', message: 'Cập nhật hạn mức ngân sách thành công.' }));
      } else {
        await budgetApi.createBudget({
          categoryId: values.categoryId,
          month,
          year,
          limitAmount: Number(values.limitAmount),
        });
        dispatch(showToast({ type: 'success', message: 'Thiết lập ngân sách mới thành công.' }));
      }
      setModalOpen(false);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể lưu ngân sách.'),
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
      dispatch(showToast({ type: 'success', message: 'Đã xóa ngân sách.' }));
      setDeleteTarget(null);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể xóa ngân sách.'),
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
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700 outline-none"
          >
            {Array.from({ length: 12 }, (_, idx) => idx + 1).map((m) => (
              <option key={m} value={m}>
                Tháng {m}
              </option>
            ))}
          </select>

          <select
            value={year}
            onChange={(e) => setYear(Number(e.target.value))}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700 outline-none"
          >
            {years.map((y) => (
              <option key={y} value={y}>
                Năm {y}
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
          Thiết lập ngân sách
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500">Tổng hạn mức</p>
          <p className="mt-2 text-xl font-bold text-slate-900 tabular-nums">
            {formatCurrency(summary?.totalLimitAmount, currency)}
          </p>
        </div>
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500">Đã chi tiêu</p>
          <p className="mt-2 text-xl font-bold text-rose-600 tabular-nums">
            {formatCurrency(summary?.totalSpentAmount, currency)}
          </p>
        </div>
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500">Còn lại</p>
          <p
            className={`mt-2 text-xl font-bold tabular-nums ${
              Number(summary?.totalRemainingAmount ?? 0) < 0 ? 'text-rose-600' : 'text-emerald-600'
            }`}
          >
            {formatCurrency(summary?.totalRemainingAmount, currency)}
          </p>
        </div>
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500">Tỷ lệ sử dụng</p>
          <p className="mt-2 text-xl font-bold text-slate-900 tabular-nums">
            {Number(summary?.overallUsagePercentage ?? 0).toFixed(1)}%
          </p>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner label="Đang tải ngân sách..." />
      ) : !budgets.length ? (
        <div className="card-surface">
          <EmptyState
            icon={ChartPieIcon}
            title={`Chưa có ngân sách tháng ${month}/${year}`}
            description="Đặt hạn mức chi tiêu theo từng danh mục để nhận cảnh báo sớm khi gần chạm ngưỡng."
            action={
              <button
                type="button"
                onClick={openCreateModal}
                className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 text-xs font-semibold"
              >
                <PlusIcon className="h-4 w-4" />
                Tạo ngân sách tháng {month}
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
                      <h3 className="text-sm font-bold text-slate-900 truncate">{b.categoryName}</h3>
                      <span
                        className={`inline-block mt-1 px-2 py-0.5 rounded-full text-[11px] font-medium border ${alert.badgeClass}`}
                      >
                        {alert.label}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-1 shrink-0">
                    <button
                      type="button"
                      onClick={() => openEditModal(b)}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-slate-800 hover:bg-slate-100"
                      title="Sửa hạn mức"
                    >
                      <PencilSquareIcon className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => setDeleteTarget(b)}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50"
                      title="Xóa"
                    >
                      <TrashIcon className="h-4 w-4" />
                    </button>
                  </div>
                </div>

                <div>
                  <div className="flex justify-between text-xs mb-1.5 tabular-nums">
                    <span className="text-slate-600">
                      Đã chi:{' '}
                      <strong className="text-slate-900">{formatCurrency(b.spentAmount, currency)}</strong>
                    </span>
                    <span className="text-slate-600">
                      Hạn mức:{' '}
                      <strong className="text-slate-900">{formatCurrency(b.limitAmount, currency)}</strong>
                    </span>
                  </div>
                  <div className="h-2.5 w-full rounded-full bg-slate-100 overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all ${alert.barClass}`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                  <div className="flex justify-between text-[11px] text-slate-500 mt-1.5 tabular-nums">
                    <span>Sử dụng: {Number(b.usagePercentage ?? 0).toFixed(1)}%</span>
                    <span>
                      {Number(b.remainingAmount ?? 0) >= 0 ? 'Còn lại: ' : 'Vượt mức: '}
                      <strong
                        className={
                          Number(b.remainingAmount ?? 0) >= 0 ? 'text-emerald-600' : 'text-rose-600'
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
            ? `Cập nhật hạn mức: ${editingBudget.categoryName}`
            : `Thiết lập ngân sách tháng ${month}/${year}`
        }
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {!editingBudget && (
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Danh mục chi tiêu <span className="text-rose-500">*</span>
              </label>
              <select
                {...register('categoryId', { required: 'Vui lòng chọn danh mục' })}
                className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm bg-white outline-none focus:border-emerald-500"
              >
                {expenseCategories.map((c) => (
                  <option key={c.id} value={c.id} disabled={usedCategoryIds.has(c.id)}>
                    {c.name} {usedCategoryIds.has(c.id) ? '(Đã có ngân sách)' : ''}
                  </option>
                ))}
              </select>
              {errors.categoryId && (
                <p className="text-xs text-rose-600 mt-1">{errors.categoryId.message}</p>
              )}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Hạn mức chi tiêu <span className="text-rose-500">*</span>
            </label>
            <input
              type="number"
              step="0.01"
              placeholder="Ví dụ: 5000000"
              {...register('limitAmount', {
                required: 'Vui lòng nhập hạn mức',
                min: { value: 0.01, message: 'Hạn mức phải lớn hơn 0' },
              })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm tabular-nums outline-none focus:border-emerald-500"
            />
            {errors.limitAmount && (
              <p className="text-xs text-rose-600 mt-1">{errors.limitAmount.message}</p>
            )}
          </div>

          <div className="flex justify-end gap-2.5 pt-2">
            <button
              type="button"
              onClick={() => setModalOpen(false)}
              className="px-4 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold disabled:opacity-60"
            >
              {submitting ? 'Đang lưu...' : editingBudget ? 'Cập nhật' : 'Tạo ngân sách'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        loading={deleting}
        title="Xóa ngân sách"
        message={`Bạn có chắc muốn xóa ngân sách cho danh mục "${deleteTarget?.categoryName}"?`}
        confirmLabel="Xóa"
      />
    </div>
  );
}

