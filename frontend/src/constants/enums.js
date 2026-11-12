export const ACCOUNT_TYPES = [
  { value: 'CASH', labelKey: 'enums.accountTypes.CASH' },
  { value: 'BANK_ACCOUNT', labelKey: 'enums.accountTypes.BANK_ACCOUNT' },
  { value: 'SAVINGS', labelKey: 'enums.accountTypes.SAVINGS' },
];

export const ACCOUNT_TYPE_LABEL_KEYS = {
  CASH: 'enums.accountTypeShort.CASH',
  BANK_ACCOUNT: 'enums.accountTypeShort.BANK_ACCOUNT',
  SAVINGS: 'enums.accountTypeShort.SAVINGS',
};

export const ACCOUNT_STATUS_LABEL_KEYS = {
  ACTIVE: 'enums.accountStatus.ACTIVE',
  ARCHIVED: 'enums.accountStatus.ARCHIVED',
};

export const TRANSACTION_TYPES = [
  {
    value: 'EXPENSE',
    labelKey: 'enums.transactionTypes.EXPENSE',
    badgeClass: 'bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-950/60 dark:text-rose-300 dark:border-rose-800',
  },
  {
    value: 'INCOME',
    labelKey: 'enums.transactionTypes.INCOME',
    badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800',
  },
  {
    value: 'TRANSFER',
    labelKey: 'enums.transactionTypes.TRANSFER',
    badgeClass: 'bg-sky-50 text-sky-700 border-sky-200 dark:bg-sky-950/60 dark:text-sky-300 dark:border-sky-800',
  },
];

export const TRANSACTION_TYPE_MAP = Object.fromEntries(
  TRANSACTION_TYPES.map((item) => [item.value, item])
);

export const CATEGORY_TYPES = [
  { value: 'EXPENSE', labelKey: 'enums.categoryTypes.EXPENSE' },
  { value: 'INCOME', labelKey: 'enums.categoryTypes.INCOME' },
];

export const BUDGET_ALERT_MAP = {
  NORMAL: {
    labelKey: 'enums.budgetAlert.NORMAL',
    badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800',
    barClass: 'bg-emerald-500',
  },
  WARNING: {
    labelKey: 'enums.budgetAlert.WARNING',
    badgeClass: 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800',
    barClass: 'bg-amber-500',
  },
  REACHED: {
    labelKey: 'enums.budgetAlert.REACHED',
    badgeClass: 'bg-orange-50 text-orange-700 border-orange-200 dark:bg-orange-950/60 dark:text-orange-300 dark:border-orange-800',
    barClass: 'bg-orange-500',
  },
  EXCEEDED: {
    labelKey: 'enums.budgetAlert.EXCEEDED',
    badgeClass: 'bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-950/60 dark:text-rose-300 dark:border-rose-800',
    barClass: 'bg-rose-600',
  },
};

export const SAVING_GOAL_STATUS_MAP = {
  IN_PROGRESS: {
    labelKey: 'enums.savingGoalStatus.IN_PROGRESS',
    badgeClass: 'bg-sky-50 text-sky-700 border-sky-200 dark:bg-sky-950/60 dark:text-sky-300 dark:border-sky-800',
  },
  COMPLETED: {
    labelKey: 'enums.savingGoalStatus.COMPLETED',
    badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800',
  },
  CANCELLED: {
    labelKey: 'enums.savingGoalStatus.CANCELLED',
    badgeClass: 'bg-slate-100 text-slate-600 border-slate-200 dark:bg-zinc-900 dark:text-slate-400 dark:border-zinc-700',
  },
};

export const RECURRING_FREQUENCIES = [
  { value: 'DAILY', labelKey: 'enums.recurringFrequency.DAILY' },
  { value: 'WEEKLY', labelKey: 'enums.recurringFrequency.WEEKLY' },
  { value: 'MONTHLY', labelKey: 'enums.recurringFrequency.MONTHLY' },
  { value: 'YEARLY', labelKey: 'enums.recurringFrequency.YEARLY' },
];

export const RECURRING_FREQUENCY_LABEL_KEYS = {
  DAILY: 'enums.recurringFrequency.DAILY',
  WEEKLY: 'enums.recurringFrequency.WEEKLY',
  MONTHLY: 'enums.recurringFrequency.MONTHLY',
  YEARLY: 'enums.recurringFrequency.YEARLY',
};

export const RECURRING_STATUS_MAP = {
  ACTIVE: {
    labelKey: 'enums.recurringStatus.ACTIVE',
    badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800',
  },
  PAUSED: {
    labelKey: 'enums.recurringStatus.PAUSED',
    badgeClass: 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800',
  },
  COMPLETED: {
    labelKey: 'enums.recurringStatus.COMPLETED',
    badgeClass: 'bg-sky-50 text-sky-700 border-sky-200 dark:bg-sky-950/60 dark:text-sky-300 dark:border-sky-800',
  },
  CANCELLED: {
    labelKey: 'enums.recurringStatus.CANCELLED',
    badgeClass: 'bg-slate-100 text-slate-600 border-slate-200 dark:bg-zinc-900 dark:text-slate-400 dark:border-zinc-700',
  },
};

export const CURRENCIES = [
  { value: 'VND', label: 'VND (₫)' },
  { value: 'USD', label: 'USD ($)' },
  { value: 'EUR', label: 'EUR (€)' },
];

export const HEX_COLOR_PALETTE = [
  '#10B981',
  '#059669',
  '#0EA5E9',
  '#3B82F6',
  '#6366F1',
  '#8B5CF6',
  '#EC4899',
  '#EF4444',
  '#F97316',
  '#F59E0B',
  '#14B8A6',
  '#6B7280',
];

export const CATEGORY_ICON_OPTIONS = [
  { value: 'banknotes', labelKey: 'enums.categoryIcons.banknotes' },
  { value: 'gift', labelKey: 'enums.categoryIcons.gift' },
  { value: 'chart-bar', labelKey: 'enums.categoryIcons.chart-bar' },
  { value: 'building-office-2', labelKey: 'enums.categoryIcons.building-office-2' },
  { value: 'cake', labelKey: 'enums.categoryIcons.cake' },
  { value: 'truck', labelKey: 'enums.categoryIcons.truck' },
  { value: 'shopping-bag', labelKey: 'enums.categoryIcons.shopping-bag' },
  { value: 'film', labelKey: 'enums.categoryIcons.film' },
  { value: 'heart', labelKey: 'enums.categoryIcons.heart' },
  { value: 'academic-cap', labelKey: 'enums.categoryIcons.academic-cap' },
  { value: 'home', labelKey: 'enums.categoryIcons.home' },
  { value: 'bolt', labelKey: 'enums.categoryIcons.bolt' },
  { value: 'globe-americas', labelKey: 'enums.categoryIcons.globe-americas' },
  { value: 'tag', labelKey: 'enums.categoryIcons.tag' },
];
