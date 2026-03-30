export type BlockchainTransactionType = 'TRANSFER' | 'COIN_CREATION' | 'TRADE' | 'REWARD' | 'MARKET_EVENT';

export interface AddTransactionRequest {
  type: BlockchainTransactionType;
  fromUserId?: string;
  toUserId?: string;
  coinSymbol?: string;
  amount: number;
  fee?: number;
  sourceEventId?: string;
  eventTimestamp?: number;
}

export interface TransactionResponse {
  id: string;
  type: BlockchainTransactionType;
  fromUserId: string | null;
  toUserId: string | null;
  coinSymbol: string | null;
  amount: number;
  fee: number | null;
  timestamp: string;
  hash: string;
}

export interface BlockResponse {
  index: number;
  hash: string;
  previousHash: string;
  timestamp: string;
  nonce: number;
  transactionCount: number;
  transactions: TransactionResponse[];
}

export interface ChainValidationResponse {
  valid: boolean;
  blockCount: number;
  message: string;
}
