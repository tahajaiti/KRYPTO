export interface BalanceItemResponse {
  coinId: string;
  symbol: string;
  balance: number;
}

export interface WalletResponse {
  id: string;
  userId: string;
  balances: BalanceItemResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface NetWorthItemResponse {
  coinId: string;
  symbol: string;
  balance: number;
  priceInKryp: number;
  valueInKryp: number;
}

export interface NetWorthResponse {
  userId: string;
  totalNetWorthInKryp: number;
  breakdown: NetWorthItemResponse[];
}

export interface TransferKrypRequest {
  toUserId: string;
  amount: number;
}

export interface TransferResponse {
  fromUserId: string;
  toUserId: string;
  amount: number;
  transferredAt: string;
}

export interface WalletTransferItemResponse {
  id: string;
  fromUserId: string;
  toUserId: string;
  amount: number;
  transferredAt: string;
}

export interface RecipientLookupResponse {
  id: string;
  username: string;
  email: string;
  avatar: string | null;
}
