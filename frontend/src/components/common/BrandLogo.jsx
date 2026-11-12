/**
 * Reusable DangCap brand emblem rendered from the official public logo asset.
 */
export default function BrandLogo({ className = 'h-9 w-9' }) {
  return (
    <img
      src="/dangcap-logo.jpg"
      alt="DangCap"
      className={`${className} rounded-xl object-cover bg-black border border-slate-200/80 dark:border-zinc-700/80 shadow-sm shrink-0`}
    />
  );
}

