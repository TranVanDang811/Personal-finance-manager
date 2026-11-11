import {
  AcademicCapIcon,
  BanknotesIcon,
  BoltIcon,
  BuildingOffice2Icon,
  CakeIcon,
  ChartBarIcon,
  FilmIcon,
  GiftIcon,
  GlobeAmericasIcon,
  HeartIcon,
  HomeIcon,
  MinusCircleIcon,
  PlusCircleIcon,
  ShoppingBagIcon,
  TagIcon,
  TruckIcon,
} from '@heroicons/react/24/outline';

const ICON_MAP = {
  banknotes: BanknotesIcon,
  gift: GiftIcon,
  'chart-bar': ChartBarIcon,
  'building-office-2': BuildingOffice2Icon,
  'plus-circle': PlusCircleIcon,
  cake: CakeIcon,
  truck: TruckIcon,
  'shopping-bag': ShoppingBagIcon,
  film: FilmIcon,
  heart: HeartIcon,
  'academic-cap': AcademicCapIcon,
  home: HomeIcon,
  bolt: BoltIcon,
  'globe-americas': GlobeAmericasIcon,
  'minus-circle': MinusCircleIcon,
  'cup-soda': CakeIcon,
  coffee: CakeIcon,
  tag: TagIcon,
};

export default function CategoryIcon({ icon, color = '#64748B', className = 'h-4 w-4' }) {
  const Icon = ICON_MAP[icon] || TagIcon;
  return <Icon className={className} style={{ color }} />;
}

