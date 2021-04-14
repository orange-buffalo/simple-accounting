import { Components } from '@/services/api/api-client-definition';

export interface ApiPage<T> {
  pageNumber: number,
  totalElements: number,
  pageSize: number,
  data: Array<T>,
}

export interface ApiPageRequest {
  pageNumber?: number,
  pageSize?: number,
}

export type LoginRequest = Components.Schemas.LoginRequest;
export type IncomesExpensesStatisticsDto = Components.Schemas.IncomesExpensesStatisticsDto;
export type IncomeTaxPaymentsStatisticsDto = Components.Schemas.IncomeTaxPaymentsStatisticsDto;
export type InvoiceDto = Components.Schemas.InvoiceDto;
export type CategoryDto = Components.Schemas.CategoryDto;
export type CustomerDto = Components.Schemas.CustomerDto;
