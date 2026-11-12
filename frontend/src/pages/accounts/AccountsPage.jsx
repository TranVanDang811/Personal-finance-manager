import { useCallback, useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useForm } from 'react-hook-form';
import {
  ArchiveBoxArrowDownIcon,
  ArchiveBoxXMarkIcon,
  ArrowsRightLeftIcon,
  BanknotesIcon,
  BuildingLibraryIcon,
  CreditCardIcon,
  PencilSquareIcon,
  PlusIcon,
  TrashIcon,
} from '@heroicons/react/24/outline';
import { accountApi } from '../../services/accountApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import Modal from '../../components/common/Modal';
import Pagination from '../../components/common/Pagination';
import EmptyState from '../../components/common/EmptyState';
import ConfirmDialog from '../../components/feedback/ConfirmDialog';
import LoadingSpinner from '../../components/feedback/LoadingSpinner';
import {
  ACCOUNT_STATUS_LABEL_KEYS,
  ACCOUNT_TYPE_LABEL_KEYS,
  ACCOUNT_TYPES,
  CURRENCIES,
  TRANSACTION_TYPE_MAP,
} from '../../constants/enums';
import { useTranslation } from '../../i18n/I18nContext';
import { extractErrorMessage, formatCurrency, formatDateTime } from '../../utils/formatters';

function getAccountIcon(type) {
  if (type === 'BANK_ACCOUNT') return BuildingLibraryIcon;
  if (type === 'SAVINGS') return BanknotesIcon;
  return CreditCardIcon;
}

export default function AccountsPage() {
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const tRef = useRef(t);
  tRef.current = t;

  const userCurrency = useSelector((state) => state.auth.user?.currency || 'VND');

  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');

  const [modalOpen, setModalOpen] = useState(false);
  const [editingAccount, setEditingAccount] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const [txAccount, setTxAccount] = useState(null);
  const [txLoading, setTxLoading] = useState(false);
  const [txPageData, setTxPageData] = useState({
    content: [],
    pageNumber: 0,
    totalPages: 0,
    totalElements: 0,
    pageSize: 8,
  });

  const {
    register,
    watch,
    setValue,
    reset,
    handleSubmit,
    formState: { errors },
  } = useForm();

  const watchedType = watch('accountType');

  useEffect(() => {
    if (watchedType && watchedType !== 'BANK_ACCOUNT') {
      setValue('allowNegative', false);
    }
  }, [watchedType, setValue]);

  const loadAccounts = useCallback(async () => {
    setLoading(true);
    try {
      const res = await accountApi.getAccountSummary();
      setSummary(res.data || null);
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, tRef.current('accounts.loadError')),
        })
      );
    } finally {
      setLoading(false);
    }
  }, [dispatch]);

  useEffect(() => {
    loadAccounts();
  }, [loadAccounts]);

  const openCreateModal = () => {
    setEditingAccount(null);
    reset({
      accountName: '',
      accountType: 'CASH',
      initialBalance: 0,
      currency: userCurrency,
      description: '',
      allowNegative: false,
    });
    setModalOpen(true);
  };

  const openEditModal = (acc) => {
    setEditingAccount(acc);
    reset({
      accountName: acc.accountName,
      accountType: acc.accountType,
      description: acc.description || '',
      allowNegative: Boolean(acc.allowNegative),
      status: acc.status,
    });
    setModalOpen(true);
  };

  const onSubmit = async (values) => {
    setSubmitting(true);
    try {
      if (editingAccount) {
        await accountApi.updateAccount(editingAccount.id, {
          accountName: values.accountName.trim(),
          accountType: values.accountType,
          description: values.description?.trim() || null,
          allowNegative: values.accountType === 'BANK_ACCOUNT' ? Boolean(values.allowNegative) : false,
          status: editingAccount.status,
        });
        dispatch(showToast({ type: 'success', message: t('accounts.updateSuccess') }));
      } else {
        await accountApi.createAccount({
          accountName: values.accountName.trim(),
          accountType: values.accountType,
          initialBalance: Number(values.initialBalance ?? 0),
          currency: values.currency || userCurrency,
          description: values.description?.trim() || null,
          allowNegative: values.accountType === 'BANK_ACCOUNT' ? Boolean(values.allowNegative) : false,
        });
        dispatch(showToast({ type: 'success', message: t('accounts.createSuccess') }));
      }
      setModalOpen(false);
      await loadAccounts();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('accounts.saveError')),
        })
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleArchive = async (acc) => {
    try {
      if (acc.status === 'ACTIVE') {
        await accountApi.archiveAccount(acc.id);
        dispatch(
          showToast({
            type: 'success',
            message: t('accounts.archiveSuccess', { name: acc.accountName }),
          })
        );
      } else {
        await accountApi.unarchiveAccount(acc.id);
        dispatch(
          showToast({
            type: 'success',
            message: t('accounts.unarchiveSuccess', { name: acc.accountName }),
          })
        );
      }
      await loadAccounts();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('accounts.statusError')),
        })
      );
    }
  };

  const handleDeleteAccount = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await accountApi.deleteAccount(deleteTarget.id);
      dispatch(
        showToast({
          type: 'success',
          message: t('accounts.deleteSuccess', { name: deleteTarget.accountName }),
        })
      );
      setDeleteTarget(null);
      await loadAccounts();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('accounts.deleteError')),
        })
      );
    } finally {
      setDeleting(false);
    }
  };

  const loadAccountTransactions = async (acc, page = 0) => {
    setTxAccount(acc);
    setTxLoading(true);
    try {
      const res = await accountApi.getAccountTransactions(acc.id, { page, size: 8 });
      setTxPageData(
        res.data || { content: [], pageNumber: 0, totalPages: 0, totalElements: 0, pageSize: 8 }
      );
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('accounts.historyError')),
        })
      );
    } finally {
      setTxLoading(false);
    }
  };

  const accounts = (summary?.accounts || []).filter((acc) => {
    const matchStatus = statusFilter === 'ALL' || acc.status === statusFilter;
    const matchType = typeFilter === 'ALL' || acc.accountType === typeFilter;
    return matchStatus && matchType;
  });

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('accounts.totalActiveBalance')}</p>
          <p className="mt-2 text-2xl font-bold text-slate-900 dark:text-white tabular-nums">
            {formatCurrency(summary?.totalActiveBalance, userCurrency)}
          </p>
        </div>
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('accounts.activeCount')}</p>
          <p className="mt-2 text-2xl font-bold text-emerald-600 dark:text-emerald-400 tabular-nums">
            {summary?.activeAccountCount ?? 0}
          </p>
        </div>
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('accounts.archivedCount')}</p>
          <p className="mt-2 text-2xl font-bold text-slate-500 dark:text-slate-400 tabular-nums">
            {summary?.archivedAccountCount ?? 0}
          </p>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          {[
            { value: 'ALL', label: t('common.all') },
            { value: 'ACTIVE', label: t(ACCOUNT_STATUS_LABEL_KEYS.ACTIVE) },
            { value: 'ARCHIVED', label: t(ACCOUNT_STATUS_LABEL_KEYS.ARCHIVED) },
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

          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2 text-xs font-medium text-slate-700 dark:text-slate-200 outline-none focus:border-emerald-500"
          >
            <option value="ALL">{t('accounts.allTypes')}</option>
            {ACCOUNT_TYPES.map((item) => (
              <option key={item.value} value={item.value}>
                {t(item.labelKey)}
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
          {t('accounts.addAccount')}
        </button>
      </div>

      {loading ? (
        <LoadingSpinner label={t('accounts.loading')} />
      ) : !accounts.length ? (
        <div className="card-surface">
          <EmptyState
            icon={CreditCardIcon}
            title={t('accounts.emptyTitle')}
            description={t('accounts.emptyDesc')}
            action={
              <button
                type="button"
                onClick={openCreateModal}
                className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 text-xs font-semibold"
              >
                <PlusIcon className="h-4 w-4" />
                {t('accounts.createFirst')}
              </button>
            }
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {accounts.map((acc) => {
            const Icon = getAccountIcon(acc.accountType);
            const isArchived = acc.status === 'ARCHIVED';
            return (
              <div
                key={acc.id}
                className={`card-surface p-5 flex flex-col justify-between transition-opacity ${
                  isArchived ? 'opacity-75 bg-slate-50/70 dark:bg-zinc-900/70' : ''
                }`}
              >
                <div>
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="h-10 w-10 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0">
                        <Icon className="h-5 w-5" />
                      </div>
                      <div className="min-w-0">
                        <h3 className="text-sm font-bold text-slate-900 dark:text-white truncate">{acc.accountName}</h3>
                        <p className="text-[11px] text-slate-500 dark:text-slate-400">
                          {ACCOUNT_TYPE_LABEL_KEYS[acc.accountType]
                            ? t(ACCOUNT_TYPE_LABEL_KEYS[acc.accountType])
                            : acc.accountType}
                        </p>
                      </div>
                    </div>
                    <span
                      className={`px-2 py-0.5 rounded-full text-[11px] font-medium border shrink-0 ${
                        isArchived
                          ? 'bg-slate-100 text-slate-600 border-slate-200 dark:bg-zinc-900 dark:text-slate-400 dark:border-zinc-700'
                          : 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800'
                      }`}
                    >
                      {ACCOUNT_STATUS_LABEL_KEYS[acc.status]
                        ? t(ACCOUNT_STATUS_LABEL_KEYS[acc.status])
                        : acc.status}
                    </span>
                  </div>

                  <div className="mt-4 pt-4 border-t border-slate-100 dark:border-zinc-800">
                    <p className="text-[11px] text-slate-500 dark:text-slate-400">{t('accounts.currentBalance')}</p>
                    <p
                      className={`mt-0.5 text-xl font-bold tabular-nums ${
                        Number(acc.currentBalance ?? 0) < 0 ? 'text-rose-600 dark:text-rose-400' : 'text-slate-900 dark:text-white'
                      }`}
                    >
                      {formatCurrency(acc.currentBalance, acc.currency)}
                    </p>
                    <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-1 tabular-nums">
                      {t('accounts.initialBalance')}: {formatCurrency(acc.initialBalance, acc.currency)}
                      {acc.allowNegative && ` • ${t('accounts.allowOverdraft')}`}
                    </p>
                    {acc.description && (
                      <p className="text-xs text-slate-600 dark:text-slate-300 mt-2 line-clamp-2">{acc.description}</p>
                    )}
                  </div>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-100 dark:border-zinc-800 flex items-center justify-between gap-2">
                  <button
                    type="button"
                    onClick={() => loadAccountTransactions(acc, 0)}
                    className="inline-flex items-center gap-1 text-xs font-semibold text-sky-600 dark:text-sky-400 hover:text-sky-700 dark:hover:text-sky-300"
                  >
                    <ArrowsRightLeftIcon className="h-3.5 w-3.5" />
                    {t('accounts.transactionsBtn')}
                  </button>

                  <div className="flex items-center gap-1">
                    <button
                      type="button"
                      onClick={() => openEditModal(acc)}
                      className="p-1.5 rounded-lg text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-zinc-800"
                      title={t('common.edit')}
                    >
                      <PencilSquareIcon className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => handleToggleArchive(acc)}
                      className="p-1.5 rounded-lg text-slate-500 dark:text-slate-400 hover:text-amber-700 dark:hover:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/50"
                      title={isArchived ? t('accounts.unarchiveTitle') : t('accounts.archiveTitle')}
                    >
                      {isArchived ? (
                        <ArchiveBoxXMarkIcon className="h-4 w-4" />
                      ) : (
                        <ArchiveBoxArrowDownIcon className="h-4 w-4" />
                      )}
                    </button>
                    <button
                      type="button"
                      onClick={() => setDeleteTarget(acc)}
                      className="p-1.5 rounded-lg text-slate-500 dark:text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/50"
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
        title={editingAccount ? t('accounts.modalEditTitle') : t('accounts.modalCreateTitle')}
        subtitle={t('accounts.modalSubtitle')}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('accounts.nameLabel')} <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              placeholder={t('accounts.namePlaceholder')}
              {...register('accountName', {
                required: t('accounts.nameRequired'),
                maxLength: { value: 100, message: t('accounts.nameMax') },
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
            />
            {errors.accountName && (
              <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.accountName.message}</p>
            )}
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('accounts.typeLabel')}
              </label>
              <select
                {...register('accountType', { required: true })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 px-3 py-2.5 text-sm bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 focus:border-emerald-500 outline-none"
              >
                {ACCOUNT_TYPES.map((item) => (
                  <option key={item.value} value={item.value}>
                    {t(item.labelKey)}
                  </option>
                ))}
              </select>
            </div>

            {!editingAccount && (
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                  {t('accounts.currencyLabel')}
                </label>
                <select
                  {...register('currency')}
                  className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 px-3 py-2.5 text-sm bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 focus:border-emerald-500 outline-none"
                >
                  {CURRENCIES.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {!editingAccount && (
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('accounts.initialBalanceLabel')}
              </label>
              <input
                type="number"
                step="0.01"
                {...register('initialBalance', {
                  required: t('accounts.initialBalanceRequired'),
                  validate: (val) => {
                    const num = Number(val);
                    if (Number.isNaN(num)) return t('accounts.invalidBalance');
                    if (num < 0 && watchedType !== 'BANK_ACCOUNT') {
                      return t('accounts.negativeBankOnly');
                    }
                    return true;
                  },
                })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm tabular-nums focus:border-emerald-500 outline-none"
              />
              {errors.initialBalance && (
                <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.initialBalance.message}</p>
              )}
            </div>
          )}

          <label className="flex items-start gap-2.5 rounded-xl border border-slate-200 dark:border-zinc-800 p-3 cursor-pointer">
            <input
              type="checkbox"
              disabled={watchedType !== 'BANK_ACCOUNT'}
              {...register('allowNegative')}
              className="mt-0.5 rounded border-slate-300 dark:border-zinc-700 text-emerald-600 focus:ring-emerald-500 disabled:opacity-40"
            />
            <div>
              <span className="text-xs font-semibold text-slate-800 dark:text-slate-200 block">
                {t('accounts.allowNegativeTitle')}
              </span>
              <span className="text-[11px] text-slate-500 dark:text-slate-400">{t('accounts.allowNegativeDesc')}</span>
            </div>
          </label>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('accounts.descLabel')}
            </label>
            <textarea
              rows={2}
              placeholder={t('accounts.descPlaceholder')}
              {...register('description', {
                maxLength: { value: 255, message: t('accounts.descMax') },
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2 text-sm focus:border-emerald-500 outline-none"
            />
            {errors.description && (
              <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.description.message}</p>
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
                : editingAccount
                  ? t('accounts.saveBtn')
                  : t('accounts.createBtn')}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={Boolean(txAccount)}
        onClose={() => setTxAccount(null)}
        title={t('accounts.historyTitle', { name: txAccount?.accountName || '' })}
        subtitle={
          txAccount
            ? t('accounts.historySubtitle', {
                balance: formatCurrency(txAccount.currentBalance, txAccount.currency),
              })
            : ''
        }
        size="lg"
      >
        {txLoading ? (
          <LoadingSpinner label={t('accounts.historyLoading')} />
        ) : !txPageData.content?.length ? (
          <EmptyState
            title={t('accounts.historyEmptyTitle')}
            description={t('accounts.historyEmptyDesc')}
          />
        ) : (
          <div className="space-y-2">
            <div className="divide-y divide-slate-100 dark:divide-zinc-800">
              {txPageData.content.map((tx) => {
                const isIncomingTransfer =
                  tx.transactionType === 'TRANSFER' && tx.toAccountId === txAccount?.id;
                const isCredit = tx.transactionType === 'INCOME' || isIncomingTransfer;
                const typeInfo = TRANSACTION_TYPE_MAP[tx.transactionType] || {};
                return (
                  <div key={tx.id} className="py-3 flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-slate-900 dark:text-white truncate">
                        {tx.description || tx.categoryName || t('accounts.transferFallback')}
                      </p>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400">
                        {typeInfo.labelKey ? t(typeInfo.labelKey) : tx.transactionType} •{' '}
                        {formatDateTime(tx.transactionDate)}
                      </p>
                    </div>
                    <span
                      className={`text-xs font-bold tabular-nums shrink-0 ${
                        isCredit ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'
                      }`}
                    >
                      {isCredit ? '+' : '-'}
                      {formatCurrency(tx.amount, txAccount?.currency || userCurrency)}
                    </span>
                  </div>
                );
              })}
            </div>
            <Pagination
              pageNumber={txPageData.pageNumber}
              totalPages={txPageData.totalPages}
              totalElements={txPageData.totalElements}
              pageSize={txPageData.pageSize}
              onPageChange={(nextPage) => loadAccountTransactions(txAccount, nextPage)}
            />
          </div>
        )}
      </Modal>

      <ConfirmDialog
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteAccount}
        loading={deleting}
        title={t('accounts.deleteConfirmTitle')}
        message={t('accounts.deleteConfirmMsg', { name: deleteTarget?.accountName || '' })}
        confirmLabel={t('accounts.deleteConfirmBtn')}
      />
    </div>
  );
}
