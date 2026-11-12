import { useCallback, useEffect, useRef, useState } from 'react';
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
import { useTranslation } from '../../i18n/I18nContext';
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
  const { t } = useTranslation();
  const tRef = useRef(t);
  tRef.current = t;

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
          message: extractErrorMessage(err, tRef.current('savings.loadError')),
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
        dispatch(showToast({ type: 'success', message: t('savings.updateSuccess') }));
      } else {
        await savingGoalApi.createSavingGoal(payload);
        dispatch(showToast({ type: 'success', message: t('savings.createSuccess') }));
      }
      setGoalModalOpen(false);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('savings.saveError')),
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
        dispatch(showToast({ type: 'success', message: t('savings.depositSuccess') }));
      } else {
        await savingGoalApi.withdraw(contribModal.goal.id, payload);
        dispatch(showToast({ type: 'success', message: t('savings.withdrawSuccess') }));
      }

      setContribModal({ open: false, mode: 'DEPOSIT', goal: null });
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('savings.contribError')),
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
          message: extractErrorMessage(err, t('savings.historyError')),
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
        dispatch(showToast({ type: 'success', message: t('savings.cancelSuccess') }));
      } else {
        await savingGoalApi.deleteSavingGoal(confirmAction.goal.id);
        dispatch(showToast({ type: 'success', message: t('savings.deleteSuccess') }));
      }
      setConfirmAction(null);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('savings.actionError')),
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
            { value: '', label: t('common.all') },
            { value: 'IN_PROGRESS', label: t(SAVING_GOAL_STATUS_MAP.IN_PROGRESS.labelKey) },
            { value: 'COMPLETED', label: t(SAVING_GOAL_STATUS_MAP.COMPLETED.labelKey) },
            { value: 'CANCELLED', label: t(SAVING_GOAL_STATUS_MAP.CANCELLED.labelKey) },
          ].map((tab) => (
            <button
              key={tab.value}
              type="button"
              onClick={() => setStatusFilter(tab.value)}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition-colors ${
                statusFilter === tab.value
                  ? 'bg-slate-900 dark:bg-emerald-600 text-white'
                  : 'bg-white dark:bg-zinc-950 border border-slate-200 dark:border-zinc-800 text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-zinc-900'
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
          {t('savings.addGoal')}
        </button>
      </div>

      {loading ? (
        <LoadingSpinner label={t('savings.loading')} />
      ) : !goals.length ? (
        <div className="card-surface">
          <EmptyState
            icon={BanknotesIcon}
            title={t('savings.emptyTitle')}
            description={t('savings.emptyDesc')}
            action={
              <button
                type="button"
                onClick={openCreateModal}
                className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 text-xs font-semibold"
              >
                <PlusIcon className="h-4 w-4" />
                {t('savings.createFirst')}
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
                        <h3 className="text-sm font-bold text-slate-900 dark:text-white truncate">{goal.name}</h3>
                        <p className="text-[11px] text-slate-500 dark:text-slate-400">
                          {t('savings.startLabel')}: {formatDate(goal.startDate)}
                          {goal.targetDate
                            ? ` • ${t('savings.deadlineShort')}: ${formatDate(goal.targetDate)}`
                            : ''}
                        </p>
                      </div>
                    </div>
                    <span
                      className={`px-2 py-0.5 rounded-full text-[11px] font-medium border shrink-0 ${statusInfo.badgeClass}`}
                    >
                      {t(statusInfo.labelKey)}
                    </span>
                  </div>

                  {goal.description && (
                    <p className="text-xs text-slate-600 dark:text-slate-300 line-clamp-2">{goal.description}</p>
                  )}

                  <div>
                    <div className="flex justify-between text-xs mb-1.5 tabular-nums">
                      <span className="text-slate-600 dark:text-slate-400">
                        {t('savings.currentLabel')}:{' '}
                        <strong className="text-slate-900 dark:text-white">
                          {formatCurrency(goal.currentAmount, currency)}
                        </strong>
                      </span>
                      <span className="text-slate-600 dark:text-slate-400">
                        {t('savings.targetLabel')}:{' '}
                        <strong className="text-slate-900 dark:text-white">
                          {formatCurrency(goal.targetAmount, currency)}
                        </strong>
                      </span>
                    </div>
                    <div className="h-2.5 w-full rounded-full bg-slate-100 dark:bg-zinc-800 overflow-hidden">
                      <div
                        className="h-full rounded-full transition-all"
                        style={{
                          width: `${pct}%`,
                          backgroundColor: goal.color || '#0EA5E9',
                        }}
                      />
                    </div>
                    <div className="flex justify-between text-[11px] text-slate-500 dark:text-slate-400 mt-1.5 tabular-nums">
                      <span>
                        {t('savings.progress')}: {Number(goal.progressPercentage ?? 0).toFixed(1)}%
                      </span>
                      <span>
                        {t('savings.stillNeeded')}: {formatCurrency(goal.remainingAmount, currency)}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-100 dark:border-zinc-800 flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5">
                    {canDeposit && (
                      <button
                        type="button"
                        onClick={() => openContribModal('DEPOSIT', goal)}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 text-xs font-semibold"
                      >
                        <ArrowDownTrayIcon className="h-3.5 w-3.5" />
                        {t('savings.deposit')}
                      </button>
                    )}
                    {canWithdraw && (
                      <button
                        type="button"
                        onClick={() => openContribModal('WITHDRAW', goal)}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 hover:bg-amber-100 dark:hover:bg-amber-900/60 text-xs font-semibold"
                      >
                        <ArrowUpTrayIcon className="h-3.5 w-3.5" />
                        {t('savings.withdraw')}
                      </button>
                    )}
                    <button
                      type="button"
                      onClick={() => loadHistory(goal, 0)}
                      className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-slate-100 dark:bg-zinc-900 text-slate-700 dark:text-slate-200 hover:bg-slate-200 dark:hover:bg-zinc-800 text-xs font-medium"
                    >
                      <ClockIcon className="h-3.5 w-3.5" />
                      {t('savings.history')}
                    </button>
                  </div>

                  <div className="flex items-center gap-1">
                    {goal.status === 'IN_PROGRESS' && (
                      <>
                        <button
                          type="button"
                          onClick={() => openEditModal(goal)}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-slate-800 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-zinc-800"
                          title={t('common.edit')}
                        >
                          <PencilSquareIcon className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmAction({ type: 'CANCEL', goal })}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-amber-600 dark:hover:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/50"
                          title={t('savings.cancelGoal')}
                        >
                          <NoSymbolIcon className="h-4 w-4" />
                        </button>
                      </>
                    )}
                    <button
                      type="button"
                      onClick={() => setConfirmAction({ type: 'DELETE', goal })}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/50"
                      title={t('common.delete')}
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
        title={editingGoal ? t('savings.modalEditTitle') : t('savings.modalCreateTitle')}
      >
        <form onSubmit={goalForm.handleSubmit(onSubmitGoal)} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('savings.nameLabel')} <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              placeholder={t('savings.namePlaceholder')}
              {...goalForm.register('name', {
                required: t('savings.nameRequired'),
                maxLength: { value: 120, message: t('savings.nameMax') },
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
            {goalForm.formState.errors.name && (
              <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{goalForm.formState.errors.name.message}</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('savings.targetAmountLabel')} <span className="text-rose-500">*</span>
            </label>
            <input
              type="number"
              step="0.01"
              {...goalForm.register('targetAmount', {
                required: t('savings.targetAmountRequired'),
                min: { value: 0.01, message: t('savings.targetAmountMin') },
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm tabular-nums outline-none focus:border-emerald-500"
            />
            {goalForm.formState.errors.targetAmount && (
              <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                {goalForm.formState.errors.targetAmount.message}
              </p>
            )}
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('savings.startDateLabel')}
              </label>
              <input
                type="date"
                {...goalForm.register('startDate', { required: t('savings.startDateRequired') })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
              />
              {goalForm.formState.errors.startDate && (
                <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                  {goalForm.formState.errors.startDate.message}
                </p>
              )}
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('savings.deadlineLabel')} <span className="text-rose-500">*</span>
              </label>
              <input
                type="date"
                {...goalForm.register('targetDate', {
                  required: t('savings.deadlineRequired'),
                  validate: (val) => {
                    const start = goalForm.getValues('startDate');
                    return !start || val >= start || t('savings.deadlineAfterStart');
                  },
                })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
              />
              {goalForm.formState.errors.targetDate && (
                <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                  {goalForm.formState.errors.targetDate.message}
                </p>
              )}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-2">
              {t('savings.colorLabel')}
            </label>
            <div className="flex flex-wrap gap-2">
              {HEX_COLOR_PALETTE.map((hex) => (
                <button
                  key={hex}
                  type="button"
                  onClick={() => goalForm.setValue('color', hex)}
                  className={`h-7 w-7 rounded-full border-2 ${
                    selectedColor?.toUpperCase() === hex.toUpperCase()
                      ? 'border-slate-900 dark:border-white scale-110'
                      : 'border-transparent'
                  }`}
                  style={{ backgroundColor: hex }}
                />
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('savings.descLabel')}
            </label>
            <textarea
              rows={2}
              {...goalForm.register('description')}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2 text-sm outline-none focus:border-emerald-500"
            />
          </div>

          <div className="flex justify-end gap-2.5 pt-2">
            <button
              type="button"
              onClick={() => setGoalModalOpen(false)}
              className="px-4 py-2 rounded-xl border border-slate-200 dark:border-zinc-700 text-xs font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-zinc-800"
            >
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              disabled={submittingGoal}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold disabled:opacity-60"
            >
              {submittingGoal
                ? t('common.saving')
                : editingGoal
                  ? t('savings.saveBtn')
                  : t('savings.createBtn')}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={contribModal.open}
        onClose={() => setContribModal({ open: false, mode: 'DEPOSIT', goal: null })}
        title={
          contribModal.mode === 'DEPOSIT'
            ? t('savings.depositTitle', { name: contribModal.goal?.name || '' })
            : t('savings.withdrawTitle', { name: contribModal.goal?.name || '' })
        }
        subtitle={
          contribModal.mode === 'DEPOSIT'
            ? t('savings.depositSubtitle')
            : t('savings.withdrawSubtitle')
        }
      >
        <form onSubmit={contribForm.handleSubmit(onSubmitContrib)} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {contribModal.mode === 'DEPOSIT' ? t('savings.fromAccount') : t('savings.toAccount')}{' '}
              <span className="text-rose-500">*</span>
            </label>
            <select
              {...contribForm.register('accountId', { required: t('savings.accountRequired') })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 px-3 py-2.5 text-sm bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 outline-none focus:border-emerald-500"
            >
              <option value="">{t('savings.chooseAccount')}</option>
              {accounts.map((acc) => (
                <option key={acc.id} value={acc.id}>
                  {acc.accountName} ({formatCurrency(acc.currentBalance, acc.currency)})
                </option>
              ))}
            </select>
            {contribForm.formState.errors.accountId && (
              <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                {contribForm.formState.errors.accountId.message}
              </p>
            )}
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('savings.amountLabel')} <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                step="0.01"
                {...contribForm.register('amount', {
                  required: t('savings.amountRequired'),
                  min: { value: 0.01, message: t('savings.amountMin') },
                })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm tabular-nums outline-none focus:border-emerald-500"
              />
              {contribForm.formState.errors.amount && (
                <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                  {contribForm.formState.errors.amount.message}
                </p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('savings.timeLabel')}
              </label>
              <input
                type="datetime-local"
                {...contribForm.register('contributionDate')}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('savings.noteLabel')}
            </label>
            <input
              type="text"
              placeholder={t('savings.notePlaceholder')}
              {...contribForm.register('note')}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
          </div>

          <div className="flex justify-end gap-2.5 pt-2">
            <button
              type="button"
              onClick={() => setContribModal({ open: false, mode: 'DEPOSIT', goal: null })}
              className="px-4 py-2 rounded-xl border border-slate-200 dark:border-zinc-700 text-xs font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-zinc-800"
            >
              {t('common.cancel')}
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
                ? t('common.processing')
                : contribModal.mode === 'DEPOSIT'
                  ? t('savings.confirmDeposit')
                  : t('savings.confirmWithdraw')}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={Boolean(historyGoal)}
        onClose={() => setHistoryGoal(null)}
        title={t('savings.historyTitle', { name: historyGoal?.name || '' })}
        size="lg"
      >
        {historyLoading ? (
          <LoadingSpinner label={t('savings.historyLoading')} />
        ) : !historyData.content?.length ? (
          <EmptyState
            title={t('savings.historyEmptyTitle')}
            description={t('savings.historyEmptyDesc')}
          />
        ) : (
          <div>
            <div className="divide-y divide-slate-100 dark:divide-zinc-800">
              {historyData.content.map((item) => {
                const isDeposit = (item.contributionType || item.type) === 'DEPOSIT';
                return (
                  <div key={item.id} className="py-3 flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-slate-900 dark:text-white">
                        {isDeposit ? t('savings.historyDeposit') : t('savings.historyWithdraw')}
                        {item.accountName ? ` • ${item.accountName}` : ''}
                      </p>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400">
                        {formatDateTime(item.contributionDate || item.createdAt)}
                        {item.note ? ` • ${item.note}` : ''}
                      </p>
                    </div>
                    <span
                      className={`text-xs font-bold tabular-nums shrink-0 ${
                        isDeposit ? 'text-emerald-600 dark:text-emerald-400' : 'text-amber-600 dark:text-amber-400'
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
        title={
          confirmAction?.type === 'CANCEL' ? t('savings.cancelTitle') : t('savings.deleteTitle')
        }
        message={
          confirmAction?.type === 'CANCEL'
            ? t('savings.cancelConfirmMsg', { name: confirmAction?.goal?.name || '' })
            : t('savings.deleteConfirmMsg', { name: confirmAction?.goal?.name || '' })
        }
        confirmLabel={
          confirmAction?.type === 'CANCEL' ? t('savings.cancelGoal') : t('common.delete')
        }
      />
    </div>
  );
}
