export const ACCOUNT_TYPES = [
  { value: 'CASH', label: 'Tiền mặt' },
  { value: 'BANK_ACCOUNT', label: 'Tài khoản ngân hàng' },
  { value: 'SAVINGS', label: 'Tài khoản tiết kiệm' },
];

export const ACCOUNT_TYPE_LABELS = {
  CASH: 'Tiền mặt',
  BANK_ACCOUNT: 'Ngân hàng',
  SAVINGS: 'Tiết kiệm',
};

export const ACCOUNT_STATUS_LABELS = {
  ACTIVE: 'Đang hoạt động',
  ARCHIVED: 'Đã lưu trữ',
};

export const TRANSACTION_TYPES = [
  { value: 'EXPENSE', label: 'Chi tiêu', badgeClass: 'bg-rose-50 text-rose-700 border-rose-200' },
  { value: 'INCOME', label: 'Thu nhập', badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  { value: 'TRANSFER', label: 'Chuyển khoản', badgeClass: 'bg-sky-50 text-sky-700 border-sky-200' },
];

export const TRANSACTION_TYPE_MAP = Object.fromEntries(
  TRANSACTION_TYPES.map((item) => [item.value, item])
);

export const CATEGORY_TYPES = [
  { value: 'EXPENSE', label: 'Chi tiêu' },
  { value: 'INCOME', label: 'Thu nhập' },
];

export const BUDGET_ALERT_MAP = {
  NORMAL: { label: 'An toàn', badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200', barClass: 'bg-emerald-500' },
  WARNING: { label: 'Cảnh báo (>=80%)', badgeClass: 'bg-amber-50 text-amber-700 border-amber-200', barClass: 'bg-amber-500' },
  REACHED: { label: 'Chạm hạn mức (100%)', badgeClass: 'bg-orange-50 text-orange-700 border-orange-200', barClass: 'bg-orange-500' },
  EXCEEDED: { label: 'Vượt hạn mức', badgeClass: 'bg-rose-50 text-rose-700 border-rose-200', barClass: 'bg-rose-600' },
};

export const SAVING_GOAL_STATUS_MAP = {
  IN_PROGRESS: { label: 'Đang thực hiện', badgeClass: 'bg-sky-50 text-sky-700 border-sky-200' },
  COMPLETED: { label: 'Hoàn thành', badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  CANCELLED: { label: 'Đã hủy', badgeClass: 'bg-slate-100 text-slate-600 border-slate-200' },
};

export const RECURRING_FREQUENCIES = [
  { value: 'DAILY', label: 'Hàng ngày' },
  { value: 'WEEKLY', label: 'Hàng tuần' },
  { value: 'MONTHLY', label: 'Hàng tháng' },
  { value: 'YEARLY', label: 'Hàng năm' },
];

export const RECURRING_FREQUENCY_LABELS = {
  DAILY: 'Hàng ngày',
  WEEKLY: 'Hàng tuần',
  MONTHLY: 'Hàng tháng',
  YEARLY: 'Hàng năm',
};

export const RECURRING_STATUS_MAP = {
  ACTIVE: { label: 'Đang chạy', badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  PAUSED: { label: 'Tạm dừng', badgeClass: 'bg-amber-50 text-amber-700 border-amber-200' },
  COMPLETED: { label: 'Hoàn tất', badgeClass: 'bg-sky-50 text-sky-700 border-sky-200' },
  CANCELLED: { label: 'Đã hủy', badgeClass: 'bg-slate-100 text-slate-600 border-slate-200' },
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
  { value: 'banknotes', label: 'Tiền lương' },
  { value: 'gift', label: 'Thưởng / Quà' },
  { value: 'chart-bar', label: 'Đầu tư' },
  { value: 'building-office-2', label: 'Kinh doanh' },
  { value: 'cake', label: 'Ăn uống' },
  { value: 'truck', label: 'Di chuyển' },
  { value: 'shopping-bag', label: 'Mua sắm' },
  { value: 'film', label: 'Giải trí' },
  { value: 'heart', label: 'Sức khỏe' },
  { value: 'academic-cap', label: 'Giáo dục' },
  { value: 'home', label: 'Nhà ở' },
  { value: 'bolt', label: 'Hóa đơn' },
  { value: 'globe-americas', label: 'Du lịch' },
  { value: 'tag', label: 'Khác' },
];

