import { useCallback, useEffect, useState } from 'react';
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
          message: extractErrorMessage(err, 'Không thể tải danh sách giao dịch.'),
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
        dispatch(showToast({ type: 'success', message: 'Cập nhật giao dịch thành công.' }));
      } else {
        const res = await transactionApi.createTransaction(payload, idempotencyKey);
        savedTxId = res.data?.id;
        dispatch(showToast({ type: 'success', message: 'Ghi nhận giao dịch thành công.' }));
      }

      if (selectedFile && savedTxId) {
        try {
          await transactionApi.uploadAttachment(savedTxId, selectedFile);
        } catch (uploadErr) {
          dispatch(
            showToast({
              type: 'error',
              message: extractErrorMessage(
                uploadErr,
                'Đã lưu giao dịch nhưng không thể tải lên ảnh chứng từ.'
              ),
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
          message: extractErrorMessage(err, 'Không thể lưu giao dịch.'),
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
    const validationErr = validateImageFile(file, 5);
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
    const validationErr = validateImageFile(file, 5);
    if (validationErr) {
      dispatch(showToast({ type: 'error', message: validationErr }));
      return;
    }
    setUploadingAttachment(true);
    try {
      const res = await transactionApi.uploadAttachment(attachmentTx.id, file);
      setAttachmentTx(res.data);
      dispatch(showToast({ type: 'success', message: 'Tải lên chứng từ thành công.' }));
      await loadTransactions();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể tải tệp đính kèm.'),
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
      dispatch(showToast({ type: 'success', message: 'Đã xóa giao dịch.' }));
      setDeleteTarget(null);
      await Promise.all([loadTransactions(), loadMetadata()]);
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể xóa giao dịch.'),
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
              placeholder="Tìm kiếm theo mô tả, ghi chú..."
              className="w-full rounded-xl border border-slate-200 pl-9 pr-3.5 py-2 text-xs focus:border-emerald-500 outline-none"
            />
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={resetFilters}
              className="inline-flex items-center gap-1 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-600 hover:bg-slate-50"
            >
              <FunnelIcon className="h-3.5 w-3.5" />
              Đặt lại
            </button>
            <button
              type="button"
              onClick={openCreateModal}
              className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 text-xs font-semibold transition-colors"
            >
              <PlusIcon className="h-4 w-4" />
              Thêm giao dịch
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-2.5">
          <select
            value={filters.transactionType}
            onChange={(e) => updateFilter('transactionType', e.target.value)}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-700 outline-none"
          >
            <option value="">Tất cả loại giao dịch</option>
            {TRANSACTION_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>

          <select
            value={filters.accountId}
            onChange={(e) => updateFilter('accountId', e.target.value)}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-700 outline-none"
          >
            <option value="">Tất cả tài khoản</option>
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.accountName}
              </option>
            ))}
          </select>

          <select
            value={filters.categoryId}
            onChange={(e) => updateFilter('categoryId', e.target.value)}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-700 outline-none"
          >
            <option value="">Tất cả danh mục</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} ({c.categoryType === 'INCOME' ? 'Thu' : 'Chi'})
              </option>
            ))}
          </select>

          <input
            type="date"
            value={filters.startDate}
            onChange={(e) => updateFilter('startDate', e.target.value)}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-700 outline-none"
            title="Từ ngày"
          />

          <input
            type="date"
            value={filters.endDate}
            onChange={(e) => updateFilter('endDate', e.target.value)}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-700 outline-none"
            title="Đến ngày"
          />
        </div>
      </div>

      <div className="card-surface overflow-hidden">
        {loading ? (
          <LoadingSpinner label="Đang tải danh sách giao dịch..." />
        ) : !pageData.content?.length ? (
          <EmptyState
            icon={ArrowsRightLeftIcon}
            title="Không tìm thấy giao dịch nào"
            description="Thử thay đổi bộ lọc hoặc ghi nhận một giao dịch thu chi mới."
          />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50/70 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                    <th className="py-3 px-4">Danh mục / Mô tả</th>
                    <th className="py-3 px-4">Tài khoản</th>
                    <th className="py-3 px-4">Loại</th>
                    <th className="py-3 px-4">Thời gian</th>
                    <th className="py-3 px-4 text-right">Số tiền</th>
                    <th className="py-3 px-4 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-xs">
                  {pageData.content.map((tx) => {
                    const typeInfo = TRANSACTION_TYPE_MAP[tx.transactionType] || {};
                    const isExpense = tx.transactionType === 'EXPENSE';
                    const isIncome = tx.transactionType === 'INCOME';
                    return (
                      <tr key={tx.id} className="hover:bg-slate-50/70 transition-colors">
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-3">
                            <div
                              className="h-9 w-9 rounded-xl flex items-center justify-center shrink-0"
                              style={{
                                backgroundColor: tx.categoryColor ? `${tx.categoryColor}18` : '#E0F2FE',
                              }}
                            >
                              <CategoryIcon
                                icon={tx.categoryIcon}
                                color={tx.categoryColor || '#0284C7'}
                                className="h-4 w-4"
                              />
                            </div>
                            <div className="min-w-0 max-w-xs">
                              <p className="font-semibold text-slate-900 truncate">
                                {tx.description || tx.categoryName || 'Chuyển khoản nội bộ'}
                              </p>
                              <div className="flex items-center gap-2 text-[11px] text-slate-500">
                                <span>{tx.categoryName || 'Chuyển khoản'}</span>
                                {tx.recurringTransactionId && (
                                  <span className="px-1.5 py-0.2 rounded bg-indigo-50 text-indigo-600 font-medium">
                                    Định kỳ
                                  </span>
                                )}
                                {tx.attachmentUrl && (
                                  <PaperClipIcon className="h-3.5 w-3.5 text-emerald-600" title="Có chứng từ" />
                                )}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="py-3 px-4 text-slate-700 font-medium">
                          {tx.accountName}
                          {tx.toAccountName && (
                            <span className="text-slate-400"> → {tx.toAccountName}</span>
                          )}
                        </td>
                        <td className="py-3 px-4">
                          <span
                            className={`inline-block px-2 py-0.5 rounded-full text-[11px] font-medium border ${
                              typeInfo.badgeClass || 'bg-slate-50 text-slate-600 border-slate-200'
                            }`}
                          >
                            {typeInfo.label || tx.transactionType}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-slate-500 whitespace-nowrap tabular-nums">
                          {formatDateTime(tx.transactionDate)}
                        </td>
                        <td
                          className={`py-3 px-4 text-right font-bold tabular-nums whitespace-nowrap ${
                            isIncome ? 'text-emerald-600' : isExpense ? 'text-rose-600' : 'text-sky-600'
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
                              className={`p-1.5 rounded-lg hover:bg-slate-100 ${
                                tx.attachmentUrl ? 'text-emerald-600' : 'text-slate-400 hover:text-slate-700'
                              }`}
                              title="Chứng từ đính kèm"
                            >
                              <PaperClipIcon className="h-4 w-4" />
                            </button>
                            <button
                              type="button"
                              onClick={() => openEditModal(tx)}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-800 hover:bg-slate-100"
                              title="Chỉnh sửa"
                            >
                              <PencilSquareIcon className="h-4 w-4" />
                            </button>
                            <button
                              type="button"
                              onClick={() => setDeleteTarget(tx)}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50"
                              title="Xóa"
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
        title={editingTx ? 'Chỉnh sửa giao dịch' : 'Ghi nhận giao dịch mới'}
        subtitle="Số dư tài khoản và ngân sách sẽ được tự động cập nhật."
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-3 gap-2 p-1 rounded-xl bg-slate-100">
            {TRANSACTION_TYPES.map((t) => {
              const active = watchedType === t.value;
              return (
                <button
                  key={t.value}
                  type="button"
                  onClick={() => handleTypeSwitch(t.value)}
                  className={`py-2 rounded-lg text-xs font-semibold transition-all ${
                    active ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
                  }`}
                >
                  {t.label}
                </button>
              );
            })}
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
                {selectableAccounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.accountName} ({formatCurrency(acc.currentBalance, acc.currency)})
                  </option>
                ))}
              </select>
              {errors.accountId && (
                <p className="text-xs text-rose-600 mt-1">{errors.accountId.message}</p>
              )}
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
                  <option value="">Chọn tài khoản nhận</option>
                  {selectableAccounts
                    .filter((acc) => acc.id !== watchedAccountId)
                    .map((acc) => (
                      <option key={acc.id} value={acc.id}>
                        {acc.accountName} ({formatCurrency(acc.currentBalance, acc.currency)})
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
                placeholder="0"
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
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Thời gian <span className="text-rose-500">*</span>
              </label>
              <input
                type="datetime-local"
                {...register('transactionDate', { required: 'Vui lòng chọn thời gian' })}
                className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
              />
              {errors.transactionDate && (
                <p className="text-xs text-rose-600 mt-1">{errors.transactionDate.message}</p>
              )}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">Mô tả</label>
            <input
              type="text"
              placeholder="Ví dụ: Ăn trưa, Lương tháng 11..."
              {...register('description', { maxLength: { value: 255, message: 'Tối đa 255 ký tự' } })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">Ghi chú</label>
            <textarea
              rows={2}
              placeholder="Chi tiết bổ sung (nếu có)..."
              {...register('note', { maxLength: { value: 1000, message: 'Tối đa 1000 ký tự' } })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-sm outline-none focus:border-emerald-500"
            />
          </div>

          {!editingTx && (
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Đính kèm ảnh hóa đơn / chứng từ (Tùy chọn)
              </label>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={handleSelectAttachmentForCreate}
                className="w-full text-xs text-slate-500 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-slate-100 file:text-slate-700 hover:file:bg-slate-200"
              />
              <p className="text-[11px] text-slate-400 mt-1">Hỗ trợ JPG, PNG, WEBP tối đa 5MB.</p>
            </div>
          )}

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
              {submitting ? 'Đang lưu...' : editingTx ? 'Cập nhật' : 'Lưu giao dịch'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={Boolean(attachmentTx)}
        onClose={() => setAttachmentTx(null)}
        title="Chứng từ giao dịch"
        subtitle={attachmentTx?.description || attachmentTx?.categoryName || 'Giao dịch'}
      >
        <div className="space-y-4">
          {attachmentTx?.attachmentUrl ? (
            <div className="rounded-xl border border-slate-200 p-3 bg-slate-50 space-y-2">
              {(/\.(png|jpe?g|gif|webp)(\?.*)?$/i.test(attachmentTx.attachmentUrl) ||
                attachmentTx.attachmentUrl.includes('/image/upload/')) ? (
                <img
                  src={attachmentTx.attachmentUrl}
                  alt="Chứng từ giao dịch"
                  className="max-h-72 w-auto mx-auto rounded-lg object-contain"
                />
              ) : null}
              <div className="text-center">
                <a
                  href={attachmentTx.attachmentUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="text-xs font-semibold text-emerald-600 hover:underline"
                >
                  Mở ảnh chứng từ trong tab mới
                </a>
              </div>
            </div>
          ) : (
            <p className="text-xs text-slate-500">Giao dịch này chưa có ảnh đính kèm.</p>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              {attachmentTx?.attachmentUrl ? 'Thay thế ảnh chứng từ mới' : 'Tải lên ảnh chứng từ'}
            </label>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              disabled={uploadingAttachment}
              onChange={handleUploadAttachment}
              className="w-full text-xs text-slate-500 file:mr-3 file:py-2 file:px-3.5 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-emerald-50 file:text-emerald-700 hover:file:bg-emerald-100"
            />
            <p className="text-[11px] text-slate-400 mt-1">Hỗ trợ JPG, PNG, WEBP tối đa 5MB.</p>
            {uploadingAttachment && (
              <p className="text-xs text-slate-500 mt-2">Đang tải ảnh lên Cloudinary...</p>
            )}
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        loading={deleting}
        title="Xóa giao dịch"
        message="Số dư tài khoản liên quan sẽ được hoàn tác khi bạn xóa giao dịch này. Tiếp tục?"
        confirmLabel="Xóa giao dịch"
      />
    </div>
  );
}

