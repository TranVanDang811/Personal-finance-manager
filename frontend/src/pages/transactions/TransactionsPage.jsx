import { useCallback, useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useForm } from 'react-hook-form';
import {
  ArrowsRightLeftIcon,
  FunnelIcon,
  MagnifyingGlassIcon,
  PaperClipIcon,
  PencilSquareIcon,
  PlusIcon,
  TrashIcon,
} from '@heroicons/react/24/outline';
import { transactionApi } from '../../services/transactionApi';
import { accountApi } from '../../services/accountApi';
import { categoryApi } from '../../services/categoryApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import { useDebounce } from '../../hooks/useDebounce';
import CategoryIcon from '../../components/common/CategoryIcon';
import EmptyState from '../../components/common/EmptyState';
import Modal from '../../components/common/Modal';
import Pagination from '../../components/common/Pagination';
import ConfirmDialog from '../../components/feedback/ConfirmDialog';
import LoadingSpinner from '../../components/feedback/LoadingSpinner';
import { TRANSACTION_TYPE_MAP, TRANSACTION_TYPES } from '../../constants/enums';
import { useTranslation } from '../../i18n/I18nContext';
import {
  dateTimeLocalToIso,
  extractErrorMessage,
  formatCurrency,
  formatDateTime,
  generateIdempotencyKey,
  toDateTimeLocalValue,
  validateImageFile,
} from '../../utils/formatters';

export default function TransactionsPage() {
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const tRef = useRef(t);
  tRef.current = t;

  const userCurrency = useSelector((state) => state.auth.user?.currency || 'VND');

  const [accounts, setAccounts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pageData, setPageData] = useState({
    content: [],
    pageNumber: 0,
    pageSize: 15,
    totalElements: 0,
    totalPages: 0,
  });

  const [keyword, setKeyword] = useState('');
  const debouncedKeyword = useDebounce(keyword, 350);
  const [filters, setFilters] = useState({
    transactionType: '',
    accountId: '',
    categoryId: '',
    startDate: '',
    endDate: '',
    page: 0,
  });

  const [modalOpen, setModalOpen] = useState(false);
  const [editingTx, setEditingTx] = useState(null);
  const [idempotencyKey, setIdempotencyKey] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [attachmentTx, setAttachmentTx] = useState(null);
  const [uploadingAttachment, setUploadingAttachment] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

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

  const loadMetadata = useCallback(async () => {
    try {
      const [accRes, catRes] = await Promise.all([
        accountApi.getAccounts(),
        categoryApi.getCategories(),
      ]);
      setAccounts(accRes.data || []);
      setCategories(catRes.data || []);
    } catch {
      // Errors handled when loading transactions
    }
  }, []);

  const loadTransactions = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        keyword: debouncedKeyword.trim() || undefined,
        transactionType: filters.transactionType || undefined,
        accountId: filters.accountId || undefined,
        categoryId: filters.categoryId || undefined,
        startDate: filters.startDate ? new Date(`${filters.startDate}T00:00:00`).toISOString() : undefined,
        endDate: filters.endDate ? new Date(`${filters.endDate}T23:59:59.999`).toISOString() : undefined,
        page: filters.page,
        size: 15,
      };
      const res = await transactionApi.getTransactions(params);
      setPageData(
        res.data || { content: [], pageNumber: 0, pageSize: 15, totalElements: 0, totalPages: 0 }
      );
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, tRef.current('transactions.loadError')),
        })
      );
    } finally {
      setLoading(false);
    }
  }, [dispatch, debouncedKeyword, filters]);

  useEffect(() => {
    loadMetadata();
  }, [loadMetadata]);

  useEffect(() => {
    loadTransactions();
  }, [loadTransactions]);

  const updateFilter = (key, value) => {
    setFilters((prev) => ({ ...prev, [key]: value, page: 0 }));
  };

  const resetFilters = () => {
    setKeyword('');
    setFilters({
      transactionType: '',
      accountId: '',
      categoryId: '',
      startDate: '',
      endDate: '',
      page: 0,
    });
  };

  const activeAccounts = accounts.filter((a) => a.status === 'ACTIVE');

  const openCreateModal = () => {
    setEditingTx(null);
    setSelectedFile(null);
    setIdempotencyKey(generateIdempotencyKey());
    const defaultAccount = activeAccounts[0]?.id || accounts[0]?.id || '';
    const defaultExpenseCat = categories.find((c) => c.categoryType === 'EXPENSE')?.id || '';
    reset({
      transactionType: 'EXPENSE',
      accountId: defaultAccount,
      toAccountId: '',
      categoryId: defaultExpenseCat,
      amount: '',
      transactionDate: toDateTimeLocalValue(new Date()),
      description: '',
      note: '',
    });
    setModalOpen(true);
  };

  const openEditModal = (tx) => {
    setEditingTx(tx);
    setSelectedFile(null);
    reset({
      transactionType: tx.transactionType,
      accountId: tx.accountId,
      toAccountId: tx.toAccountId || '',
      categoryId: tx.categoryId || '',
      amount: tx.amount,
      transactionDate: toDateTimeLocalValue(tx.transactionDate),
      description: tx.description || '',
      note: tx.note || '',
    });
    setModalOpen(true);
  };

  const handleTypeSwitch = (nextType) => {
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
        description: values.description?.trim() || null,
        note: values.note?.trim() || null,
        transactionDate: dateTimeLocalToIso(values.transactionDate),
      };

      let savedTxId = editingTx?.id;
      if (editingTx) {
        await transactionApi.updateTransaction(editingTx.id, payload);
        dispatch(showToast({ type: 'success', message: t('transactions.updateSuccess') }));
      } else {
        const res = await transactionApi.createTransaction(payload, idempotencyKey);
        savedTxId = res.data?.id;
        dispatch(showToast({ type: 'success', message: t('transactions.createSuccess') }));
      }

      if (selectedFile && savedTxId) {
        try {
          await transactionApi.uploadAttachment(savedTxId, selectedFile);
        } catch (uploadErr) {
          dispatch(
            showToast({
              type: 'error',
              message: extractErrorMessage(uploadErr, t('transactions.uploadAfterCreateError')),
            })
          );
        }
      }

      setModalOpen(false);
      await Promise.all([loadTransactions(), loadMetadata()]);
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('transactions.saveError')),
        })
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleSelectAttachmentForCreate = (e) => {
    const file = e.target.files?.[0];
    if (!file) {
      setSelectedFile(null);
      return;
    }
    const validationErr = validateImageFile(file, 5, t);
    if (validationErr) {
      dispatch(showToast({ type: 'error', message: validationErr }));
      e.target.value = '';
      setSelectedFile(null);
      return;
    }
    setSelectedFile(file);
  };

  const handleUploadAttachment = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || !attachmentTx) return;
    const validationErr = validateImageFile(file, 5, t);
    if (validationErr) {
      dispatch(showToast({ type: 'error', message: validationErr }));
      return;
    }
    setUploadingAttachment(true);
    try {
      const res = await transactionApi.uploadAttachment(attachmentTx.id, file);
      setAttachmentTx(res.data);
      dispatch(showToast({ type: 'success', message: t('transactions.uploadSuccess') }));
      await loadTransactions();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('transactions.uploadError')),
        })
      );
    } finally {
      setUploadingAttachment(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await transactionApi.deleteTransaction(deleteTarget.id);
      dispatch(showToast({ type: 'success', message: t('transactions.deleteSuccess') }));
      setDeleteTarget(null);
      await Promise.all([loadTransactions(), loadMetadata()]);
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('transactions.deleteError')),
        })
      );
    } finally {
      setDeleting(false);
    }
  };

  const selectableCategories = categories.filter((c) => c.categoryType === watchedType);
  const selectableAccounts = editingTx ? accounts : activeAccounts;

  return (
    <div className="space-y-5">
      <div className="card-surface p-4 space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="relative flex-1 min-w-[220px]">
            <MagnifyingGlassIcon className="h-4 w-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setFilters((prev) => ({ ...prev, page: 0 }));
              }}
              placeholder={t('transactions.searchPlaceholder')}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 pl-9 pr-3.5 py-2 text-xs focus:border-emerald-500 outline-none"
            />
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={resetFilters}
              className="inline-flex items-center gap-1 rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2 text-xs font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-zinc-800"
            >
              <FunnelIcon className="h-3.5 w-3.5" />
              {t('common.reset')}
            </button>
            <button
              type="button"
              onClick={openCreateModal}
              className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 text-xs font-semibold transition-colors"
            >
              <PlusIcon className="h-4 w-4" />
              {t('transactions.addTx')}
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-2.5">
          <select
            value={filters.transactionType}
            onChange={(e) => updateFilter('transactionType', e.target.value)}
            className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2 text-xs text-slate-700 dark:text-slate-200 outline-none"
          >
            <option value="">{t('transactions.allTypes')}</option>
            {TRANSACTION_TYPES.map((item) => (
              <option key={item.value} value={item.value}>
                {t(item.labelKey)}
              </option>
            ))}
          </select>

          <select
            value={filters.accountId}
            onChange={(e) => updateFilter('accountId', e.target.value)}
            className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2 text-xs text-slate-700 dark:text-slate-200 outline-none"
          >
            <option value="">{t('transactions.allAccounts')}</option>
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.accountName}
              </option>
            ))}
          </select>

          <select
            value={filters.categoryId}
            onChange={(e) => updateFilter('categoryId', e.target.value)}
            className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2 text-xs text-slate-700 dark:text-slate-200 outline-none"
          >
            <option value="">{t('transactions.allCategories')}</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} ({c.categoryType === 'INCOME' ? t('reports.incomeShort') : t('reports.expenseShort')})
              </option>
            ))}
          </select>

          <input
            type="date"
            value={filters.startDate}
            onChange={(e) => updateFilter('startDate', e.target.value)}
            className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2 text-xs text-slate-700 dark:text-slate-200 outline-none"
            title={t('transactions.fromDateTitle')}
          />

          <input
            type="date"
            value={filters.endDate}
            onChange={(e) => updateFilter('endDate', e.target.value)}
            className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2 text-xs text-slate-700 dark:text-slate-200 outline-none"
            title={t('transactions.toDateTitle')}
          />
        </div>
      </div>

      <div className="card-surface overflow-hidden">
        {loading ? (
          <LoadingSpinner label={t('transactions.loading')} />
        ) : !pageData.content?.length ? (
          <EmptyState
            icon={ArrowsRightLeftIcon}
            title={t('transactions.emptyTitle')}
            description={t('transactions.emptyDesc')}
          />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-100 dark:border-zinc-800 bg-slate-50/70 dark:bg-zinc-900/70 text-[11px] font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    <th className="py-3 px-4">{t('transactions.colCategoryDesc')}</th>
                    <th className="py-3 px-4">{t('transactions.colAccount')}</th>
                    <th className="py-3 px-4">{t('transactions.colType')}</th>
                    <th className="py-3 px-4">{t('transactions.colTime')}</th>
                    <th className="py-3 px-4 text-right">{t('transactions.colAmount')}</th>
                    <th className="py-3 px-4 text-right">{t('transactions.colActions')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-zinc-800 text-xs">
                  {pageData.content.map((tx) => {
                    const typeInfo = TRANSACTION_TYPE_MAP[tx.transactionType] || {};
                    const isExpense = tx.transactionType === 'EXPENSE';
                    const isIncome = tx.transactionType === 'INCOME';
                    return (
                      <tr key={tx.id} className="hover:bg-slate-50/70 dark:hover:bg-zinc-900/60 transition-colors">
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-3">
                            <div
                              className="h-9 w-9 rounded-xl flex items-center justify-center shrink-0"
                              style={{
                                backgroundColor: tx.categoryColor ? `${tx.categoryColor}20` : '#E0F2FE',
                              }}
                            >
                              <CategoryIcon
                                icon={tx.categoryIcon}
                                color={tx.categoryColor || '#0284C7'}
                                className="h-4 w-4"
                              />
                            </div>
                            <div className="min-w-0 max-w-xs">
                              <p className="font-semibold text-slate-900 dark:text-white truncate">
                                {tx.description || tx.categoryName || t('transactions.internalTransfer')}
                              </p>
                              <div className="flex items-center gap-2 text-[11px] text-slate-500 dark:text-slate-400">
                                <span>{tx.categoryName || t('transactions.transferCategory')}</span>
                                {tx.recurringTransactionId && (
                                  <span className="px-1.5 py-0.2 rounded bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300 font-medium">
                                    {t('transactions.recurringBadge')}
                                  </span>
                                )}
                                {tx.attachmentUrl && (
                                  <PaperClipIcon
                                    className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400"
                                    title={t('transactions.hasAttachmentTitle')}
                                  />
                                )}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="py-3 px-4 text-slate-700 dark:text-slate-200 font-medium">
                          {tx.accountName}
                          {tx.toAccountName && (
                            <span className="text-slate-400 dark:text-slate-500"> → {tx.toAccountName}</span>
                          )}
                        </td>
                        <td className="py-3 px-4">
                          <span
                            className={`inline-block px-2 py-0.5 rounded-full text-[11px] font-medium border ${
                              typeInfo.badgeClass || 'bg-slate-50 dark:bg-zinc-900 text-slate-600 dark:text-slate-300 border-slate-200 dark:border-zinc-700'
                            }`}
                          >
                            {typeInfo.labelKey ? t(typeInfo.labelKey) : tx.transactionType}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-slate-500 dark:text-slate-400 whitespace-nowrap tabular-nums">
                          {formatDateTime(tx.transactionDate)}
                        </td>
                        <td
                          className={`py-3 px-4 text-right font-bold tabular-nums whitespace-nowrap ${
                            isIncome ? 'text-emerald-600 dark:text-emerald-400' : isExpense ? 'text-rose-600 dark:text-rose-400' : 'text-sky-600 dark:text-sky-400'
                          }`}
                        >
                          {isIncome ? '+' : isExpense ? '-' : ''}
                          {formatCurrency(tx.amount, userCurrency)}
                        </td>
                        <td className="py-3 px-4 text-right whitespace-nowrap">
                          <div className="inline-flex items-center gap-1">
                            <button
                              type="button"
                              onClick={() => setAttachmentTx(tx)}
                              className={`p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-zinc-800 ${
                                tx.attachmentUrl
                                  ? 'text-emerald-600 dark:text-emerald-400'
                                  : 'text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
                              }`}
                              title={t('transactions.attachmentBtnTitle')}
                            >
                              <PaperClipIcon className="h-4 w-4" />
                            </button>
                            <button
                              type="button"
                              onClick={() => openEditModal(tx)}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-800 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-zinc-800"
                              title={t('common.edit')}
                            >
                              <PencilSquareIcon className="h-4 w-4" />
                            </button>
                            <button
                              type="button"
                              onClick={() => setDeleteTarget(tx)}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/50"
                              title={t('common.delete')}
                            >
                              <TrashIcon className="h-4 w-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            <Pagination
              pageNumber={pageData.pageNumber}
              totalPages={pageData.totalPages}
              totalElements={pageData.totalElements}
              pageSize={pageData.pageSize}
              onPageChange={(nextPage) => setFilters((prev) => ({ ...prev, page: nextPage }))}
            />
          </>
        )}
      </div>

      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editingTx ? t('transactions.modalEditTitle') : t('transactions.modalCreateTitle')}
        subtitle={t('transactions.modalSubtitle')}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-3 gap-2 p-1 rounded-xl bg-slate-100 dark:bg-zinc-900">
            {TRANSACTION_TYPES.map((item) => {
              const active = watchedType === item.value;
              return (
                <button
                  key={item.value}
                  type="button"
                  onClick={() => handleTypeSwitch(item.value)}
                  className={`py-2 rounded-lg text-xs font-semibold transition-all ${
                    active
                      ? 'bg-white dark:bg-zinc-800 text-slate-900 dark:text-white shadow-sm'
                      : 'text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-white'
                  }`}
                >
                  {t(item.labelKey)}
                </button>
              );
            })}
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {watchedType === 'TRANSFER'
                  ? t('transactions.sourceAccount')
                  : t('transactions.account')}{' '}
                <span className="text-rose-500">*</span>
              </label>
              <select
                {...register('accountId', { required: t('transactions.accountRequired') })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 px-3 py-2.5 text-sm bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 outline-none focus:border-emerald-500"
              >
                <option value="">{t('transactions.chooseAccount')}</option>
                {selectableAccounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.accountName} ({formatCurrency(acc.currentBalance, acc.currency)})
                  </option>
                ))}
              </select>
              {errors.accountId && (
                <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.accountId.message}</p>
              )}
            </div>

            {watchedType === 'TRANSFER' ? (
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                  {t('transactions.targetAccount')} <span className="text-rose-500">*</span>
                </label>
                <select
                  {...register('toAccountId', {
                    validate: (val) =>
                      watchedType !== 'TRANSFER' ||
                      (Boolean(val) && val !== watchedAccountId) ||
                      t('transactions.targetInvalid'),
                  })}
                  className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 px-3 py-2.5 text-sm bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 outline-none focus:border-emerald-500"
                >
                  <option value="">{t('transactions.chooseTarget')}</option>
                  {selectableAccounts
                    .filter((acc) => acc.id !== watchedAccountId)
                    .map((acc) => (
                      <option key={acc.id} value={acc.id}>
                        {acc.accountName} ({formatCurrency(acc.currentBalance, acc.currency)})
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
                  {t('transactions.category')} <span className="text-rose-500">*</span>
                </label>
                <select
                  {...register('categoryId', {
                    validate: (val) =>
                      watchedType === 'TRANSFER' || Boolean(val) || t('transactions.categoryRequired'),
                  })}
                  className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 px-3 py-2.5 text-sm bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 outline-none focus:border-emerald-500"
                >
                  <option value="">{t('transactions.chooseCategory')}</option>
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
                {t('transactions.amount')} <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                step="0.01"
                placeholder="0"
                {...register('amount', {
                  required: t('transactions.amountRequired'),
                  min: { value: 0.01, message: t('transactions.amountMin') },
                })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm tabular-nums outline-none focus:border-emerald-500"
              />
              {errors.amount && (
                <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.amount.message}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('transactions.time')} <span className="text-rose-500">*</span>
              </label>
              <input
                type="datetime-local"
                {...register('transactionDate', { required: t('transactions.timeRequired') })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
              />
              {errors.transactionDate && (
                <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.transactionDate.message}</p>
              )}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('transactions.description')}
            </label>
            <input
              type="text"
              placeholder={t('transactions.descriptionPlaceholder')}
              {...register('description', {
                maxLength: { value: 255, message: t('transactions.descriptionMax') },
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('transactions.note')}
            </label>
            <textarea
              rows={2}
              placeholder={t('transactions.notePlaceholder')}
              {...register('note', { maxLength: { value: 1000, message: t('transactions.noteMax') } })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2 text-sm outline-none focus:border-emerald-500"
            />
          </div>

          {!editingTx && (
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('transactions.attachmentOptional')}
              </label>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={handleSelectAttachmentForCreate}
                className="w-full text-xs text-slate-500 dark:text-slate-400 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-slate-100 dark:file:bg-zinc-800 file:text-slate-700 dark:file:text-slate-200 hover:file:bg-slate-200 dark:hover:file:bg-zinc-700"
              />
              <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-1">{t('common.uploadMaxHint')}</p>
            </div>
          )}

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
                : editingTx
                  ? t('common.update')
                  : t('transactions.saveTxBtn')}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={Boolean(attachmentTx)}
        onClose={() => setAttachmentTx(null)}
        title={t('transactions.attachmentModalTitle')}
        subtitle={
          attachmentTx?.description ||
          attachmentTx?.categoryName ||
          t('transactions.attachmentModalFallback')
        }
      >
        <div className="space-y-4">
          {attachmentTx?.attachmentUrl ? (
            <div className="rounded-xl border border-slate-200 dark:border-zinc-800 p-3 bg-slate-50 dark:bg-zinc-900 space-y-2">
              {(/\.(png|jpe?g|gif|webp)(\?.*)?$/i.test(attachmentTx.attachmentUrl) ||
                attachmentTx.attachmentUrl.includes('/image/upload/')) ? (
                <img
                  src={attachmentTx.attachmentUrl}
                  alt={t('transactions.attachmentAlt')}
                  className="max-h-72 w-auto mx-auto rounded-lg object-contain"
                />
              ) : null}
              <div className="text-center">
                <a
                  href={attachmentTx.attachmentUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="text-xs font-semibold text-emerald-600 dark:text-emerald-400 hover:underline"
                >
                  {t('transactions.openImageNewTab')}
                </a>
              </div>
            </div>
          ) : (
            <p className="text-xs text-slate-500 dark:text-slate-400">{t('transactions.noAttachmentYet')}</p>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {attachmentTx?.attachmentUrl
                ? t('transactions.replaceImage')
                : t('transactions.uploadImage')}
            </label>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              disabled={uploadingAttachment}
              onChange={handleUploadAttachment}
              className="w-full text-xs text-slate-500 dark:text-slate-400 file:mr-3 file:py-2 file:px-3.5 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-emerald-50 dark:file:bg-emerald-950/60 file:text-emerald-700 dark:file:text-emerald-300 hover:file:bg-emerald-100 dark:hover:file:bg-emerald-900/60"
            />
            <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-1">{t('common.uploadMaxHint')}</p>
            {uploadingAttachment && (
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-2">{t('transactions.uploadingCloudinary')}</p>
            )}
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        loading={deleting}
        title={t('transactions.deleteTitle')}
        message={t('transactions.deleteConfirmMsg')}
        confirmLabel={t('transactions.deleteConfirmBtn')}
      />
    </div>
  );
}
