import { useCallback, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useForm } from 'react-hook-form';
import { LockClosedIcon, PencilSquareIcon, PlusIcon, TagIcon, TrashIcon } from '@heroicons/react/24/outline';
import { categoryApi } from '../../services/categoryApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import CategoryIcon from '../../components/common/CategoryIcon';
import Modal from '../../components/common/Modal';
import EmptyState from '../../components/common/EmptyState';
import ConfirmDialog from '../../components/feedback/ConfirmDialog';
import LoadingSpinner from '../../components/feedback/LoadingSpinner';
import {
  CATEGORY_ICON_OPTIONS,
  CATEGORY_TYPES,
  HEX_COLOR_PALETTE,
} from '../../constants/enums';
import { extractErrorMessage } from '../../utils/formatters';

export default function CategoriesPage() {
  const dispatch = useDispatch();
  const [loading, setLoading] = useState(true);
  const [categories, setCategories] = useState([]);
  const [activeTab, setActiveTab] = useState('ALL');

  const [modalOpen, setModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [submitting, setSubmitting] = useState(false);

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

  const selectedIcon = watch('icon');
  const selectedColor = watch('color');

  const loadCategories = useCallback(async () => {
    setLoading(true);
    try {
      const res = await categoryApi.getCategories();
      setCategories(res.data || []);
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể tải danh mục.'),
        })
      );
    } finally {
      setLoading(false);
    }
  }, [dispatch]);

  useEffect(() => {
    loadCategories();
  }, [loadCategories]);

  const openCreateModal = () => {
    setEditingCategory(null);
    reset({
      name: '',
      categoryType: activeTab === 'INCOME' ? 'INCOME' : 'EXPENSE',
      icon: 'tag',
      color: HEX_COLOR_PALETTE[0],
    });
    setModalOpen(true);
  };

  const openEditModal = (cat) => {
    setEditingCategory(cat);
    reset({
      name: cat.name,
      categoryType: cat.categoryType,
      icon: cat.icon || 'tag',
      color: cat.color || HEX_COLOR_PALETTE[0],
    });
    setModalOpen(true);
  };

  const onSubmit = async (values) => {
    setSubmitting(true);
    try {
      if (editingCategory) {
        await categoryApi.updateCategory(editingCategory.id, {
          name: values.name.trim(),
          icon: values.icon,
          color: values.color,
        });
        dispatch(showToast({ type: 'success', message: 'Cập nhật danh mục thành công.' }));
      } else {
        await categoryApi.createCategory({
          name: values.name.trim(),
          categoryType: values.categoryType,
          icon: values.icon,
          color: values.color,
        });
        dispatch(showToast({ type: 'success', message: 'Tạo danh mục mới thành công.' }));
      }
      setModalOpen(false);
      await loadCategories();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể lưu danh mục.'),
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
      await categoryApi.deleteCategory(deleteTarget.id);
      dispatch(showToast({ type: 'success', message: `Đã xóa danh mục "${deleteTarget.name}".` }));
      setDeleteTarget(null);
      await loadCategories();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể xóa danh mục.'),
        })
      );
    } finally {
      setDeleting(false);
    }
  };

  const filtered = categories.filter(
    (cat) => activeTab === 'ALL' || cat.categoryType === activeTab
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          {[
            { value: 'ALL', label: 'Tất cả danh mục' },
            { value: 'EXPENSE', label: 'Chi tiêu' },
            { value: 'INCOME', label: 'Thu nhập' },
          ].map((tab) => (
            <button
              key={tab.value}
              type="button"
              onClick={() => setActiveTab(tab.value)}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition-colors ${
                activeTab === tab.value
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
          Thêm danh mục
        </button>
      </div>

      {loading ? (
        <LoadingSpinner label="Đang tải danh mục..." />
      ) : !filtered.length ? (
        <div className="card-surface">
          <EmptyState
            icon={TagIcon}
            title="Chưa có danh mục"
            description="Tạo danh mục tùy chỉnh để phân loại giao dịch thu chi rõ ràng hơn."
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((cat) => {
            const isSystem = Boolean(cat.isSystem ?? cat.system);
            const isIncome = cat.categoryType === 'INCOME';
            return (
              <div
                key={cat.id}
                className="card-surface p-4 flex items-center justify-between gap-3"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div
                    className="h-10 w-10 rounded-xl flex items-center justify-center shrink-0"
                    style={{ backgroundColor: `${cat.color || '#64748B'}18` }}
                  >
                    <CategoryIcon
                      icon={cat.icon}
                      color={cat.color || '#64748B'}
                      className="h-5 w-5"
                    />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h3 className="text-sm font-semibold text-slate-900 truncate">{cat.name}</h3>
                      {isSystem && (
                        <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded bg-slate-100 text-slate-500 text-[10px] font-medium shrink-0">
                          <LockClosedIcon className="h-3 w-3" />
                          Hệ thống
                        </span>
                      )}
                    </div>
                    <span
                      className={`inline-block mt-1 text-[11px] font-medium ${
                        isIncome ? 'text-emerald-600' : 'text-rose-600'
                      }`}
                    >
                      {isIncome ? 'Thu nhập' : 'Chi tiêu'}
                    </span>
                  </div>
                </div>

                {!isSystem && (
                  <div className="flex items-center gap-1 shrink-0">
                    <button
                      type="button"
                      onClick={() => openEditModal(cat)}
                      className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100"
                      title="Chỉnh sửa"
                    >
                      <PencilSquareIcon className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => setDeleteTarget(cat)}
                      className="p-1.5 rounded-lg text-slate-500 hover:text-rose-600 hover:bg-rose-50"
                      title="Xóa"
                    >
                      <TrashIcon className="h-4 w-4" />
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editingCategory ? 'Chỉnh sửa danh mục' : 'Thêm danh mục tùy chỉnh'}
        subtitle="Chọn tên, biểu tượng và màu sắc để dễ nhận diện khi ghi giao dịch."
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Tên danh mục <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                placeholder="Ví dụ: Cà phê, Thú cưng..."
                {...register('name', {
                  required: 'Vui lòng nhập tên danh mục',
                  maxLength: { value: 100, message: 'Tối đa 100 ký tự' },
                })}
                className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm focus:border-emerald-500 outline-none"
              />
              {errors.name && <p className="text-xs text-rose-600 mt-1">{errors.name.message}</p>}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">Loại</label>
              <select
                disabled={Boolean(editingCategory)}
                {...register('categoryType')}
                className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm bg-white disabled:bg-slate-100 outline-none focus:border-emerald-500"
              >
                {CATEGORY_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-2">Biểu tượng</label>
            <div className="grid grid-cols-4 sm:grid-cols-6 gap-2">
              {CATEGORY_ICON_OPTIONS.map((opt) => {
                const active = selectedIcon === opt.value;
                return (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => setValue('icon', opt.value)}
                    className={`flex flex-col items-center gap-1 p-2 rounded-xl border text-[11px] transition-colors ${
                      active
                        ? 'border-emerald-500 bg-emerald-50 text-emerald-700 font-semibold'
                        : 'border-slate-200 hover:bg-slate-50 text-slate-600'
                    }`}
                  >
                    <CategoryIcon
                      icon={opt.value}
                      color={active ? selectedColor || '#10B981' : '#64748B'}
                      className="h-5 w-5"
                    />
                    <span className="truncate w-full text-center">{opt.label}</span>
                  </button>
                );
              })}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-2">Màu sắc</label>
            <div className="flex flex-wrap gap-2">
              {HEX_COLOR_PALETTE.map((hex) => {
                const active = selectedColor?.toUpperCase() === hex.toUpperCase();
                return (
                  <button
                    key={hex}
                    type="button"
                    onClick={() => setValue('color', hex)}
                    className={`h-8 w-8 rounded-full border-2 transition-transform ${
                      active ? 'border-slate-900 scale-110' : 'border-transparent'
                    }`}
                    style={{ backgroundColor: hex }}
                    aria-label={hex}
                  />
                );
              })}
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
              {submitting ? 'Đang lưu...' : editingCategory ? 'Cập nhật' : 'Tạo danh mục'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        loading={deleting}
        title="Xóa danh mục"
        message={`Bạn có chắc muốn xóa danh mục "${deleteTarget?.name}"?`}
        confirmLabel="Xóa"
      />
    </div>
  );
}

