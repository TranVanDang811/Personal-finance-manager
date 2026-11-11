import { useCallback, useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useForm } from 'react-hook-form';
import {
  ArrowPathIcon,
  NoSymbolIcon,
  PauseIcon,
  PencilSquareIcon,
  PlayIcon,
  PlusIcon,
  TrashIcon,
} from '@heroicons/react/24/outline';
import { recurringApi } from '../../services/recurringApi';
import { accountApi } from '../../services/accountApi';
import { categoryApi } from '../../services/categoryApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import CategoryIcon from '../../components/common/CategoryIcon';
import EmptyState from '../../components/common/EmptyState';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/feedback/ConfirmDialog';
import LoadingSpinner from '../../components/feedback/LoadingSpinner';
import {
  RECURRING_FREQUENCIES,
  RECURRING_FREQUENCY_LABELS,
  RECURRING_STATUS_MAP,
  TRANSACTION_TYPE_MAP,
  TRANSACTION_TYPES,
} from '../../constants/enums';
import {
  extractErrorMessage,
  formatCurrency,
  formatDate,
  formatDateTime,
  toDateInputValue,
} from '../../utils/formatters';

export default function RecurringTransactionsPage() {
  const dispatch = useDispatch();
  const currency = useSelector((state) => state.auth.user?.currency || 'VND');

  const [loading, setLoading] = useState(true);
  const [items, setItems] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [executeTarget, setExecuteTarget] = useState(null);
  const [allowEarly, setAllowEarly] = useState(false);
  const [executing, setExecuting] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [processingConfirm, setProcessingConfirm] = useState(false);

  const {
    register,
    watch,
    setValue,
    reset,
    handleSubmit,
    formState: { errors },
  } = useForm();

  const watchedType = watch('transactionType');
  const watchedAccountId = watch('accountId');
  const watchedStartDate = watch('startDate');

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [recRes, accRes, catRes] = await Promise.all([
        recurringApi.getRecurringTransactions(statusFilter || undefined),
        accountApi.getAccounts({ status: 'ACTIVE' }),
        categoryApi.getCategories(),
      ]);
      setItems(recRes.data || []);
      setAccounts(accRes.data || []);
      setCategories(catRes.data || []);
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể tải danh sách giao dịch định kỳ.'),
        })
      );
    } finally {
      setLoading(false);
    }
  }, [dispatch, statusFilter]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const openCreateModal = () => {
    setEditingItem(null);
    const defaultExpCat = categories.find((c) => c.categoryType === 'EXPENSE')?.id || '';
    reset({
      transactionType: 'EXPENSE',
      accountId: accounts[0]?.id || '',
      toAccountId: '',
      categoryId: defaultExpCat,
      amount: '',
      description: '',
      frequency: 'MONTHLY',
      startDate: toDateInputValue(new Date()),
      endDate: '',
    });
    setModalOpen(true);
  };

  const openEditModal = (item) => {
    setEditingItem(item);
    reset({
      transactionType: item.transactionType,
      accountId: item.accountId,
      toAccountId: item.toAccountId || '',
      categoryId: item.categoryId || '',
      amount: item.amount,
      description: item.description,
      frequency: item.frequency,
      startDate: item.startDate ? toDateInputValue(item.startDate) : toDateInputValue(new Date()),
      endDate: item.endDate ? toDateInputValue(item.endDate) : '',
    });
    setModalOpen(true);
  };

  const handleTypeChange = (nextType) => {
    setValue('transactionType', nextType);
    if (nextType === 'TRANSFER') {
      setValue('categoryId', '');
    } else {
      setValue('toAccountId', '');
      const match = categories.find((c) => c.categoryType === nextType);
      setValue('categoryId', match?.id || '');
    }
  };

  const onSubmit = async (values) => {
    setSubmitting(true);
    try {
      const payload = {
        accountId: values.accountId,
        toAccountId: values.transactionType === 'TRANSFER' ? values.toAccountId : null,
        categoryId: values.transactionType === 'TRANSFER' ? null : values.categoryId,
        transactionType: values.transactionType,
        amount: Number(values.amount),
        description: values.description.trim(),
        frequency: values.frequency,
        startDate: values.startDate,
        endDate: values.endDate || null,
      };

      if (editingItem) {
        await recurringApi.updateRecurringTransaction(editingItem.id, payload);
        dispatch(showToast({ type: 'success', message: 'Cập nhật lịch định kỳ thành công.' }));
      } else {
        await recurringApi.createRecurringTransaction(payload);
        dispatch(showToast({ type: 'success', message: 'Tạo giao dịch định kỳ mới thành công.' }));
      }
      setModalOpen(false);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể lưu giao dịch định kỳ.'),
        })
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleTogglePauseResume = async (item) => {
    try {
      if (item.status === 'ACTIVE') {
        await recurringApi.pauseRecurringTransaction(item.id);
        dispatch(showToast({ type: 'success', message: 'Đã tạm dừng giao dịch định kỳ.' }));
      } else if (item.status === 'PAUSED') {
        await recurringApi.resumeRecurringTransaction(item.id);
        dispatch(showToast({ type: 'success', message: 'Đã kích hoạt lại giao dịch định kỳ.' }));
      }
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể cập nhật trạng thái.'),
        })
      );
    }
  };

  const handleExecuteNow = async () => {
    if (!executeTarget) return;
    setExecuting(true);
    try {
      await recurringApi.executeNow(executeTarget.id, allowEarly);
      dispatch(
        showToast({
          type: 'success',
          message: `Đã thực thi "${executeTarget.description}" và ghi nhận giao dịch mới.`,
        })
      );
      setExecuteTarget(null);
      setAllowEarly(false);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể thực thi giao dịch định kỳ.'),
        })
      );
    } finally {
      setExecuting(false);
    }
  };

  const handleConfirm = async () => {
    if (!confirmTarget) return;
    setProcessingConfirm(true);
    try {
      if (confirmTarget.type === 'CANCEL') {
        await recurringApi.cancelRecurringTransaction(confirmTarget.item.id);
        dispatch(showToast({ type: 'success', message: 'Đã hủy lịch giao dịch định kỳ.' }));
      } else {
        await recurringApi.deleteRecurringTransaction(confirmTarget.item.id);
        dispatch(showToast({ type: 'success', message: 'Đã xóa lịch giao dịch định kỳ.' }));
      }
      setConfirmTarget(null);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể thực hiện thao tác.'),
        })
      );
    } finally {
      setProcessingConfirm(false);
    }
  };

  const selectableCategories = categories.filter((c) => c.categoryType === watchedType);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          {[
            { value: '', label: 'Tất cả' },
            { value: 'ACTIVE', label: 'Đang chạy' },
            { value: 'PAUSED', label: 'Tạm dừng' },
            { value: 'COMPLETED', label: 'Hoàn tất' },
            { value: 'CANCELLED', label: 'Đã hủy' },
          ].map((tab) => (
            <button
              key={tab.value}
              type="button"
              onClick={() => setStatusFilter(tab.value)}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition-colors ${
                statusFilter === tab.value
                  ? 'bg-slate-900 text-white'
                  : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <button
          type="button"
          onClick={openCreateModal}
          className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 text-xs font-semibold transition-colors"
        >
          <PlusIcon className="h-4 w-4" />
          Thêm lịch định kỳ
        </button>
      </div>

      {loading ? (
        <LoadingSpinner label="Đang tải giao dịch định kỳ..." />
      ) : !items.length ? (
        <div className="card-surface">
          <EmptyState
            icon={ArrowPathIcon}
            title="Chưa có giao dịch định kỳ"
            description="Tự động hóa tiền thuê nhà, hóa đơn điện nước, lương hàng tháng hoặc tiết kiệm định kỳ."
            action={
              <button
                type="button"
                onClick={openCreateModal}
                className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 text-xs font-semibold"
              >
                <PlusIcon className="h-4 w-4" />
                Tạo lịch định kỳ
              </button>
            }
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {items.map((item) => {
            const statusInfo = RECURRING_STATUS_MAP[item.status] || RECURRING_STATUS_MAP.ACTIVE;
            const typeInfo = TRANSACTION_TYPE_MAP[item.transactionType] || {};
            const isIncome = item.transactionType === 'INCOME';
            const isExpense = item.transactionType === 'EXPENSE';

            return (
              <div key={item.id} className="card-surface p-5 flex flex-col justify-between space-y-4">
                <div className="space-y-3">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <div
                        className="h-10 w-10 rounded-xl flex items-center justify-center shrink-0"
                        style={{
                          backgroundColor: item.categoryColor ? `${item.categoryColor}18` : '#E0F2FE',
                        }}
                      >
                        <CategoryIcon
                          icon={item.categoryIcon}
                          color={item.categoryColor || '#0EA5E9'}
                          className="h-5 w-5"
                        />
                      </div>
                      <div className="min-w-0">
                        <h3 className="text-sm font-bold text-slate-900 truncate">
                          {item.description}
                        </h3>
                        <p className="text-[11px] text-slate-500 truncate">
                          {item.accountName}
                          {item.toAccountName ? ` → ${item.toAccountName}` : ''} •{' '}
                          {item.categoryName || typeInfo.label}
                        </p>
                      </div>
                    </div>

                    <span
                      className={`px-2 py-0.5 rounded-full text-[11px] font-medium border shrink-0 ${statusInfo.badgeClass}`}
                    >
                      {statusInfo.label}
                    </span>
                  </div>

                  <div className="flex flex-wrap items-baseline justify-between gap-2 pt-2 border-t border-slate-100">
                    <span
                      className={`text-lg font-bold tabular-nums ${
                        isIncome ? 'text-emerald-600' : isExpense ? 'text-rose-600' : 'text-sky-600'
                      }`}
                    >
                      {isIncome ? '+' : isExpense ? '-' : ''}
                      {formatCurrency(item.amount, currency)}
                    </span>
                    <span className="text-xs font-medium text-slate-600 bg-slate-100 px-2.5 py-0.5 rounded-full">
                      {RECURRING_FREQUENCY_LABELS[item.frequency] || item.frequency}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-[11px] text-slate-500">
                    <div>
                      Kỳ kế tiếp:{' '}
                      <strong className="text-slate-700">{formatDate(item.nextExecutionDate)}</strong>
                    </div>
                    <div>
                      Lần chạy cuối:{' '}
                      <strong className="text-slate-700">
                        {item.lastExecutedAt ? formatDateTime(item.lastExecutedAt) : 'Chưa chạy'}
                      </strong>
                    </div>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5">
                    {item.status === 'ACTIVE' && (
                      <button
                        type="button"
                        onClick={() => {
                          setAllowEarly(true);
                          setExecuteTarget(item);
                        }}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-emerald-50 text-emerald-700 hover:bg-emerald-100 text-xs font-semibold"
                      >
                        <PlayIcon className="h-3.5 w-3.5" />
                        Thực thi ngay
                      </button>
                    )}
                    {(item.status === 'ACTIVE' || item.status === 'PAUSED') && (
                      <button
                        type="button"
                        onClick={() => handleTogglePauseResume(item)}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-amber-50 text-amber-700 hover:bg-amber-100 text-xs font-semibold"
                      >
                        {item.status === 'ACTIVE' ? (
                          <>
                            <PauseIcon className="h-3.5 w-3.5" />
                            Tạm dừng
                          </>
                        ) : (
                          <>
                            <PlayIcon className="h-3.5 w-3.5" />
                            Tiếp tục
                          </>
                        )}
                      </button>
                    )}
                  </div>

                  <div className="flex items-center gap-1">
                    {(item.status === 'ACTIVE' || item.status === 'PAUSED') && (
                      <>
                        <button
                          type="button"
                          onClick={() => openEditModal(item)}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-slate-800 hover:bg-slate-100"
                          title="Chỉnh sửa"
                        >
                          <PencilSquareIcon className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmTarget({ type: 'CANCEL', item })}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-amber-600 hover:bg-amber-50"
                          title="Hủy lịch"
                        >
                          <NoSymbolIcon className="h-4 w-4" />
                        </button>
                      </>
                    )}
                    <button
                      type="button"
                      onClick={() => setConfirmTarget({ type: 'DELETE', item })}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50"
                      title="Xóa"
                    >
                      <TrashIcon className="h-4 w-4" />
                    </button>
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
        title={editingItem ? 'Cập nhật giao dịch định kỳ' : 'Tạo giao dịch định kỳ mới'}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-3 gap-2 p-1 rounded-xl bg-slate-100">
            {TRANSACTION_TYPES.map((t) => (
              <button
                key={t.value}
                type="button"
                onClick={() => handleTypeChange(t.value)}
                className={`py-2 rounded-lg text-xs font-semibold transition-all ${
                  watchedType === t.value
                    ? 'bg-white text-slate-900 shadow-sm'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Mô tả <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              placeholder="Ví dụ: Tiền thuê nhà, Lương hàng tháng..."
              {...register('description', {
                required: 'Vui lòng nhập mô tả',
                maxLength: { value: 255, message: 'Tối đa 255 ký tự' },
              })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
            {errors.description && (
              <p className="text-xs text-rose-600 mt-1">{errors.description.message}</p>
            )}
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                {watchedType === 'TRANSFER' ? 'Tài khoản nguồn' : 'Tài khoản'}{' '}
                <span className="text-rose-500">*</span>
              </label>
              <select
                {...register('accountId', { required: 'Vui lòng chọn tài khoản' })}
                className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm bg-white outline-none focus:border-emerald-500"
              >
                <option value="">Chọn tài khoản</option>
                {accounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.accountName}
                  </option>
                ))}
              </select>
            </div>

            {watchedType === 'TRANSFER' ? (
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Tài khoản đích <span className="text-rose-500">*</span>
                </label>
                <select
                  {...register('toAccountId', {
                    validate: (val) =>
                      watchedType !== 'TRANSFER' ||
                      (Boolean(val) && val !== watchedAccountId) ||
                      'Chọn tài khoản đích khác tài khoản nguồn',
                  })}
                  className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm bg-white outline-none focus:border-emerald-500"
                >
                  <option value="">Chọn tài khoản đích</option>
                  {accounts
                    .filter((a) => a.id !== watchedAccountId)
                    .map((acc) => (
                      <option key={acc.id} value={acc.id}>
                        {acc.accountName}
                      </option>
                    ))}
                </select>
                {errors.toAccountId && (
                  <p className="text-xs text-rose-600 mt-1">{errors.toAccountId.message}</p>
                )}
              </div>
            ) : (
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Danh mục <span className="text-rose-500">*</span>
                </label>
                <select
                  {...register('categoryId', {
                    validate: (val) =>
                      watchedType === 'TRANSFER' || Boolean(val) || 'Vui lòng chọn danh mục',
                  })}
                  className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm bg-white outline-none focus:border-emerald-500"
                >
                  <option value="">Chọn danh mục</option>
                  {selectableCategories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
                {errors.categoryId && (
                  <p className="text-xs text-rose-600 mt-1">{errors.categoryId.message}</p>
                )}
              </div>
            )}
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Số tiền <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                step="0.01"
                {...register('amount', {
                  required: 'Vui lòng nhập số tiền',
                  min: { value: 0.01, message: 'Số tiền phải lớn hơn 0' },
                })}
                className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm tabular-nums outline-none focus:border-emerald-500"
              />
              {errors.amount && (
                <p className="text-xs text-rose-600 mt-1">{errors.amount.message}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">Chu kỳ lặp</label>
              <select
                {...register('frequency')}
                className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm bg-white outline-none focus:border-emerald-500"
              >
                {RECURRING_FREQUENCIES.map((f) => (
                  <option key={f.value} value={f.value}>
                    {f.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">Ngày bắt đầu</label>
              <input
                type="date"
                {...register('startDate', { required: 'Vui lòng chọn ngày bắt đầu' })}
                className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Ngày kết thúc (Tùy chọn)
              </label>
              <input
                type="date"
                {...register('endDate', {
                  validate: (val) =>
                    !val || !watchedStartDate || val >= watchedStartDate || 'Ngày kết thúc phải sau ngày bắt đầu',
                })}
                className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
              />
              {errors.endDate && (
                <p className="text-xs text-rose-600 mt-1">{errors.endDate.message}</p>
              )}
            </div>
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
              {submitting ? 'Đang lưu...' : editingItem ? 'Lưu thay đổi' : 'Tạo lịch định kỳ'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={Boolean(executeTarget)}
        onClose={() => setExecuteTarget(null)}
        title="Thực thi giao dịch định kỳ ngay"
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-xs text-slate-600 leading-relaxed">
            Hệ thống sẽ tạo ngay một giao dịch cho{' '}
            <strong className="text-slate-900">{executeTarget?.description}</strong> với số tiền{' '}
            <strong className="text-slate-900">
              {formatCurrency(executeTarget?.amount, currency)}
            </strong>{' '}
            và tự động tính kỳ chạy tiếp theo.
          </p>

          <label className="flex items-start gap-2.5 rounded-xl border border-slate-200 p-3 cursor-pointer">
            <input
              type="checkbox"
              checked={allowEarly}
              onChange={(e) => setAllowEarly(e.target.checked)}
              className="mt-0.5 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500"
            />
            <span className="text-xs text-slate-700">
              Cho phép thực thi trước ngày dự kiến ({formatDate(executeTarget?.nextExecutionDate)})
            </span>
          </label>

          <div className="flex justify-end gap-2.5">
            <button
              type="button"
              onClick={() => setExecuteTarget(null)}
              className="px-4 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-50"
            >
              Hủy
            </button>
            <button
              type="button"
              onClick={handleExecuteNow}
              disabled={executing}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold disabled:opacity-60"
            >
              {executing ? 'Đang thực thi...' : 'Xác nhận thực thi'}
            </button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        isOpen={Boolean(confirmTarget)}
        onClose={() => setConfirmTarget(null)}
        onConfirm={handleConfirm}
        loading={processingConfirm}
        danger={confirmTarget?.type === 'DELETE'}
        title={confirmTarget?.type === 'CANCEL' ? 'Hủy lịch định kỳ' : 'Xóa lịch định kỳ'}
        message={
          confirmTarget?.type === 'CANCEL'
            ? `Bạn có chắc muốn hủy lịch định kỳ "${confirmTarget?.item?.description}"?`
            : `Bạn có chắc muốn xóa lịch định kỳ "${confirmTarget?.item?.description}"?`
        }
        confirmLabel={confirmTarget?.type === 'CANCEL' ? 'Hủy lịch' : 'Xóa'}
      />
    </div>
  );
}

