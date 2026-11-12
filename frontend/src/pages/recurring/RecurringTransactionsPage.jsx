import { useCallback, useEffect, useRef, useState } from 'react';
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
  RECURRING_FREQUENCY_LABEL_KEYS,
  RECURRING_STATUS_MAP,
  TRANSACTION_TYPE_MAP,
  TRANSACTION_TYPES,
} from '../../constants/enums';
import { useTranslation } from '../../i18n/I18nContext';
import {
  extractErrorMessage,
  formatCurrency,
  formatDate,
  formatDateTime,
  toDateInputValue,
} from '../../utils/formatters';

export default function RecurringTransactionsPage() {
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const tRef = useRef(t);
  tRef.current = t;

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
          message: extractErrorMessage(err, tRef.current('recurring.loadError')),
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
        dispatch(showToast({ type: 'success', message: t('recurring.updateSuccess') }));
      } else {
        await recurringApi.createRecurringTransaction(payload);
        dispatch(showToast({ type: 'success', message: t('recurring.createSuccess') }));
      }
      setModalOpen(false);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('recurring.saveError')),
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
        dispatch(showToast({ type: 'success', message: t('recurring.pauseSuccess') }));
      } else if (item.status === 'PAUSED') {
        await recurringApi.resumeRecurringTransaction(item.id);
        dispatch(showToast({ type: 'success', message: t('recurring.resumeSuccess') }));
      }
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('recurring.statusError')),
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
          message: t('recurring.executeSuccess', { description: executeTarget.description }),
        })
      );
      setExecuteTarget(null);
      setAllowEarly(false);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('recurring.executeError')),
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
        dispatch(showToast({ type: 'success', message: t('recurring.cancelSuccess') }));
      } else {
        await recurringApi.deleteRecurringTransaction(confirmTarget.item.id);
        dispatch(showToast({ type: 'success', message: t('recurring.deleteSuccess') }));
      }
      setConfirmTarget(null);
      await loadData();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('recurring.actionError')),
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
            { value: '', label: t('common.all') },
            { value: 'ACTIVE', label: t(RECURRING_STATUS_MAP.ACTIVE.labelKey) },
            { value: 'PAUSED', label: t(RECURRING_STATUS_MAP.PAUSED.labelKey) },
            { value: 'COMPLETED', label: t(RECURRING_STATUS_MAP.COMPLETED.labelKey) },
            { value: 'CANCELLED', label: t(RECURRING_STATUS_MAP.CANCELLED.labelKey) },
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
          {t('recurring.addRecurring')}
        </button>
      </div>

      {loading ? (
        <LoadingSpinner label={t('recurring.loading')} />
      ) : !items.length ? (
        <div className="card-surface">
          <EmptyState
            icon={ArrowPathIcon}
            title={t('recurring.emptyTitle')}
            description={t('recurring.emptyDesc')}
            action={
              <button
                type="button"
                onClick={openCreateModal}
                className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 text-xs font-semibold"
              >
                <PlusIcon className="h-4 w-4" />
                {t('recurring.createFirst')}
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
                        <h3 className="text-sm font-bold text-slate-900 dark:text-white truncate">
                          {item.description}
                        </h3>
                        <p className="text-[11px] text-slate-500 dark:text-slate-400 truncate">
                          {item.accountName}
                          {item.toAccountName ? ` → ${item.toAccountName}` : ''} •{' '}
                          {item.categoryName ||
                            (typeInfo.labelKey ? t(typeInfo.labelKey) : item.transactionType)}
                        </p>
                      </div>
                    </div>

                    <span
                      className={`px-2 py-0.5 rounded-full text-[11px] font-medium border shrink-0 ${statusInfo.badgeClass}`}
                    >
                      {t(statusInfo.labelKey)}
                    </span>
                  </div>

                  <div className="flex flex-wrap items-baseline justify-between gap-2 pt-2 border-t border-slate-100 dark:border-zinc-800">
                    <span
                      className={`text-lg font-bold tabular-nums ${
                        isIncome ? 'text-emerald-600 dark:text-emerald-400' : isExpense ? 'text-rose-600 dark:text-rose-400' : 'text-sky-600 dark:text-sky-400'
                      }`}
                    >
                      {isIncome ? '+' : isExpense ? '-' : ''}
                      {formatCurrency(item.amount, currency)}
                    </span>
                    <span className="text-xs font-medium text-slate-600 dark:text-slate-300 bg-slate-100 dark:bg-zinc-900 px-2.5 py-0.5 rounded-full">
                      {RECURRING_FREQUENCY_LABEL_KEYS[item.frequency]
                        ? t(RECURRING_FREQUENCY_LABEL_KEYS[item.frequency])
                        : item.frequency}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-[11px] text-slate-500 dark:text-slate-400">
                    <div>
                      {t('recurring.nextRun')}:{' '}
                      <strong className="text-slate-700 dark:text-slate-200">{formatDate(item.nextExecutionDate)}</strong>
                    </div>
                    <div>
                      {t('recurring.lastRun')}:{' '}
                      <strong className="text-slate-700 dark:text-slate-200">
                        {item.lastExecutedAt ? formatDateTime(item.lastExecutedAt) : t('recurring.neverRun')}
                      </strong>
                    </div>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-100 dark:border-zinc-800 flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5">
                    {item.status === 'ACTIVE' && (
                      <button
                        type="button"
                        onClick={() => {
                          setAllowEarly(true);
                          setExecuteTarget(item);
                        }}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 text-xs font-semibold"
                      >
                        <PlayIcon className="h-3.5 w-3.5" />
                        {t('recurring.executeNow')}
                      </button>
                    )}
                    {(item.status === 'ACTIVE' || item.status === 'PAUSED') && (
                      <button
                        type="button"
                        onClick={() => handleTogglePauseResume(item)}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 hover:bg-amber-100 dark:hover:bg-amber-900/60 text-xs font-semibold"
                      >
                        {item.status === 'ACTIVE' ? (
                          <>
                            <PauseIcon className="h-3.5 w-3.5" />
                            {t('recurring.pause')}
                          </>
                        ) : (
                          <>
                            <PlayIcon className="h-3.5 w-3.5" />
                            {t('recurring.resume')}
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
                          className="p-1.5 rounded-lg text-slate-400 hover:text-slate-800 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-zinc-800"
                          title={t('common.edit')}
                        >
                          <PencilSquareIcon className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmTarget({ type: 'CANCEL', item })}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-amber-600 dark:hover:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/50"
                          title={t('recurring.cancelRule')}
                        >
                          <NoSymbolIcon className="h-4 w-4" />
                        </button>
                      </>
                    )}
                    <button
                      type="button"
                      onClick={() => setConfirmTarget({ type: 'DELETE', item })}
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
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editingItem ? t('recurring.modalEditTitle') : t('recurring.modalCreateTitle')}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-3 gap-2 p-1 rounded-xl bg-slate-100 dark:bg-zinc-900">
            {TRANSACTION_TYPES.map((item) => (
              <button
                key={item.value}
                type="button"
                onClick={() => handleTypeChange(item.value)}
                className={`py-2 rounded-lg text-xs font-semibold transition-all ${
                  watchedType === item.value
                    ? 'bg-white dark:bg-zinc-800 text-slate-900 dark:text-white shadow-sm'
                    : 'text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-white'
                }`}
              >
                {t(item.labelKey)}
              </button>
            ))}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('recurring.descLabel')} <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              placeholder={t('recurring.descPlaceholder')}
              {...register('description', {
                required: t('recurring.descRequired'),
                maxLength: { value: 255, message: t('recurring.descMax') },
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
            {errors.description && (
              <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.description.message}</p>
            )}
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {watchedType === 'TRANSFER' ? t('recurring.sourceAccount') : t('recurring.account')}{' '}
                <span className="text-rose-500">*</span>
              </label>
              <select
                {...register('accountId', { required: t('recurring.accountRequired') })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 px-3 py-2.5 text-sm bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 outline-none focus:border-emerald-500"
              >
                <option value="">{t('recurring.chooseAccount')}</option>
                {accounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.accountName}
                  </option>
                ))}
              </select>
            </div>

            {watchedType === 'TRANSFER' ? (
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                  {t('recurring.targetAccount')} <span className="text-rose-500">*</span>
                </label>
                <select
                  {...register('toAccountId', {
                    validate: (val) =>
                      watchedType !== 'TRANSFER' ||
                      (Boolean(val) && val !== watchedAccountId) ||
                      t('recurring.targetInvalid'),
                  })}
                  className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 px-3 py-2.5 text-sm bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 outline-none focus:border-emerald-500"
                >
                  <option value="">{t('recurring.chooseTarget')}</option>
                  {accounts
                    .filter((a) => a.id !== watchedAccountId)
                    .map((acc) => (
                      <option key={acc.id} value={acc.id}>
                        {acc.accountName}
                      </option>
                    ))}
                </select>
                {errors.toAccountId && (
                  <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.toAccountId.message}</p>
                )}
              </div>
            ) : (
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                  {t('recurring.category')} <span className="text-rose-500">*</span>
                </label>
                <select
                  {...register('categoryId', {
                    validate: (val) =>
                      watchedType === 'TRANSFER' || Boolean(val) || t('recurring.categoryRequired'),
                  })}
                  className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 px-3 py-2.5 text-sm bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 outline-none focus:border-emerald-500"
                >
                  <option value="">{t('recurring.chooseCategory')}</option>
                  {selectableCategories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
                {errors.categoryId && (
                  <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.categoryId.message}</p>
                )}
              </div>
            )}
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('recurring.amount')} <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                step="0.01"
                {...register('amount', {
                  required: t('recurring.amountRequired'),
                  min: { value: 0.01, message: t('recurring.amountMin') },
                })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm tabular-nums outline-none focus:border-emerald-500"
              />
              {errors.amount && (
                <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.amount.message}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('recurring.frequency')}
              </label>
              <select
                {...register('frequency')}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 px-3 py-2.5 text-sm bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 outline-none focus:border-emerald-500"
              >
                {RECURRING_FREQUENCIES.map((f) => (
                  <option key={f.value} value={f.value}>
                    {t(f.labelKey)}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('recurring.startDate')}
              </label>
              <input
                type="date"
                {...register('startDate', { required: t('recurring.startDateRequired') })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('recurring.endDate')}
              </label>
              <input
                type="date"
                {...register('endDate', {
                  validate: (val) =>
                    !val ||
                    !watchedStartDate ||
                    val >= watchedStartDate ||
                    t('recurring.endDateAfterStart'),
                })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
              />
              {errors.endDate && (
                <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.endDate.message}</p>
              )}
            </div>
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
                : editingItem
                  ? t('recurring.saveBtn')
                  : t('recurring.createBtn')}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={Boolean(executeTarget)}
        onClose={() => setExecuteTarget(null)}
        title={t('recurring.executeModalTitle')}
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
            {t('recurring.executeDescPrefix')}{' '}
            <strong className="text-slate-900 dark:text-white">{executeTarget?.description}</strong>{' '}
            {t('recurring.executeDescMid')}{' '}
            <strong className="text-slate-900 dark:text-white">
              {formatCurrency(executeTarget?.amount, currency)}
            </strong>{' '}
            {t('recurring.executeDescSuffix')}
          </p>

          <label className="flex items-start gap-2.5 rounded-xl border border-slate-200 dark:border-zinc-800 p-3 cursor-pointer">
            <input
              type="checkbox"
              checked={allowEarly}
              onChange={(e) => setAllowEarly(e.target.checked)}
              className="mt-0.5 rounded border-slate-300 dark:border-zinc-700 text-emerald-600 focus:ring-emerald-500"
            />
            <span className="text-xs text-slate-700 dark:text-slate-200">
              {t('recurring.allowEarlyLabel', { date: formatDate(executeTarget?.nextExecutionDate) })}
            </span>
          </label>

          <div className="flex justify-end gap-2.5">
            <button
              type="button"
              onClick={() => setExecuteTarget(null)}
              className="px-4 py-2 rounded-xl border border-slate-200 dark:border-zinc-700 text-xs font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-zinc-800"
            >
              {t('common.cancel')}
            </button>
            <button
              type="button"
              onClick={handleExecuteNow}
              disabled={executing}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold disabled:opacity-60"
            >
              {executing ? t('recurring.executing') : t('recurring.confirmExecute')}
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
        title={
          confirmTarget?.type === 'CANCEL' ? t('recurring.cancelTitle') : t('recurring.deleteTitle')
        }
        message={
          confirmTarget?.type === 'CANCEL'
            ? t('recurring.cancelConfirmMsg', {
                description: confirmTarget?.item?.description || '',
              })
            : t('recurring.deleteConfirmMsg', {
                description: confirmTarget?.item?.description || '',
              })
        }
        confirmLabel={
          confirmTarget?.type === 'CANCEL' ? t('recurring.cancelRule') : t('common.delete')
        }
      />
    </div>
  );
}
