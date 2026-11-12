import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { ChartBarIcon } from '@heroicons/react/24/outline';
import EmptyState from '../common/EmptyState';
import { formatCompactCurrency, formatCurrency, formatDate } from '../../utils/formatters';
import { HEX_COLOR_PALETTE } from '../../constants/enums';
import { useTranslation } from '../../i18n/I18nContext';
import { useTheme } from '../../theme/ThemeContext';

function CurrencyTooltip({ active, payload, label, currency = 'VND', formatLabel }) {
  if (!active || !payload?.length) return null;

  return (
    <div className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white/95 dark:bg-zinc-950/95 backdrop-blur px-3.5 py-2.5 shadow-lg text-xs">
      <p className="font-semibold text-slate-800 dark:text-slate-100 mb-1.5">
        {formatLabel ? formatLabel(label) : label}
      </p>
      <div className="space-y-1">
        {payload.map((entry) => (
          <div key={entry.dataKey || entry.name} className="flex items-center justify-between gap-4">
            <span className="flex items-center gap-1.5 text-slate-600 dark:text-slate-300">
              <span
                className="h-2.5 w-2.5 rounded-full shrink-0"
                style={{ backgroundColor: entry.color || entry.fill }}
              />
              {entry.name}
            </span>
            <span className="font-semibold text-slate-900 dark:text-white tabular-nums">
              {formatCurrency(entry.value, currency)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

export function CashflowTrendChart({ data = [], currency = 'VND', height = 300 }) {
  const { t } = useTranslation();
  const { isDark } = useTheme();
  const gridColor = isDark ? '#27272A' : '#E2E8F0';
  const tickColor = isDark ? '#A1A1AA' : '#64748B';

  if (!data.length) {
    return (
      <EmptyState
        icon={ChartBarIcon}
        title={t('charts.noCashflowTitle')}
        description={t('charts.noCashflowDesc')}
      />
    );
  }

  const normalized = data.map((item) => ({
    ...item,
    label: item.label || (item.month && item.year ? `T${item.month}/${String(item.year).slice(-2)}` : ''),
    income: Number(item.totalIncome ?? item.income ?? 0),
    expense: Number(item.totalExpense ?? item.expense ?? 0),
  }));

  return (
    <div style={{ height }} className="w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={normalized} margin={{ top: 10, right: 12, left: 0, bottom: 0 }} barGap={6}>
          <CartesianGrid strokeDasharray="3 3" stroke={gridColor} vertical={false} />
          <XAxis
            dataKey="label"
            tick={{ fontSize: 12, fill: tickColor }}
            axisLine={{ stroke: gridColor }}
            tickLine={false}
          />
          <YAxis
            tickFormatter={(v) => formatCompactCurrency(v)}
            tick={{ fontSize: 12, fill: tickColor }}
            axisLine={false}
            tickLine={false}
            width={56}
          />
          <Tooltip content={<CurrencyTooltip currency={currency} />} />
          <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '8px', color: tickColor }} />
          <Bar dataKey="income" name={t('charts.income')} fill="#10B981" radius={[6, 6, 0, 0]} maxBarSize={36} />
          <Bar dataKey="expense" name={t('charts.expense')} fill="#F43F5E" radius={[6, 6, 0, 0]} maxBarSize={36} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function CategoryPieChart({ data = [], currency = 'VND', height = 240 }) {
  const { t } = useTranslation();

  const items = data
    .map((item, idx) => ({
      ...item,
      name: item.categoryName || item.name || t('charts.otherCategory'),
      value: Number(item.totalAmount ?? item.amount ?? 0),
      color: item.categoryColor || item.color || HEX_COLOR_PALETTE[idx % HEX_COLOR_PALETTE.length],
      percentage: Number(item.percentage ?? 0),
    }))
    .filter((item) => item.value > 0);

  if (!items.length) {
    return (
      <EmptyState
        icon={ChartBarIcon}
        title={t('charts.noCategoryTitle')}
        description={t('charts.noCategoryDesc')}
      />
    );
  }

  return (
    <div className="space-y-4">
      <div style={{ height }} className="w-full">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={items}
              dataKey="value"
              nameKey="name"
              cx="50%"
              cy="50%"
              innerRadius={56}
              outerRadius={86}
              paddingAngle={3}
            >
              {items.map((entry, idx) => (
                <Cell key={`${entry.categoryId || entry.name}-${idx}`} fill={entry.color} stroke="none" />
              ))}
            </Pie>
            <Tooltip content={<CurrencyTooltip currency={currency} />} />
          </PieChart>
        </ResponsiveContainer>
      </div>

      <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
        {items.map((item, idx) => (
          <div
            key={`${item.categoryId || item.name}-${idx}`}
            className="flex items-center justify-between gap-2 text-xs py-1 border-b border-slate-100 dark:border-zinc-800 last:border-none"
          >
            <div className="flex items-center gap-2 min-w-0">
              <span
                className="h-2.5 w-2.5 rounded-full shrink-0"
                style={{ backgroundColor: item.color }}
              />
              <span className="font-medium text-slate-700 dark:text-slate-200 truncate">{item.name}</span>
            </div>
            <div className="flex items-center gap-2.5 shrink-0 tabular-nums">
              <span className="text-slate-500 dark:text-slate-400">{item.percentage.toFixed(1)}%</span>
              <span className="font-semibold text-slate-900 dark:text-white">{formatCurrency(item.value, currency)}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function DailyAreaChart({ data = [], currency = 'VND', height = 300 }) {
  const { t } = useTranslation();
  const { isDark } = useTheme();
  const gridColor = isDark ? '#27272A' : '#E2E8F0';
  const tickColor = isDark ? '#A1A1AA' : '#64748B';

  if (!data.length) {
    return (
      <EmptyState
        icon={ChartBarIcon}
        title={t('charts.noDailyTitle')}
        description={t('charts.noDailyDesc')}
      />
    );
  }

  const normalized = data.map((item) => ({
    ...item,
    dateLabel: item.date ? formatDate(item.date, 'dd/MM') : '',
    income: Number(item.totalIncome ?? item.income ?? 0),
    expense: Number(item.totalExpense ?? item.expense ?? 0),
  }));

  return (
    <div style={{ height }} className="w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={normalized} margin={{ top: 10, right: 12, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id="incomeGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#10B981" stopOpacity={0.28} />
              <stop offset="95%" stopColor="#10B981" stopOpacity={0} />
            </linearGradient>
            <linearGradient id="expenseGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#F43F5E" stopOpacity={0.28} />
              <stop offset="95%" stopColor="#F43F5E" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke={gridColor} vertical={false} />
          <XAxis
            dataKey="dateLabel"
            tick={{ fontSize: 12, fill: tickColor }}
            axisLine={{ stroke: gridColor }}
            tickLine={false}
          />
          <YAxis
            tickFormatter={(v) => formatCompactCurrency(v)}
            tick={{ fontSize: 12, fill: tickColor }}
            axisLine={false}
            tickLine={false}
            width={56}
          />
          <Tooltip content={<CurrencyTooltip currency={currency} />} />
          <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '8px', color: tickColor }} />
          <Area
            type="monotone"
            dataKey="income"
            name={t('charts.income')}
            stroke="#10B981"
            strokeWidth={2}
            fill="url(#incomeGrad)"
          />
          <Area
            type="monotone"
            dataKey="expense"
            name={t('charts.expense')}
            stroke="#F43F5E"
            strokeWidth={2}
            fill="url(#expenseGrad)"
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
