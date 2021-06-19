import { Components, Paths } from '@/services/api/api-client-definition';

export interface ApiPage<T> {
  pageNumber: number,
  totalElements: number,
  pageSize: number,
  data: Array<T>,
}

export interface ApiPageRequest {
  pageNumber?: number | null,
  pageSize?: number | null,
}

export interface HasId {
  id?: number | null,
}

export type LoginRequest = Components.Schemas.LoginRequest;
export type IncomesExpensesStatisticsDto = Components.Schemas.IncomesExpensesStatisticsDto;
export type IncomeTaxPaymentsStatisticsDto = Components.Schemas.IncomeTaxPaymentsStatisticsDto;
export type InvoiceDto = Components.Schemas.InvoiceDto;
export type ExpenseDto = Components.Schemas.ExpenseDto;
export type IncomeDto = Components.Schemas.IncomeDto;
export type IncomeTaxPaymentDto = Components.Schemas.IncomeTaxPaymentDto;
export type GeneralTaxReportDto = Components.Schemas.GeneralTaxReportDto;
export type FinalizedTaxSummaryItemDto = Components.Schemas.FinalizedTaxSummaryItemDto;
export type PendingTaxSummaryItemDto = Components.Schemas.PendingTaxSummaryItemDto;
export type CategoryDto = Components.Schemas.CategoryDto;
export type CustomerDto = Components.Schemas.CustomerDto;
export type GeneralTaxDto = Components.Schemas.GeneralTaxDto;
export type GetInvoicesParameters = Paths.GetInvoices.PathParameters & Paths.GetInvoices.QueryParameters;
export type WorkspaceDto = Components.Schemas.WorkspaceDto;
export type WorkspaceAccessTokenDto = Components.Schemas.WorkspaceAccessTokenDto;
