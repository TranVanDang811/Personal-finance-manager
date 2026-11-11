export default function LoadingSpinner({ label = 'Đang tải dữ liệu...' }) {
  return (
    <div className="flex flex-col items-center justify-center py-12 gap-3 text-slate-500">
      <div className="h-8 w-8 rounded-full border-[3px] border-slate-200 border-t-emerald-600 animate-spin" />
      <span className="text-sm font-medium">{label}</span>
    </div>
  );
}
