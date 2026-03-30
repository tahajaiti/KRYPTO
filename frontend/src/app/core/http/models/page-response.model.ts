export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  numberOfElements: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
  empty: boolean;
  sortBy?: string;
  sortDirection?: string;
}
