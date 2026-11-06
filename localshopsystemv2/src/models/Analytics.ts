/** Respuestas de /graphs/* (importes en euros). */

export type Totals = {
  revenue: number;
  taxBase: number;
  tax: number;
  cost: number;
  profit: number;
  /** Margen sobre la base imponible con coste conocido. null si no hay costes. */
  marginPct: number | null;
  /** % de la base imponible cuyo coste se conoce (el beneficio solo cuenta esa parte). */
  costCoverage: number;
  trades: number;
  units: number;
  avgTicket: number;
  avgUnitsPerTrade: number;
  cashRevenue: number;
  cardRevenue: number;
  cancelledTrades: number;
  cancelledAmount: number;
};

export type Summary = {
  from: string;
  to: string;
  current: Totals;
  previousFrom: string;
  previousTo: string;
  previous: Totals;
};

export type Granularity = 'day' | 'week' | 'month';

export type TimePoint = { period: string; revenue: number; profit: number; trades: number; units: number };

export type RankMetric = 'revenue' | 'units' | 'profit';

export type ItemRank = {
  itemId: number | null;
  name: string;
  category: string;
  units: number;
  revenue: number;
  profit: number | null;
  marginPct: number | null;
  trades: number;
};

export type CategoryShare = { category: string; revenue: number; profit: number | null; units: number; sharePct: number };

export type PaymentShare = { method: 'CASH' | 'CARD'; label: string; revenue: number; trades: number; sharePct: number };

export type Bucket = { bucket: number; label: string; revenue: number; trades: number; avgTicket: number };

export type InventoryCategory = { category: string; items: number; units: number; valueAtCost: number; valueAtPrice: number };

export type StockAlert = { itemId: number; name: string; category: string; stock: number; bareMinimun: number };

export type Inventory = {
  items: number;
  units: number;
  valueAtCost: number;
  valueAtPrice: number;
  potentialProfit: number;
  itemsWithoutCost: number;
  outOfStock: number;
  belowMinimum: number;
  ignoringStock: number;
  byCategory: InventoryCategory[];
  alerts: StockAlert[];
};
