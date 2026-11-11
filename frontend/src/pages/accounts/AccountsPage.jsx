import { useCallback, useEffect, useState } from 'react';
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
  ACCOUNT_STATUS_LABELS,
  ACCOUNT_TYPE_LABELS,
  ACCOUNT_TYPES,
  CURRENCIES,
  TRANSACTION_TYPE_MAP,
} from '../../constants/enums';
import { extractErrorMessage, formatCurrency, formatDateTime } from '../../utils/formatters';

function getAccountIcon(type) {
  if (type === 'BANK_ACCOUNT') return BuildingLibraryIcon;
  if (type === 'SAVINGS') return BanknotesIcon;
  return CreditCardIcon;
}

export default function AccountsPage() {
  const dispatch = useDispatch();
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
  const [txPageData, setTxPageData] = useState({ content: [], pageNumber: 0, totalPages: 0, totalElements: 0, pageSize: 8 });

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
          message: extractErrorMessage(err, 'Không thể tải danh sách tài khoản.'),
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
        dispatch(showToast({ type: 'success', message: 'Cập nhật tài khoản thành công.' }));
      } else {
        await accountApi.createAccount({
          accountName: values.accountName.trim(),
          accountType: values.accountType,
          initialBalance: Number(values.initialBalance ?? 0),
          currency: values.currency || userCurrency,
          description: values.description?.trim() || null,
          allowNegative: values.accountType === 'BANK_ACCOUNT' ? Boolean(values.allowNegative) : false,
        });
        dispatch(showToast({ type: 'success', message: 'Tạo tài khoản mới thành công.' }));
      }
      setModalOpen(false);
      await loadAccounts();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể lưu tài khoản.'),
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
        dispatch(showToast({ type: 'success', message: `Đã lưu trữ "${acc.accountName}".` }));
      } else {
        await accountApi.unarchiveAccount(acc.id);
        dispatch(showToast({ type: 'success', message: `Đã khôi phục "${acc.accountName}".` }));
      }
      await loadAccounts();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể thay đổi trạng thái tài khoản.'),
        })
      );
    }
  };

  const handleDeleteAccount = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await accountApi.deleteAccount(deleteTarget.id);
      dispatch(showToast({ type: 'success', message: `Đã xóa tài khoản "${deleteTarget.accountName}".` }));
      setDeleteTarget(null);
      await loadAccounts();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể xóa tài khoản.'),
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
      setTxPageData(res.data || { content: [], pageNumber: 0, totalPages: 0, totalElements: 0, pageSize: 8 });
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể tải lịch sử giao dịch của tài khoản.'),
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
          <p className="text-xs font-medium text-slate-500">Tổng số dư đang hoạt động</p>
          <p className="mt-2 text-2xl font-bold text-slate-900 tabular-nums">
            {formatCurrency(summary?.totalActiveBalance, userCurrency)}
          </p>
        </div>
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500">Tài khoản đang hoạt động</p>
          <p className="mt-2 text-2xl font-bold text-emerald-600 tabular-nums">
            {summary?.activeAccountCount ?? 0}
          </p>
        </div>
        <div className="card-surface p-5">
          <p className="text-xs font-medium text-slate-500">Tài khoản đã lưu trữ</p>
          <p className="mt-2 text-2xl font-bold text-slate-500 tabular-nums">
            {summary?.archivedAccountCount ?? 0}
          </p>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          {[
            { value: 'ALL', label: 'Tất cả' },
            { value: 'ACTIVE', label: 'Đang hoạt động' },
            { value: 'ARCHIVED', label: 'Đã lưu trữ' },
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

          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700 outline-none focus:border-emerald-500"
          >
            <option value="ALL">Mọi loại tài khoản</option>
            {ACCOUNT_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
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
          Thêm tài khoản
        </button>
      </div>

      {loading ? (
        <LoadingSpinner label="Đang tải danh sách tài khoản..." />
      ) : !accounts.length ? (
        <div className="card-surface">
          <EmptyState
            icon={CreditCardIcon}
            title="Chưa có tài khoản phù hợp"
            description="Tạo ví tiền mặt, tài khoản ngân hàng hoặc tài khoản tiết kiệm để bắt đầu."
            action={
              <button
                type="button"
                onClick={openCreateModal}
                className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 text-xs font-semibold"
              >
                <PlusIcon className="h-4 w-4" />
                Tạo tài khoản đầu tiên
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
                  isArchived ? 'opacity-75 bg-slate-50/70' : ''
                }`}
              >
                <div>
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="h-10 w-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                        <Icon className="h-5 w-5" />
                      </div>
                      <div className="min-w-0">
                        <h3 className="text-sm font-bold text-slate-900 truncate">{acc.accountName}</h3>
                        <p className="text-[11px] text-slate-500">
                          {ACCOUNT_TYPE_LABELS[acc.accountType] || acc.accountType}
                        </p>
                      </div>
                    </div>
                    <span
                      className={`px-2 py-0.5 rounded-full text-[11px] font-medium border shrink-0 ${
                        isArchived
                          ? 'bg-slate-100 text-slate-600 border-slate-200'
                          : 'bg-emerald-50 text-emerald-700 border-emerald-200'
                      }`}
                    >
                      {ACCOUNT_STATUS_LABELS[acc.status] || acc.status}
                    </span>
                  </div>

                  <div className="mt-4 pt-4 border-t border-slate-100">
                    <p className="text-[11px] text-slate-500">Số dư hiện tại</p>
                    <p
                      className={`mt-0.5 text-xl font-bold tabular-nums ${
                        Number(acc.currentBalance ?? 0) < 0 ? 'text-rose-600' : 'text-slate-900'
                      }`}
                    >
                      {formatCurrency(acc.currentBalance, acc.currency)}
                    </p>
                    <p className="text-[11px] text-slate-400 mt-1 tabular-nums">
                      Số dư ban đầu: {formatCurrency(acc.initialBalance, acc.currency)}
                      {acc.allowNegative && ' • Cho phép thấu chi'}
                    </p>
                    {acc.description && (
                      <p className="text-xs text-slate-600 mt-2 line-clamp-2">{acc.description}</p>
                    )}
                  </div>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                  <button
                    type="button"
                    onClick={() => loadAccountTransactions(acc, 0)}
                    className="inline-flex items-center gap-1 text-xs font-semibold text-sky-600 hover:text-sky-700"
                  >
                    <ArrowsRightLeftIcon className="h-3.5 w-3.5" />
                    Giao dịch
                  </button>

                  <div className="flex items-center gap-1">
                    <button
                      type="button"
                      onClick={() => openEditModal(acc)}
                      className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100"
                      title="Chỉnh sửa"
                    >
                      <PencilSquareIcon className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => handleToggleArchive(acc)}
                      className="p-1.5 rounded-lg text-slate-500 hover:text-amber-700 hover:bg-amber-50"
                      title={isArchived ? 'Khôi phục' : 'Lưu trữ'}
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
                      className="p-1.5 rounded-lg text-slate-500 hover:text-rose-600 hover:bg-rose-50"
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
        title={editingAccount ? 'Chỉnh sửa tài khoản' : 'Thêm tài khoản mới'}
        subtitle="Thiết lập thông tin ví hoặc tài khoản ngân hàng của bạn."
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Tên tài khoản <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              placeholder="Ví dụ: Techcombank, Ví tiền mặt..."
              {...register('accountName', {
                required: 'Vui lòng nhập tên tài khoản',
                maxLength: { value: 100, message: 'Tối đa 100 ký tự' },
              })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
            />
            {errors.accountName && (
              <p className="text-xs text-rose-600 mt-1">{errors.accountName.message}</p>
            )}
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">Loại tài khoản</label>
              <select
                {...register('accountType', { required: true })}
                className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm bg-white focus:border-emerald-500 outline-none"
              >
                {ACCOUNT_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>

            {!editingAccount && (
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">Tiền tệ</label>
                <select
                  {...register('currency')}
                  className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm bg-white focus:border-emerald-500 outline-none"
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
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">Số dư ban đầu</label>
              <input
                type="number"
                step="0.01"
                {...register('initialBalance', {
                  required: 'Vui lòng nhập số dư ban đầu',
                  validate: (val) => {
                    const num = Number(val);
                    if (Number.isNaN(num)) return 'Số dư không hợp lệ';
                    if (num < 0 && watchedType !== 'BANK_ACCOUNT') {
                      return 'Chỉ tài khoản ngân hàng mới được phép có số dư âm';
                    }
                    return true;
                  },
                })}
                className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm tabular-nums focus:border-emerald-500 outline-none"
              />
              {errors.initialBalance && (
                <p className="text-xs text-rose-600 mt-1">{errors.initialBalance.message}</p>
              )}
            </div>
          )}

          <label className="flex items-start gap-2.5 rounded-xl border border-slate-200 p-3 cursor-pointer">
            <input
              type="checkbox"
              disabled={watchedType !== 'BANK_ACCOUNT'}
              {...register('allowNegative')}
              className="mt-0.5 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500 disabled:opacity-40"
            />
            <div>
              <span className="text-xs font-semibold text-slate-800 block">
                Cho phép số dư âm (Thấu chi)
              </span>
              <span className="text-[11px] text-slate-500">
                Chỉ áp dụng cho loại Tài khoản ngân hàng.
              </span>
            </div>
          </label>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">Mô tả</label>
            <textarea
              rows={2}
              placeholder="Ghi chú thêm về tài khoản..."
              {...register('description', {
                maxLength: { value: 255, message: 'Mô tả tối đa 255 ký tự' },
              })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-sm focus:border-emerald-500 outline-none"
            />
            {errors.description && (
              <p className="text-xs text-rose-600 mt-1">{errors.description.message}</p>
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
              {submitting ? 'Đang lưu...' : editingAccount ? 'Lưu thay đổi' : 'Tạo tài khoản'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={Boolean(txAccount)}
        onClose={() => setTxAccount(null)}
        title={`Lịch sử giao dịch: ${txAccount?.accountName || ''}`}
        subtitle={
          txAccount
            ? `Số dư hiện tại: ${formatCurrency(txAccount.currentBalance, txAccount.currency)}`
            : ''
        }
        size="lg"
      >
        {txLoading ? (
          <LoadingSpinner label="Đang tải giao dịch..." />
        ) : !txPageData.content?.length ? (
          <EmptyState
            title="Tài khoản chưa có giao dịch"
            description="Mọi giao dịch thu, chi hoặc chuyển khoản liên quan tới tài khoản này sẽ hiển thị tại đây."
          />
        ) : (
          <div className="space-y-2">
            <div className="divide-y divide-slate-100">
              {txPageData.content.map((tx) => {
                const isIncomingTransfer =
                  tx.transactionType === 'TRANSFER' && tx.toAccountId === txAccount?.id;
                const isCredit = tx.transactionType === 'INCOME' || isIncomingTransfer;
                const typeInfo = TRANSACTION_TYPE_MAP[tx.transactionType] || {};
                return (
                  <div key={tx.id} className="py-3 flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-slate-900 truncate">
                        {tx.description || tx.categoryName || 'Chuyển khoản'}
                      </p>
                      <p className="text-[11px] text-slate-500">
                        {typeInfo.label || tx.transactionType} • {formatDateTime(tx.transactionDate)}
                      </p>
                    </div>
                    <span
                      className={`text-xs font-bold tabular-nums shrink-0 ${
                        isCredit ? 'text-emerald-600' : 'text-rose-600'
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
        title="Xóa tài khoản"
        message={`Bạn có chắc muốn xóa tài khoản "${deleteTarget?.accountName}"? Tài khoản chỉ có thể xóa nếu chưa có giao dịch phát sinh.`}
        confirmLabel="Xóa tài khoản"
      />
    </div>
  );
}

