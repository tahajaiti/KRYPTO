export type OrderType = 'MARKET' | 'LIMIT';
export type OrderSide = 'BUY' | 'SELL';

export interface PlaceOrderRequest {
  coinId: string;
  type: OrderType;
  side: OrderSide;
  price?: number;
  amount: number;
}

export interface OrderResponse {
  id: string;
  userId: string;
  coinId: string;
  type: OrderType;
  side: OrderSide;
  price: number | null;
  amount: number;
  filledAmount: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface TradeResponse {
  id: string;
  buyOrderId: string;
  sellOrderId: string;
  coinId: string;
  buyerId: string;
  sellerId: string;
  price: number;
  amount: number;
  fee: number;
  executedAt: string;
}

export interface TradingLeaderboardEntryResponse {
  userId: string;
  username?: string;
  totalVolume: number;
  totalNotional: number;
  trades: number;
}
