import { useCallback, useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useForm } from 'react-hook-form';
import {
  ArrowDownTrayIcon,
  ArrowUpTrayIcon,
  BanknotesIcon,
  ClockIcon,
  NoSymbolIcon,
  PencilSquareIcon,
  PlusIcon,
  TrashIcon,
} from '@heroicons/react/24/outline';
import { savingGoalApi } from '../../services/savingGoalApi';
import { accountApi } from '../../services/accountApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import Modal from '../../components/common/Modal';
import Pagination from '../../components/common/Pagination';
import EmptyState from '../../components/common/EmptyState';
import ConfirmDialog from '../../components/feedback/ConfirmDialog';
import LoadingSpinner from '../../components/feedback/LoadingSpinner';
import { HEX_COLOR_PALETTE, SAVING_GOAL_STATUS_MAP } from '../../constants/enums';
import {
  dateTimeLocalToIso,
  extractErrorMessage,
  formatCurrency,
  formatDate,
  formatDateTime,
  toDateInputValue,
  toDateTimeLocalValue,
} from '../../utils/formatters';

export default function SavingGoalsPage() {
  const dispatch = useDispatch();
  const currency = useSelector((state) => state.auth.user?.currency || 'VND');

  const [loading, setLoading] = useState(true);
  const [goals, setGoals] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');

  const [goalModalOpen, setGoalModalOpen] = useState(false);
  const [editingGoal, setEditingGoal] = useState(null);
  const [submittingGoal, setSubmittingGoal] = useState(false);

  const [contribModal, setContribModal] = useState({ open: false, mode: 'DEPOSIT', goal: null });
  const [submittingContrib, setSubmittingContrib] = useState(false);

  const [historyGoal, setHistoryGoal] = useState(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyData, setHistoryData] = useState({
    content: [],
    pageNumber: 0,
    pageSize: 8,
    totalElements: 0,
    totalPages: 0,
  });

  const [confirmAction, setConfirmAction] = useState(null);
  const [processingConfirm, setProcessingConfirm] = useState(false);

  const goalForm = useForm();
  const contribForm = useForm();
  const selectedColor = goalForm.watch('color');

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [goalRes, accRes] = await Promise.all([
        savingGoalApi.getSavingGoals(statusFilter || undefined),
        accountApi.getAccounts({ status: 'ACTIVE' }),
      ]);
      setGoals(goalRes.data || []);
      setAccounts(accRes.data || []);
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể tải mục tiêu tiết kiệm.'),
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
    setEditingGoal(null);
    goalForm.reset({
      name: '',
      targetAmount: '',
      startDate: toDateInputValue(new Date()),
      targetDate: '',
      description: '',
      color: HEX_COLOR_PALETTE[2],
    });
    setGoalModalOpen(true);
  };

  const openEditModal = (goal) => {
    setEditingGoal(goal);
    goalForm.reset({
      name: goal.name,
      targetAmount: goal.targetAmount,
      startDate: goal.startDate ? toDateInputValue(goal.startDate) : toDateInputValue(new Date()),
      targetDate: goal.targetDate ? toDateInputValue(goal.targetDate) : '',
      description: goal.description || '',
      color: goal.color || HEX_COLOR_PALETTE[2],
    });
    setGoalModalOpen(true);
  };

  const onSubmitGoal = async (values) => {
    setSubmittingGoal(true);
    try {
      const payload = {
        name: values.name.trim(),
        targetAmount: Number(values.targetAmount),
        startDate: values.startDate,
        targetDate: values.targetDate || null,
        description: values.description?.trim() || null,
        color: values.color || HEX_COLOR_PALETTE[2],
      };

      if (editingGoal) {
        await savingGoalApi.updateSavingGoal(editingGoal.id, payload);
        dispatch(showToast({ type: 'success', message: 'Cập nhật mục tiêu thành công.' }));
      } else {
        await savingGoalApi.createSavingGoal(payload);
        dispatch(showToast({ type: 'success', message: 'Tạo mục tiêu tiết kiệm mới thành công.' }));
      }
      setGoalModalOpen(false);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể lưu mục tiêu tiết kiệm.'),
        })
      );
    } finally {
      setSubmittingGoal(false);
    }
  };

  const openContribModal = (mode, goal) => {
    contribForm.reset({
      accountId: accounts[0]?.id || '',
      amount: '',
      note: '',
      contributionDate: toDateTimeLocalValue(new Date()),
    });
    setContribModal({ open: true, mode, goal });
  };

  const onSubmitContrib = async (values) => {
    if (!contribModal.goal) return;
    setSubmittingContrib(true);
    try {
      const payload = {
        accountId: values.accountId,
        amount: Number(values.amount),
        note: values.note?.trim() || null,
        contributionDate: dateTimeLocalToIso(values.contributionDate),
      };

      if (contribModal.mode === 'DEPOSIT') {
        await savingGoalApi.deposit(contribModal.goal.id, payload);
        dispatch(showToast({ type: 'success', message: 'Nạp tiền vào mục tiêu thành công.' }));
      } else {
        await savingGoalApi.withdraw(contribModal.goal.id, payload);
        dispatch(showToast({ type: 'success', message: 'Rút tiền từ mục tiêu thành công.' }));
      }

      setContribModal({ open: false, mode: 'DEPOSIT', goal: null });
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Giao dịch mục tiêu không thành công.'),
        })
      );
    } finally {
      setSubmittingContrib(false);
    }
  };

  const loadHistory = async (goal, page = 0) => {
    setHistoryGoal(goal);
    setHistoryLoading(true);
    try {
      const res = await savingGoalApi.getContributions(goal.id, { page, size: 8 });
      setHistoryData(
        res.data || { content: [], pageNumber: 0, pageSize: 8, totalElements: 0, totalPages: 0 }
      );
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể tải lịch sử đóng góp.'),
        })
      );
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleConfirmAction = async () => {
    if (!confirmAction) return;
    setProcessingConfirm(true);
    try {
      if (confirmAction.type === 'CANCEL') {
        await savingGoalApi.cancelSavingGoal(confirmAction.goal.id);
        dispatch(showToast({ type: 'success', message: 'Đã hủy mục tiêu tiết kiệm.' }));
      } else {
        await savingGoalApi.deleteSavingGoal(confirmAction.goal.id);
        dispatch(showToast({ type: 'success', message: 'Đã xóa mục tiêu tiết kiệm.' }));
      }
      setConfirmAction(null);
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

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          {[
            { value: '', label: 'Tất cả' },
            { value: 'IN_PROGRESS', label: 'Đang thực hiện' },
            { value: 'COMPLETED', label: 'Hoàn thành' },
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
          Tạo mục tiêu
        </button>
      </div>

      {loading ? (
        <LoadingSpinner label="Đang tải mục tiêu tiết kiệm..." />
      ) : !goals.length ? (
        <div className="card-surface">
          <EmptyState
            icon={BanknotesIcon}
            title="Chưa có mục tiêu tiết kiệm"
            description="Thiết lập quỹ dự phòng, du lịch hoặc mua sắm để theo dõi tiến độ tích lũy."
            action={
              <button
                type="button"
                onClick={openCreateModal}
                className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 text-xs font-semibold"
              >
                <PlusIcon className="h-4 w-4" />
                Tạo mục tiêu đầu tiên
              </button>
            }
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {goals.map((goal) => {
            const statusInfo = SAVING_GOAL_STATUS_MAP[goal.status] || SAVING_GOAL_STATUS_MAP.IN_PROGRESS;
            const pct = Math.min(100, Number(goal.progressPercentage ?? 0));
            const canDeposit = goal.status === 'IN_PROGRESS';
            const canWithdraw = Number(goal.currentAmount ?? 0) > 0 && goal.status !== 'CANCELLED';

            return (
              <div key={goal.id} className="card-surface p-5 flex flex-col justify-between space-y-4">
                <div className="space-y-3">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2.5 min-w-0">
                      <span
                        className="h-3.5 w-3.5 rounded-full shrink-0"
                        style={{ backgroundColor: goal.color || '#0EA5E9' }}
                      />
                      <div className="min-w-0">
                        <h3 className="text-sm font-bold text-slate-900 truncate">{goal.name}</h3>
                        <p className="text-[11px] text-slate-500">
                          Bắt đầu: {formatDate(goal.startDate)}
                          {goal.targetDate ? ` • Hạn: ${formatDate(goal.targetDate)}` : ''}
                        </p>
                      </div>
                    </div>
                    <span
                      className={`px-2 py-0.5 rounded-full text-[11px] font-medium border shrink-0 ${statusInfo.badgeClass}`}
                    >
                      {statusInfo.label}
                    </span>
                  </div>

                  {goal.description && (
                    <p className="text-xs text-slate-600 line-clamp-2">{goal.description}</p>
                  )}

                  <div>
                    <div className="flex justify-between text-xs mb-1.5 tabular-nums">
                      <span className="text-slate-600">
                        Hiện có:{' '}
                        <strong className="text-slate-900">
                          {formatCurrency(goal.currentAmount, currency)}
                        </strong>
                      </span>
                      <span className="text-slate-600">
                        Mục tiêu:{' '}
                        <strong className="text-slate-900">
                          {formatCurrency(goal.targetAmount, currency)}
                        </strong>
                      </span>
                    </div>
                    <div className="h-2.5 w-full rounded-full bg-slate-100 overflow-hidden">
                      <div
                        className="h-full rounded-full transition-all"
                        style={{
                          width: `${pct}%`,
                          backgroundColor: goal.color || '#0EA5E9',
                        }}
                      />
                    </div>
                    <div className="flex justify-between text-[11px] text-slate-500 mt-1.5 tabular-nums">
                      <span>Tiến độ: {Number(goal.progressPercentage ?? 0).toFixed(1)}%</span>
                      <span>Còn thiếu: {formatCurrency(goal.remainingAmount, currency)}</span>
                    </div>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5">
                    {canDeposit && (
                      <button
                        type="button"
                        onClick={() => openContribModal('DEPOSIT', goal)}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-emerald-50 text-emerald-700 hover:bg-emerald-100 text-xs font-semibold"
                      >
                        <ArrowDownTrayIcon className="h-3.5 w-3.5" />
                        Nạp tiền
                      </button>
                    )}
                    {canWithdraw && (
                      <button
                        type="button"
                        onClick={() => openContribModal('WITHDRAW', goal)}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-amber-50 text-amber-700 hover:bg-amber-100 text-xs font-semibold"
                      >
                        <ArrowUpTrayIcon className="h-3.5 w-3.5" />
                        Rút tiền
                      </button>
                    )}
                    <button
                      type="button"
                      onClick={() => loadHistory(goal, 0)}
                      className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-slate-100 text-slate-700 hover:bg-slate-200 text-xs font-medium"
                    >
                      <ClockIcon className="h-3.5 w-3.5" />
                      Lịch sử
                    </button>
                  </div>

                  <div className="flex items-center gap-1">
                    {goal.status === 'IN_PROGRESS' && (
                      <>
                        <button
                          type="button"
                          onClick={() => openEditModal(goal)}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-slate-800 hover:bg-slate-100"
                          title="Chỉnh sửa"
                        >
                          <PencilSquareIcon className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmAction({ type: 'CANCEL', goal })}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-amber-600 hover:bg-amber-50"
                          title="Hủy mục tiêu"
                        >
                          <NoSymbolIcon className="h-4 w-4" />
                        </button>
                      </>
                    )}
                    <button
                      type="button"
                      onClick={() => setConfirmAction({ type: 'DELETE', goal })}
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
        isOpen={goalModalOpen}
        onClose={() => setGoalModalOpen(false)}
        title={editingGoal ? 'Cập nhật mục tiêu tiết kiệm' : 'Tạo mục tiêu tiết kiệm'}
      >
        <form onSubmit={goalForm.handleSubmit(onSubmitGoal)} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Tên mục tiêu <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              placeholder="Ví dụ: Quỹ khẩn cấp 6 tháng, Mua xe..."
              {...goalForm.register('name', {
                required: 'Vui lòng nhập tên mục tiêu',
                maxLength: { value: 120, message: 'Tối đa 120 ký tự' },
              })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
            {goalForm.formState.errors.name && (
              <p className="text-xs text-rose-600 mt-1">{goalForm.formState.errors.name.message}</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Số tiền mục tiêu <span className="text-rose-500">*</span>
            </label>
            <input
              type="number"
              step="0.01"
              {...goalForm.register('targetAmount', {
                required: 'Vui lòng nhập số tiền mục tiêu',
                min: { value: 0.01, message: 'Số tiền mục tiêu phải lớn hơn 0' },
              })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm tabular-nums outline-none focus:border-emerald-500"
            />
            {goalForm.formState.errors.targetAmount && (
              <p className="text-xs text-rose-600 mt-1">
                {goalForm.formState.errors.targetAmount.message}
              </p>
            )}
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">Ngày bắt đầu</label>
              <input
                type="date"
                {...goalForm.register('startDate', { required: 'Vui lòng chọn ngày bắt đầu' })}
                className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
              />
              {goalForm.formState.errors.startDate && (
                <p className="text-xs text-rose-600 mt-1">
                  {goalForm.formState.errors.startDate.message}
                </p>
              )}
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Hạn hoàn thành <span className="text-rose-500">*</span>
              </label>
              <input
                type="date"
                {...goalForm.register('targetDate', {
                  required: 'Vui lòng chọn ngày dự kiến hoàn thành',
                  validate: (val) => {
                    const start = goalForm.getValues('startDate');
                    return !start || val >= start || 'Hạn hoàn thành phải sau ngày bắt đầu';
                  },
                })}
                className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
              />
              {goalForm.formState.errors.targetDate && (
                <p className="text-xs text-rose-600 mt-1">
                  {goalForm.formState.errors.targetDate.message}
                </p>
              )}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-2">Màu nhận diện</label>
            <div className="flex flex-wrap gap-2">
              {HEX_COLOR_PALETTE.map((hex) => (
                <button
                  key={hex}
                  type="button"
                  onClick={() => goalForm.setValue('color', hex)}
                  className={`h-7 w-7 rounded-full border-2 ${
                    selectedColor?.toUpperCase() === hex.toUpperCase()
                      ? 'border-slate-900 scale-110'
                      : 'border-transparent'
                  }`}
                  style={{ backgroundColor: hex }}
                />
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">Mô tả</label>
            <textarea
              rows={2}
              {...goalForm.register('description')}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-sm outline-none focus:border-emerald-500"
            />
          </div>

          <div className="flex justify-end gap-2.5 pt-2">
            <button
              type="button"
              onClick={() => setGoalModalOpen(false)}
              className="px-4 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={submittingGoal}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold disabled:opacity-60"
            >
              {submittingGoal ? 'Đang lưu...' : editingGoal ? 'Lưu thay đổi' : 'Tạo mục tiêu'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={contribModal.open}
        onClose={() => setContribModal({ open: false, mode: 'DEPOSIT', goal: null })}
        title={
          contribModal.mode === 'DEPOSIT'
            ? `Nạp tiền vào "${contribModal.goal?.name || ''}"`
            : `Rút tiền từ "${contribModal.goal?.name || ''}"`
        }
        subtitle={
          contribModal.mode === 'DEPOSIT'
            ? 'Số tiền sẽ được trừ từ tài khoản nguồn bạn chọn.'
            : 'Số tiền rút sẽ được hoàn về tài khoản bạn chọn.'
        }
      >
        <form onSubmit={contribForm.handleSubmit(onSubmitContrib)} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              {contribModal.mode === 'DEPOSIT' ? 'Từ tài khoản' : 'Hoàn về tài khoản'}{' '}
              <span className="text-rose-500">*</span>
            </label>
            <select
              {...contribForm.register('accountId', { required: 'Vui lòng chọn tài khoản' })}
              className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm bg-white outline-none focus:border-emerald-500"
            >
              <option value="">Chọn tài khoản</option>
              {accounts.map((acc) => (
                <option key={acc.id} value={acc.id}>
                  {acc.accountName} ({formatCurrency(acc.currentBalance, acc.currency)})
                </option>
              ))}
            </select>
            {contribForm.formState.errors.accountId && (
              <p className="text-xs text-rose-600 mt-1">
                {contribForm.formState.errors.accountId.message}
              </p>
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
                {...contribForm.register('amount', {
                  required: 'Vui lòng nhập số tiền',
                  min: { value: 0.01, message: 'Số tiền phải lớn hơn 0' },
                })}
                className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm tabular-nums outline-none focus:border-emerald-500"
              />
              {contribForm.formState.errors.amount && (
                <p className="text-xs text-rose-600 mt-1">
                  {contribForm.formState.errors.amount.message}
                </p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">Thời gian</label>
              <input
                type="datetime-local"
                {...contribForm.register('contributionDate')}
                className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">Ghi chú</label>
            <input
              type="text"
              placeholder="Ví dụ: Trích lương tháng này"
              {...contribForm.register('note')}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
          </div>

          <div className="flex justify-end gap-2.5 pt-2">
            <button
              type="button"
              onClick={() => setContribModal({ open: false, mode: 'DEPOSIT', goal: null })}
              className="px-4 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={submittingContrib}
              className={`px-4 py-2 rounded-xl text-white text-xs font-semibold disabled:opacity-60 ${
                contribModal.mode === 'DEPOSIT'
                  ? 'bg-emerald-600 hover:bg-emerald-700'
                  : 'bg-amber-600 hover:bg-amber-700'
              }`}
            >
              {submittingContrib
                ? 'Đang xử lý...'
                : contribModal.mode === 'DEPOSIT'
                  ? 'Xác nhận nạp tiền'
                  : 'Xác nhận rút tiền'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={Boolean(historyGoal)}
        onClose={() => setHistoryGoal(null)}
        title={`Lịch sử đóng góp: ${historyGoal?.name || ''}`}
        size="lg"
      >
        {historyLoading ? (
          <LoadingSpinner label="Đang tải lịch sử..." />
        ) : !historyData.content?.length ? (
          <EmptyState
            title="Chưa có lịch sử đóng góp"
            description="Thực hiện nạp hoặc rút tiền để ghi nhận lịch sử cho mục tiêu này."
          />
        ) : (
          <div>
            <div className="divide-y divide-slate-100">
              {historyData.content.map((item) => {
                const isDeposit = (item.contributionType || item.type) === 'DEPOSIT';
                return (
                  <div key={item.id} className="py-3 flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-slate-900">
                        {isDeposit ? 'Nạp vào mục tiêu' : 'Rút từ mục tiêu'}
                        {item.accountName ? ` • ${item.accountName}` : ''}
                      </p>
                      <p className="text-[11px] text-slate-500">
                        {formatDateTime(item.contributionDate || item.createdAt)}
                        {item.note ? ` • ${item.note}` : ''}
                      </p>
                    </div>
                    <span
                      className={`text-xs font-bold tabular-nums shrink-0 ${
                        isDeposit ? 'text-emerald-600' : 'text-amber-600'
                      }`}
                    >
                      {isDeposit ? '+' : '-'}
                      {formatCurrency(item.amount, currency)}
                    </span>
                  </div>
                );
              })}
            </div>
            <Pagination
              pageNumber={historyData.pageNumber}
              totalPages={historyData.totalPages}
              totalElements={historyData.totalElements}
              pageSize={historyData.pageSize}
              onPageChange={(nextPage) => loadHistory(historyGoal, nextPage)}
            />
          </div>
        )}
      </Modal>

      <ConfirmDialog
        isOpen={Boolean(confirmAction)}
        onClose={() => setConfirmAction(null)}
        onConfirm={handleConfirmAction}
        loading={processingConfirm}
        danger={confirmAction?.type === 'DELETE'}
        title={confirmAction?.type === 'CANCEL' ? 'Hủy mục tiêu tiết kiệm' : 'Xóa mục tiêu tiết kiệm'}
        message={
          confirmAction?.type === 'CANCEL'
            ? `Bạn có chắc muốn hủy mục tiêu "${confirmAction?.goal?.name}"?`
            : `Bạn có chắc muốn xóa vĩnh viễn mục tiêu "${confirmAction?.goal?.name}"?`
        }
        confirmLabel={confirmAction?.type === 'CANCEL' ? 'Hủy mục tiêu' : 'Xóa'}
      />
    </div>
  );
}
