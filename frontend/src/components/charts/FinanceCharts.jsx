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

function CurrencyTooltip({ active, payload, label, currency = 'VND', formatLabel }) {
  if (!active || !payload?.length) return null;

  return (
    <div className="rounded-xl border border-slate-200 bg-white/95 backdrop-blur px-3.5 py-2.5 shadow-lg text-xs">
      <p className="font-semibold text-slate-800 mb-1.5">
        {formatLabel ? formatLabel(label) : label}
      </p>
      <div className="space-y-1">
        {payload.map((entry) => (
          <div key={entry.dataKey || entry.name} className="flex items-center justify-between gap-4">
            <span className="flex items-center gap-1.5 text-slate-600">
              <span
                className="h-2.5 w-2.5 rounded-full shrink-0"
                style={{ backgroundColor: entry.color || entry.fill }}
              />
              {entry.name}
            </span>
            <span className="font-semibold text-slate-900 tabular-nums">
              {formatCurrency(entry.value, currency)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

export function CashflowTrendChart({ data = [], currency = 'VND', height = 300 }) {
  if (!data.length) {
    return (
      <EmptyState
        icon={ChartBarIcon}
        title="Chưa có dữ liệu dòng tiền"
        description="Thêm giao dịch thu nhập hoặc chi tiêu để xem biểu đồ xu hướng."
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
          <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" vertical={false} />
          <XAxis
            dataKey="label"
            tick={{ fontSize: 12, fill: '#64748B' }}
            axisLine={{ stroke: '#E2E8F0' }}
            tickLine={false}
          />
          <YAxis
            tickFormatter={(v) => formatCompactCurrency(v)}
            tick={{ fontSize: 12, fill: '#64748B' }}
            axisLine={false}
            tickLine={false}
            width={56}
          />
          <Tooltip content={<CurrencyTooltip currency={currency} />} />
          <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '8px' }} />
          <Bar dataKey="income" name="Thu nhập" fill="#10B981" radius={[6, 6, 0, 0]} maxBarSize={36} />
          <Bar dataKey="expense" name="Chi tiêu" fill="#F43F5E" radius={[6, 6, 0, 0]} maxBarSize={36} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function CategoryPieChart({ data = [], currency = 'VND', height = 240 }) {
  const items = data
    .map((item, idx) => ({
      ...item,
      name: item.categoryName || item.name || 'Khác',
      value: Number(item.totalAmount ?? item.amount ?? 0),
      color: item.categoryColor || item.color || HEX_COLOR_PALETTE[idx % HEX_COLOR_PALETTE.length],
      percentage: Number(item.percentage ?? 0),
    }))
    .filter((item) => item.value > 0);

  if (!items.length) {
    return (
      <EmptyState
        icon={ChartBarIcon}
        title="Chưa có dữ liệu danh mục"
        description="Không có giao dịch nào trong khoảng thời gian đã chọn."
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
            className="flex items-center justify-between gap-2 text-xs py-1 border-b border-slate-100 last:border-none"
          >
            <div className="flex items-center gap-2 min-w-0">
              <span
                className="h-2.5 w-2.5 rounded-full shrink-0"
                style={{ backgroundColor: item.color }}
              />
              <span className="font-medium text-slate-700 truncate">{item.name}</span>
            </div>
            <div className="flex items-center gap-2.5 shrink-0 tabular-nums">
              <span className="text-slate-500">{item.percentage.toFixed(1)}%</span>
              <span className="font-semibold text-slate-900">{formatCurrency(item.value, currency)}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function DailyAreaChart({ data = [], currency = 'VND', height = 300 }) {
  if (!data.length) {
    return (
      <EmptyState
        icon={ChartBarIcon}
        title="Chưa có phát sinh theo ngày"
        description="Chọn kỳ báo cáo có giao dịch để xem chi tiết dòng tiền hàng ngày."
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
          <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" vertical={false} />
          <XAxis
            dataKey="dateLabel"
            tick={{ fontSize: 12, fill: '#64748B' }}
            axisLine={{ stroke: '#E2E8F0' }}
            tickLine={false}
          />
          <YAxis
            tickFormatter={(v) => formatCompactCurrency(v)}
            tick={{ fontSize: 12, fill: '#64748B' }}
            axisLine={false}
            tickLine={false}
            width={56}
          />
          <Tooltip content={<CurrencyTooltip currency={currency} />} />
          <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '8px' }} />
          <Area
            type="monotone"
            dataKey="income"
            name="Thu nhập"
            stroke="#10B981"
            strokeWidth={2}
            fill="url(#incomeGrad)"
          />
          <Area
            type="monotone"
            dataKey="expense"
            name="Chi tiêu"
            stroke="#F43F5E"
            strokeWidth={2}
            fill="url(#expenseGrad)"
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

