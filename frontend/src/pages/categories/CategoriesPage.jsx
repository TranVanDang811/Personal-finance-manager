import { useCallback, useEffect, useRef, useState } from 'react';
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
import { useTranslation } from '../../i18n/I18nContext';
import { extractErrorMessage } from '../../utils/formatters';

export default function CategoriesPage() {
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const tRef = useRef(t);
  tRef.current = t;

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
          message: extractErrorMessage(err, tRef.current('categories.loadError')),
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
        dispatch(showToast({ type: 'success', message: t('categories.updateSuccess') }));
      } else {
        await categoryApi.createCategory({
          name: values.name.trim(),
          categoryType: values.categoryType,
          icon: values.icon,
          color: values.color,
        });
        dispatch(showToast({ type: 'success', message: t('categories.createSuccess') }));
      }
      setModalOpen(false);
      await loadCategories();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('categories.saveError')),
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
      dispatch(
        showToast({
          type: 'success',
          message: t('categories.deleteSuccess', { name: deleteTarget.name }),
        })
      );
      setDeleteTarget(null);
      await loadCategories();
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('categories.deleteError')),
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
            { value: 'ALL', label: t('categories.allTab') },
            { value: 'EXPENSE', label: t('enums.categoryTypes.EXPENSE') },
            { value: 'INCOME', label: t('enums.categoryTypes.INCOME') },
          ].map((tab) => (
            <button
              key={tab.value}
              type="button"
              onClick={() => setActiveTab(tab.value)}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition-colors ${
                activeTab === tab.value
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
          {t('categories.addCategory')}
        </button>
      </div>

      {loading ? (
        <LoadingSpinner label={t('categories.loading')} />
      ) : !filtered.length ? (
        <div className="card-surface">
          <EmptyState
            icon={TagIcon}
            title={t('categories.emptyTitle')}
            description={t('categories.emptyDesc')}
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
                      <h3 className="text-sm font-semibold text-slate-900 dark:text-white truncate">{cat.name}</h3>
                      {isSystem && (
                        <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded bg-slate-100 dark:bg-zinc-900 text-slate-500 dark:text-slate-400 text-[10px] font-medium shrink-0">
                          <LockClosedIcon className="h-3 w-3" />
                          {t('categories.systemBadge')}
                        </span>
                      )}
                    </div>
                    <span
                      className={`inline-block mt-1 text-[11px] font-medium ${
                        isIncome ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'
                      }`}
                    >
                      {isIncome ? t('enums.categoryTypes.INCOME') : t('enums.categoryTypes.EXPENSE')}
                    </span>
                  </div>
                </div>

                {!isSystem && (
                  <div className="flex items-center gap-1 shrink-0">
                    <button
                      type="button"
                      onClick={() => openEditModal(cat)}
                      className="p-1.5 rounded-lg text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-zinc-800"
                      title={t('common.edit')}
                    >
                      <PencilSquareIcon className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => setDeleteTarget(cat)}
                      className="p-1.5 rounded-lg text-slate-500 dark:text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/50"
                      title={t('common.delete')}
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
        title={editingCategory ? t('categories.modalEditTitle') : t('categories.modalCreateTitle')}
        subtitle={t('categories.modalSubtitle')}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('categories.nameLabel')} <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                placeholder={t('categories.namePlaceholder')}
                {...register('name', {
                  required: t('categories.nameRequired'),
                  maxLength: { value: 100, message: t('categories.nameMax') },
                })}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm focus:border-emerald-500 outline-none"
              />
              {errors.name && <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.name.message}</p>}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('categories.typeLabel')}
              </label>
              <select
                disabled={Boolean(editingCategory)}
                {...register('categoryType')}
                className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 px-3 py-2.5 text-sm bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 disabled:bg-slate-100 dark:disabled:bg-zinc-900/60 outline-none focus:border-emerald-500"
              >
                {CATEGORY_TYPES.map((item) => (
                  <option key={item.value} value={item.value}>
                    {t(item.labelKey)}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-2">
              {t('categories.iconLabel')}
            </label>
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
                        ? 'border-emerald-500 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 font-semibold'
                        : 'border-slate-200 dark:border-zinc-800 hover:bg-slate-50 dark:hover:bg-zinc-900 text-slate-600 dark:text-slate-300'
                    }`}
                  >
                    <CategoryIcon
                      icon={opt.value}
                      color={active ? selectedColor || '#10B981' : '#64748B'}
                      className="h-5 w-5"
                    />
                    <span className="truncate w-full text-center">{t(opt.labelKey)}</span>
                  </button>
                );
              })}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-2">
              {t('categories.colorLabel')}
            </label>
            <div className="flex flex-wrap gap-2">
              {HEX_COLOR_PALETTE.map((hex) => {
                const active = selectedColor?.toUpperCase() === hex.toUpperCase();
                return (
                  <button
                    key={hex}
                    type="button"
                    onClick={() => setValue('color', hex)}
                    className={`h-8 w-8 rounded-full border-2 transition-transform ${
                      active ? 'border-slate-900 dark:border-white scale-110' : 'border-transparent'
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
                : editingCategory
                  ? t('categories.updateBtn')
                  : t('categories.createBtn')}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        loading={deleting}
        title={t('categories.deleteTitle')}
        message={t('categories.deleteConfirm', { name: deleteTarget?.name || '' })}
        confirmLabel={t('common.delete')}
      />
    </div>
  );
}
