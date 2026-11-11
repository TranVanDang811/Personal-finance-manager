import { format, parseISO, isValid } from 'date-fns';

export function formatCurrency(amount, currency = 'VND') {
  const num = Number(amount ?? 0);
  const safeNum = Number.isFinite(num) ? num : 0;

  if (currency === 'USD') {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      maximumFractionDigits: 2,
    }).format(safeNum);
  }

  if (currency === 'EUR') {
    return new Intl.NumberFormat('de-DE', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 2,
    }).format(safeNum);
  }

  return `${new Intl.NumberFormat('vi-VN', {
    maximumFractionDigits: 0,
  }).format(safeNum)} ₫`;
}

export function formatCompactCurrency(amount) {
  const num = Number(amount ?? 0);
  if (!Number.isFinite(num) || num === 0) return '0';
  const abs = Math.abs(num);
  const sign = num < 0 ? '-' : '';
  if (abs >= 1_000_000_000) return `${sign}${(abs / 1_000_000_000).toFixed(1)}B`;
  if (abs >= 1_000_000) return `${sign}${(abs / 1_000_000).toFixed(1)}M`;
  if (abs >= 1_000) return `${sign}${(abs / 1_000).toFixed(0)}K`;
  return String(num);
}

export function formatDate(value, pattern = 'dd/MM/yyyy') {
  if (!value) return '—';
  const parsed = typeof value === 'string' ? parseISO(value) : new Date(value);
  return isValid(parsed) ? format(parsed, pattern) : '—';
}

export function formatDateTime(value) {
  return formatDate(value, 'dd/MM/yyyy HH:mm');
}

export function toDateInputValue(value = new Date()) {
  const date = typeof value === 'string' ? parseISO(value) : value;
  return isValid(date) ? format(date, 'yyyy-MM-dd') : format(new Date(), 'yyyy-MM-dd');
}

export function toDateTimeLocalValue(value = new Date()) {
  const date = typeof value === 'string' ? parseISO(value) : value;
  return isValid(date) ? format(date, "yyyy-MM-dd'T'HH:mm") : format(new Date(), "yyyy-MM-dd'T'HH:mm");
}

export function dateTimeLocalToIso(localValue) {
  if (!localValue) return new Date().toISOString();
  const date = new Date(localValue);
  return isValid(date) ? date.toISOString() : new Date().toISOString();
}

export function extractErrorMessage(error, fallback = 'Đã xảy ra lỗi. Vui lòng thử lại.') {
  const data = error?.response?.data;
  if (!data) {
    return error?.message || fallback;
  }
  if (data.errors && typeof data.errors === 'object') {
    const details = Object.values(data.errors).filter(Boolean);
    if (details.length > 0) {
      return details.join(' • ');
    }
  }
  return data.message || fallback;
}

export function generateIdempotencyKey() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

const ALLOWED_IMAGE_MIME_TYPES = new Set(['image/jpeg', 'image/jpg', 'image/png', 'image/webp']);

export function validateImageFile(file, maxSizeMb = 5) {
  if (!file) return 'Vui lòng chọn tệp ảnh hợp lệ.';
  if (!ALLOWED_IMAGE_MIME_TYPES.has((file.type || '').toLowerCase())) {
    return 'Chỉ hỗ trợ ảnh định dạng JPG, PNG hoặc WEBP.';
  }
  if (file.size > maxSizeMb * 1024 * 1024) {
    return `Kích thước ảnh tối đa ${maxSizeMb}MB.`;
  }
  return null;
}

