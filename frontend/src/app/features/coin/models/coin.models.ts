export interface CoinResponse {
  id: string;
  name: string;
  symbol: string;
  image: string | null;
  initialSupply: number;
  currentSupply: number;
  creatorId: string;
  creationFee: number;
  currentPrice: number;
  marketCap: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CoinPriceResponse {
  coinId: string;
  symbol: string;
  currentPrice: number;
  active: boolean;
}

export interface CoinPriceHistoryPointResponse {
  price: number;
  volume: number;
  recordedAt: string;
}

export interface CoinInvestmentPreferenceResponse {
  coinId: string;
  investing: boolean;
}

export interface CreateCoinRequest {
  name: string;
  symbol: string;
  image?: string;
  initialSupply: number;
}
