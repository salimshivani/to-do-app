export type Direction = 'inward' | 'outward';
export type BarcodeSource = 'scanned' | 'generated';

export interface Item {
  id: number;
  barcode: string;
  name: string;
  hsn_code: string | null;
  unit: string | null;
  created_at: string;
  barcode_source: BarcodeSource;
  label_printed_at: string | null;
}

export interface Transaction {
  id: number;
  item_id: number;
  direction: Direction;
  quantity: number;
  timestamp: string;
  note: string | null;
}

export interface TransactionWithItem extends Transaction {
  barcode: string;
  item_name: string;
  hsn_code: string | null;
}

export interface DatewiseRow {
  date: string;
  direction: Direction;
  total_quantity: number;
  transaction_count: number;
}

export interface ItemwiseRow {
  item_id: number;
  barcode: string;
  item_name: string;
  hsn_code: string | null;
  inward_total: number;
  outward_total: number;
  net_stock: number;
}

export interface HsnwiseRow {
  hsn_code: string;
  inward_total: number;
  outward_total: number;
  net_stock: number;
  item_count: number;
}
