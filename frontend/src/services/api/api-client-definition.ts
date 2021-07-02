import {
  OpenAPIClient,
  Parameters,
  UnknownParamsObject,
  OperationResponse,
  AxiosRequestConfig,
} from 'openapi-client-axios';
          
export namespace Components {
  export namespace Schemas {
    export interface ApiPageCategoryDto {
      pageNumber: number; // int32
      pageSize: number; // int32
      totalElements: number; // int64
      data: CategoryDto[];
    }
    export interface ApiPageCustomerDto {
      pageNumber: number; // int32
      pageSize: number; // int32
      totalElements: number; // int64
      data: CustomerDto[];
    }
    export interface ApiPageDocumentDto {
      pageNumber: number; // int32
      pageSize: number; // int32
      totalElements: number; // int64
      data: DocumentDto[];
    }
    export interface ApiPageExpenseDto {
      pageNumber: number; // int32
      pageSize: number; // int32
      totalElements: number; // int64
      data: ExpenseDto[];
    }
    export interface ApiPageGeneralTaxDto {
      pageNumber: number; // int32
      pageSize: number; // int32
      totalElements: number; // int64
      data: GeneralTaxDto[];
    }
    export interface ApiPageIncomeDto {
      pageNumber: number; // int32
      pageSize: number; // int32
      totalElements: number; // int64
      data: IncomeDto[];
    }
    export interface ApiPageIncomeTaxPaymentDto {
      pageNumber: number; // int32
      pageSize: number; // int32
      totalElements: number; // int64
      data: IncomeTaxPaymentDto[];
    }
    export interface ApiPageInvoiceDto {
      pageNumber: number; // int32
      pageSize: number; // int32
      totalElements: number; // int64
      data: InvoiceDto[];
    }
    export interface ApiPageUserDto {
      pageNumber: number; // int32
      pageSize: number; // int32
      totalElements: number; // int64
      data: UserDto[];
    }
    export interface ApiPageWorkspaceAccessTokenDto {
      pageNumber: number; // int32
      pageSize: number; // int32
      totalElements: number; // int64
      data: WorkspaceAccessTokenDto[];
    }
    export interface CategoryDto {
      id?: number | null; // int64
      version: number; // int32
      name: string;
      description?: string | null;
      income: boolean;
      expense: boolean;
    }
    export interface CreateCategoryDto {
      name: string;
      description?: string | null;
      income: boolean;
      expense: boolean;
    }
    export interface CreateUserDto {
      userName: string;
      admin: boolean;
      password: string;
    }
    export interface CreateWorkspaceAccessTokenDto {
      validTill: string; // date-time
    }
    export interface CreateWorkspaceDto {
      name: string;
      defaultCurrency: string;
    }
    export interface CurrentUserPushNotificationMessage {
      eventName: string;
      data?: {
      };
    }
    export interface CustomerDto {
      name: string;
      id: number; // int64
      version: number; // int32
    }
    export interface DataBuffer {
    }
    export interface DocumentDto {
      id?: number | null; // int64
      version: number; // int32
      name: string;
      timeUploaded: string; // date-time
      sizeInBytes?: number | null; // int64
    }
    export interface DocumentsStorageStatus {
      active: boolean;
    }
    export interface EditCustomerDto {
      name: string;
    }
    export interface EditExpenseDto {
      category?: number | null; // int64
      datePaid: string; // date
      title: string;
      currency: string;
      originalAmount: number; // int64
      convertedAmountInDefaultCurrency?: number | null; // int64
      useDifferentExchangeRateForIncomeTaxPurposes: boolean;
      incomeTaxableAmountInDefaultCurrency?: number | null; // int64
      attachments?: number /* int64 */ [] | null;
      percentOnBusiness?: number | null; // int32
      notes?: string | null;
      generalTax?: number | null; // int64
    }
    export interface EditGeneralTaxDto {
      title: string;
      description?: string | null;
      rateInBps: number; // int32
    }
    export interface EditIncomeDto {
      category?: number | null; // int64
      dateReceived: string; // date
      title: string;
      currency: string;
      originalAmount: number; // int64
      convertedAmountInDefaultCurrency?: number | null; // int64
      useDifferentExchangeRateForIncomeTaxPurposes: boolean;
      incomeTaxableAmountInDefaultCurrency?: number | null; // int64
      attachments?: number /* int64 */ [] | null;
      notes?: string | null;
      generalTax?: number | null; // int64
      linkedInvoice?: number | null; // int64
    }
    export interface EditIncomeTaxPaymentDto {
      datePaid: string; // date
      reportingDate?: string | null; // date
      amount: number; // int64
      attachments?: number /* int64 */ [] | null;
      notes?: string | null;
      title: string;
    }
    export interface EditInvoiceDto {
      title: string;
      customer: number; // int64
      dateIssued: string; // date
      dateSent?: string | null; // date
      datePaid?: string | null; // date
      dateCancelled?: string | null; // date
      dueDate: string; // date
      currency: string;
      amount: number; // int64
      attachments?: number /* int64 */ [] | null;
      notes?: string | null;
      generalTax?: number | null; // int64
    }
    export interface EditWorkspaceDto {
      name: string;
    }
    export interface ErrorResponse {
      errorId: string;
    }
    export interface ExpenseAmountsDto {
      originalAmountInDefaultCurrency?: number | null; // int64
      adjustedAmountInDefaultCurrency?: number | null; // int64
    }
    export interface ExpenseDto {
      category?: number | null; // int64
      title: string;
      timeRecorded: string; // date-time
      datePaid: string; // date
      currency: string;
      originalAmount: number; // int64
      attachments: number /* int64 */ [];
      percentOnBusiness: number; // int32
      notes?: string | null;
      id: number; // int64
      version: number; // int32
      status: "FINALIZED" | "PENDING_CONVERSION" | "PENDING_CONVERSION_FOR_TAXATION_PURPOSES";
      generalTax?: number | null; // int64
      generalTaxRateInBps?: number | null; // int32
      generalTaxAmount?: number | null; // int64
      convertedAmounts: ExpenseAmountsDto;
      incomeTaxableAmounts: ExpenseAmountsDto;
      useDifferentExchangeRateForIncomeTaxPurposes: boolean;
    }
    export interface FinalizedTaxSummaryItemDto {
      tax: number; // int64
      taxAmount: number; // int64
      includedItemsNumber: number; // int64
      includedItemsAmount: number; // int64
    }
    export interface GeneralTaxDto {
      title: string;
      id: number; // int64
      version: number; // int32
      description?: string | null;
      rateInBps: number; // int32
    }
    export interface GeneralTaxReportDto {
      finalizedCollectedTaxes: FinalizedTaxSummaryItemDto[];
      finalizedPaidTaxes: FinalizedTaxSummaryItemDto[];
      pendingCollectedTaxes: PendingTaxSummaryItemDto[];
      pendingPaidTaxes: PendingTaxSummaryItemDto[];
    }
    export interface GetDownloadTokenResponse {
      token: string;
    }
    export interface GoogleDriveStorageIntegrationStatus {
      folderId?: string | null;
      folderName?: string | null;
      authorizationUrl?: string | null;
      authorizationRequired: boolean;
    }
    export interface I18nSettingsDto {
      locale: string;
      language: string;
    }
    export interface IncomeAmountsDto {
      originalAmountInDefaultCurrency?: number | null; // int64
      adjustedAmountInDefaultCurrency?: number | null; // int64
    }
    export interface IncomeDto {
      category?: number | null; // int64
      title: string;
      timeRecorded: string; // date-time
      dateReceived: string; // date
      currency: string;
      originalAmount: number; // int64
      attachments: number /* int64 */ [];
      notes?: string | null;
      id: number; // int64
      version: number; // int32
      status: "FINALIZED" | "PENDING_CONVERSION" | "PENDING_CONVERSION_FOR_TAXATION_PURPOSES";
      linkedInvoice?: number | null; // int64
      generalTax?: number | null; // int64
      generalTaxRateInBps?: number | null; // int32
      generalTaxAmount?: number | null; // int64
      convertedAmounts: IncomeAmountsDto;
      incomeTaxableAmounts: IncomeAmountsDto;
      useDifferentExchangeRateForIncomeTaxPurposes: boolean;
    }
    export interface IncomeExpensesStatisticsItemDto {
      categoryId?: number | null; // int64
      totalAmount: number; // int64
      finalizedCount: number; // int64
      pendingCount: number; // int64
      currencyExchangeDifference: number; // int64
    }
    export interface IncomeTaxPaymentDto {
      id: number; // int64
      version: number; // int32
      title: string;
      timeRecorded: string; // date-time
      datePaid: string; // date
      reportingDate: string; // date
      amount: number; // int64
      attachments: number /* int64 */ [];
      notes?: string | null;
    }
    export interface IncomeTaxPaymentsStatisticsDto {
      totalTaxPayments: number; // int64
    }
    export interface IncomesExpensesStatisticsDto {
      items: IncomeExpensesStatisticsItemDto[];
      totalAmount: number; // int64
      finalizedCount: number; // int64
      pendingCount: number; // int64
      currencyExchangeDifference: number; // int64
    }
    export interface InvoiceDto {
      title: string;
      customer: number; // int64
      timeRecorded: string; // date-time
      dateIssued: string; // date
      dateSent?: string | null; // date
      datePaid?: string | null; // date
      dueDate: string; // date
      currency: string;
      amount: number; // int64
      attachments: number /* int64 */ [];
      notes?: string | null;
      id: number; // int64
      version: number; // int32
      status: "DRAFT" | "SENT" | "OVERDUE" | "PAID" | "CANCELLED";
      generalTax?: number | null; // int64
    }
    export interface LoginRequest {
      userName: string;
      password: string;
      rememberMe: boolean;
    }
    export interface OAuth2AuthorizationCallbackRequest {
      code?: string | null;
      error?: string | null;
      state: string;
    }
    export interface PendingTaxSummaryItemDto {
      tax: number; // int64
      includedItemsNumber: number; // int64
    }
    export interface ProfileDto {
      userName: string;
      documentsStorage?: string | null;
      i18n: I18nSettingsDto;
    }
    export interface SaveSharedWorkspaceRequestDto {
      token: string;
    }
    export interface TokenResponse {
      token: string;
    }
    export interface UpdateProfileRequestDto {
      documentsStorage?: string | null;
      i18n: I18nSettingsDto;
    }
    export interface UserDto {
      userName: string;
      id?: number | null; // int64
      version: number; // int32
      admin: boolean;
    }
    export interface WorkspaceAccessTokenDto {
      validTill: string; // date-time
      revoked: boolean;
      token: string;
      id: number; // int64
      version: number; // int32
    }
    export interface WorkspaceDto {
      id?: number | null; // int64
      version: number; // int32
      name: string;
      taxEnabled: boolean;
      multiCurrencyEnabled: boolean;
      defaultCurrency: string;
      editable: boolean;
    }
  }
}
export namespace Paths {
  export namespace AuthCallback {
    export type RequestBody = Components.Schemas.OAuth2AuthorizationCallbackRequest;
  }
  export namespace CancelInvoice {
    export namespace Parameters {
      export type InvoiceId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      invoiceId: Parameters.InvoiceId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.InvoiceDto;
    }
  }
  export namespace CreateAccessToken {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.CreateWorkspaceAccessTokenDto;
    export namespace Responses {
      export type $200 = Components.Schemas.WorkspaceAccessTokenDto;
    }
  }
  export namespace CreateCategory {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.CreateCategoryDto;
    export namespace Responses {
      export type $200 = Components.Schemas.CategoryDto;
    }
  }
  export namespace CreateCustomer {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditCustomerDto;
    export namespace Responses {
      export type $200 = Components.Schemas.CustomerDto;
    }
  }
  export namespace CreateExpense {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditExpenseDto;
    export namespace Responses {
      export type $200 = Components.Schemas.ExpenseDto;
    }
  }
  export namespace CreateIncome {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditIncomeDto;
    export namespace Responses {
      export type $200 = Components.Schemas.IncomeDto;
    }
  }
  export namespace CreateInvoice {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditInvoiceDto;
    export namespace Responses {
      export type $200 = Components.Schemas.InvoiceDto;
    }
  }
  export namespace CreateTax {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditGeneralTaxDto;
    export namespace Responses {
      export type $200 = Components.Schemas.GeneralTaxDto;
    }
  }
  export namespace CreateTaxPayment {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditIncomeTaxPaymentDto;
    export namespace Responses {
      export type $200 = Components.Schemas.IncomeTaxPaymentDto;
    }
  }
  export namespace CreateUser {
    export type RequestBody = Components.Schemas.CreateUserDto;
    export namespace Responses {
      export type $200 = Components.Schemas.UserDto;
    }
  }
  export namespace CreateWorkspace {
    export type RequestBody = Components.Schemas.CreateWorkspaceDto;
    export namespace Responses {
      export type $200 = Components.Schemas.WorkspaceDto;
    }
  }
  export namespace EditWorkspace {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditWorkspaceDto;
    export namespace Responses {
      export type $200 = Components.Schemas.WorkspaceDto;
    }
  }
  export namespace GetAccessTokens {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.ApiPageWorkspaceAccessTokenDto;
    }
  }
  export namespace GetCategories {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.ApiPageCategoryDto;
    }
  }
  export namespace GetContent {
    export namespace Parameters {
      export type Token = string;
    }
    export interface QueryParameters {
      token: Parameters.Token;
    }
    export namespace Responses {
      export type $200 = Components.Schemas.DataBuffer[];
    }
  }
  export namespace GetCurrenciesShortlist {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export namespace Responses {
      export type $200 = string[];
    }
  }
  export namespace GetCustomer {
    export namespace Parameters {
      export type CustomerId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      customerId: Parameters.CustomerId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.CustomerDto;
    }
  }
  export namespace GetCustomers {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.ApiPageCustomerDto;
    }
  }
  export namespace GetDocumentContent {
    export namespace Parameters {
      export type DocumentId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      documentId: Parameters.DocumentId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.DataBuffer[];
    }
  }
  export namespace GetDocuments {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.ApiPageDocumentDto;
    }
  }
  export namespace GetDocumentsStorageStatus {
    export namespace Responses {
      export type $200 = Components.Schemas.DocumentsStorageStatus;
    }
  }
  export namespace GetDownloadToken {
    export namespace Parameters {
      export type DocumentId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      documentId: Parameters.DocumentId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.GetDownloadTokenResponse;
    }
  }
  export namespace GetExpense {
    export namespace Parameters {
      export type ExpenseId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      expenseId: Parameters.ExpenseId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.ExpenseDto;
    }
  }
  export namespace GetExpenses {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.ApiPageExpenseDto;
    }
  }
  export namespace GetExpensesStatistics {
    export namespace Parameters {
      export type FromDate = string; // date
      export type ToDate = string; // date
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export interface QueryParameters {
      fromDate: Parameters.FromDate; // date
      toDate: Parameters.ToDate; // date
    }
    export namespace Responses {
      export type $200 = Components.Schemas.IncomesExpensesStatisticsDto;
    }
  }
  export namespace GetGeneralTaxReport {
    export namespace Parameters {
      export type FromDate = string; // date
      export type ToDate = string; // date
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export interface QueryParameters {
      fromDate: Parameters.FromDate; // date
      toDate: Parameters.ToDate; // date
    }
    export namespace Responses {
      export type $200 = Components.Schemas.GeneralTaxReportDto;
    }
  }
  export namespace GetIncome {
    export namespace Parameters {
      export type IncomeId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      incomeId: Parameters.IncomeId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.IncomeDto;
    }
  }
  export namespace GetIncomes {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.ApiPageIncomeDto;
    }
  }
  export namespace GetIncomesStatistics {
    export namespace Parameters {
      export type FromDate = string; // date
      export type ToDate = string; // date
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export interface QueryParameters {
      fromDate: Parameters.FromDate; // date
      toDate: Parameters.ToDate; // date
    }
    export namespace Responses {
      export type $200 = Components.Schemas.IncomesExpensesStatisticsDto;
    }
  }
  export namespace GetIntegrationStatus {
    export namespace Responses {
      export type $200 = Components.Schemas.GoogleDriveStorageIntegrationStatus;
    }
  }
  export namespace GetInvoice {
    export namespace Parameters {
      export type InvoiceId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      invoiceId: Parameters.InvoiceId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.InvoiceDto;
    }
  }
  export namespace GetInvoices {
    export namespace Parameters {
      export type FreeSearchTextEq = string;
      export type PageNumber = number; // int32
      export type PageSize = number; // int32
      export type SortBy = "_NOT_SUPPORTED";
      export type SortOrder = "asc" | "desc";
      export type StatusIn = ("DRAFT" | "SENT" | "OVERDUE" | "PAID" | "CANCELLED")[];
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export interface QueryParameters {
      sortBy?: Parameters.SortBy | null;
      "freeSearchText[eq]"?: Parameters.FreeSearchTextEq | null;
      "status[in]"?: Parameters.StatusIn | null;
      pageNumber?: Parameters.PageNumber | null; // int32
      pageSize?: Parameters.PageSize | null; // int32
      sortOrder?: Parameters.SortOrder | null;
    }
    export namespace Responses {
      export type $200 = Components.Schemas.ApiPageInvoiceDto;
    }
  }
  export namespace GetProfile {
    export namespace Responses {
      export type $200 = Components.Schemas.ProfileDto;
    }
  }
  export namespace GetPushNotificationMessages {
    export namespace Responses {
      export type $200 = Components.Schemas.CurrentUserPushNotificationMessage[];
    }
  }
  export namespace GetSharedWorkspaces {
    export namespace Responses {
      export type $200 = Components.Schemas.WorkspaceDto[];
    }
  }
  export namespace GetTax {
    export namespace Parameters {
      export type TaxId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      taxId: Parameters.TaxId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.GeneralTaxDto;
    }
  }
  export namespace GetTaxPayment {
    export namespace Parameters {
      export type TaxPaymentId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      taxPaymentId: Parameters.TaxPaymentId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.IncomeTaxPaymentDto;
    }
  }
  export namespace GetTaxPayments {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.ApiPageIncomeTaxPaymentDto;
    }
  }
  export namespace GetTaxPaymentsStatistics {
    export namespace Parameters {
      export type FromDate = string; // date
      export type ToDate = string; // date
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export interface QueryParameters {
      fromDate: Parameters.FromDate; // date
      toDate: Parameters.ToDate; // date
    }
    export namespace Responses {
      export type $200 = Components.Schemas.IncomeTaxPaymentsStatisticsDto;
    }
  }
  export namespace GetTaxes {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.ApiPageGeneralTaxDto;
    }
  }
  export namespace GetUsers {
    export namespace Responses {
      export type $200 = Components.Schemas.ApiPageUserDto;
    }
  }
  export namespace GetWorkspaces {
    export namespace Responses {
      export type $200 = Components.Schemas.WorkspaceDto[];
    }
  }
  export namespace Login {
    export type RequestBody = Components.Schemas.LoginRequest;
    export namespace Responses {
      export type $200 = Components.Schemas.TokenResponse;
    }
  }
  export namespace LoginBySharedWorkspaceToken {
    export namespace Parameters {
      export type SharedWorkspaceToken = string;
    }
    export interface QueryParameters {
      sharedWorkspaceToken: Parameters.SharedWorkspaceToken;
    }
    export namespace Responses {
      export type $200 = Components.Schemas.TokenResponse;
    }
  }
  export namespace Logout {
    export namespace Responses {
      export type $200 = string;
    }
  }
  export namespace RefreshToken {
    export interface CookieParameters {
      refreshToken?: Parameters.RefreshToken | null;
    }
    export namespace Parameters {
      export type RefreshToken = string;
    }
    export namespace Responses {
      export type $200 = Components.Schemas.TokenResponse;
    }
  }
  export namespace SaveSharedWorkspace {
    export type RequestBody = Components.Schemas.SaveSharedWorkspaceRequestDto;
    export namespace Responses {
      export type $200 = Components.Schemas.WorkspaceDto;
    }
  }
  export namespace UpdateCustomer {
    export namespace Parameters {
      export type CustomerId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      customerId: Parameters.CustomerId; // int64
    }
    export type RequestBody = Components.Schemas.EditCustomerDto;
    export namespace Responses {
      export type $200 = Components.Schemas.CustomerDto;
    }
  }
  export namespace UpdateExpense {
    export namespace Parameters {
      export type ExpenseId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      expenseId: Parameters.ExpenseId; // int64
    }
    export type RequestBody = Components.Schemas.EditExpenseDto;
    export namespace Responses {
      export type $200 = Components.Schemas.ExpenseDto;
    }
  }
  export namespace UpdateIncome {
    export namespace Parameters {
      export type IncomeId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      incomeId: Parameters.IncomeId; // int64
    }
    export type RequestBody = Components.Schemas.EditIncomeDto;
    export namespace Responses {
      export type $200 = Components.Schemas.IncomeDto;
    }
  }
  export namespace UpdateInvoice {
    export namespace Parameters {
      export type InvoiceId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      invoiceId: Parameters.InvoiceId; // int64
    }
    export type RequestBody = Components.Schemas.EditInvoiceDto;
    export namespace Responses {
      export type $200 = Components.Schemas.InvoiceDto;
    }
  }
  export namespace UpdateProfile {
    export type RequestBody = Components.Schemas.UpdateProfileRequestDto;
    export namespace Responses {
      export type $200 = Components.Schemas.ProfileDto;
    }
  }
  export namespace UpdateTax {
    export namespace Parameters {
      export type TaxId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      taxId: Parameters.TaxId; // int64
    }
    export type RequestBody = Components.Schemas.EditGeneralTaxDto;
    export namespace Responses {
      export type $200 = Components.Schemas.GeneralTaxDto;
    }
  }
  export namespace UpdateTaxPayment {
    export namespace Parameters {
      export type TaxPaymentId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      taxPaymentId: Parameters.TaxPaymentId; // int64
    }
    export type RequestBody = Components.Schemas.EditIncomeTaxPaymentDto;
    export namespace Responses {
      export type $200 = Components.Schemas.IncomeTaxPaymentDto;
    }
  }
  export namespace UploadNewDocument {
    export namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export namespace Responses {
      export type $200 = Components.Schemas.DocumentDto;
    }
  }
}
          
export interface OperationMethods {
  /**
   * editWorkspace
   */
  'editWorkspace'(
    parameters?: Parameters<Paths.EditWorkspace.PathParameters> | null,
    data?: Paths.EditWorkspace.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.EditWorkspace.Responses.$200>
  /**
   * getInvoice
   */
  'getInvoice'(
    parameters?: Parameters<Paths.GetInvoice.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetInvoice.Responses.$200>
  /**
   * updateInvoice
   */
  'updateInvoice'(
    parameters?: Parameters<Paths.UpdateInvoice.PathParameters> | null,
    data?: Paths.UpdateInvoice.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.UpdateInvoice.Responses.$200>
  /**
   * getIncome
   */
  'getIncome'(
    parameters?: Parameters<Paths.GetIncome.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetIncome.Responses.$200>
  /**
   * updateIncome
   */
  'updateIncome'(
    parameters?: Parameters<Paths.UpdateIncome.PathParameters> | null,
    data?: Paths.UpdateIncome.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.UpdateIncome.Responses.$200>
  /**
   * getTaxPayment
   */
  'getTaxPayment'(
    parameters?: Parameters<Paths.GetTaxPayment.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetTaxPayment.Responses.$200>
  /**
   * updateTaxPayment
   */
  'updateTaxPayment'(
    parameters?: Parameters<Paths.UpdateTaxPayment.PathParameters> | null,
    data?: Paths.UpdateTaxPayment.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.UpdateTaxPayment.Responses.$200>
  /**
   * getTax
   */
  'getTax'(
    parameters?: Parameters<Paths.GetTax.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetTax.Responses.$200>
  /**
   * updateTax
   */
  'updateTax'(
    parameters?: Parameters<Paths.UpdateTax.PathParameters> | null,
    data?: Paths.UpdateTax.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.UpdateTax.Responses.$200>
  /**
   * getExpense
   */
  'getExpense'(
    parameters?: Parameters<Paths.GetExpense.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetExpense.Responses.$200>
  /**
   * updateExpense
   */
  'updateExpense'(
    parameters?: Parameters<Paths.UpdateExpense.PathParameters> | null,
    data?: Paths.UpdateExpense.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.UpdateExpense.Responses.$200>
  /**
   * getCustomer
   */
  'getCustomer'(
    parameters?: Parameters<Paths.GetCustomer.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetCustomer.Responses.$200>
  /**
   * updateCustomer
   */
  'updateCustomer'(
    parameters?: Parameters<Paths.UpdateCustomer.PathParameters> | null,
    data?: Paths.UpdateCustomer.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.UpdateCustomer.Responses.$200>
  /**
   * getProfile
   */
  'getProfile'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetProfile.Responses.$200>
  /**
   * updateProfile
   */
  'updateProfile'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.UpdateProfile.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.UpdateProfile.Responses.$200>
  /**
   * getWorkspaces
   */
  'getWorkspaces'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetWorkspaces.Responses.$200>
  /**
   * createWorkspace
   */
  'createWorkspace'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.CreateWorkspace.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.CreateWorkspace.Responses.$200>
  /**
   * getAccessTokens
   */
  'getAccessTokens'(
    parameters?: Parameters<Paths.GetAccessTokens.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetAccessTokens.Responses.$200>
  /**
   * createAccessToken
   */
  'createAccessToken'(
    parameters?: Parameters<Paths.CreateAccessToken.PathParameters> | null,
    data?: Paths.CreateAccessToken.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.CreateAccessToken.Responses.$200>
  /**
   * getInvoices
   */
  'getInvoices'(
    parameters?: Parameters<Paths.GetInvoices.PathParameters & Paths.GetInvoices.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetInvoices.Responses.$200>
  /**
   * createInvoice
   */
  'createInvoice'(
    parameters?: Parameters<Paths.CreateInvoice.PathParameters> | null,
    data?: Paths.CreateInvoice.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.CreateInvoice.Responses.$200>
  /**
   * cancelInvoice
   */
  'cancelInvoice'(
    parameters?: Parameters<Paths.CancelInvoice.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.CancelInvoice.Responses.$200>
  /**
   * getIncomes
   */
  'getIncomes'(
    parameters?: Parameters<Paths.GetIncomes.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetIncomes.Responses.$200>
  /**
   * createIncome
   */
  'createIncome'(
    parameters?: Parameters<Paths.CreateIncome.PathParameters> | null,
    data?: Paths.CreateIncome.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.CreateIncome.Responses.$200>
  /**
   * getTaxPayments
   */
  'getTaxPayments'(
    parameters?: Parameters<Paths.GetTaxPayments.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetTaxPayments.Responses.$200>
  /**
   * createTaxPayment
   */
  'createTaxPayment'(
    parameters?: Parameters<Paths.CreateTaxPayment.PathParameters> | null,
    data?: Paths.CreateTaxPayment.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.CreateTaxPayment.Responses.$200>
  /**
   * getTaxes
   */
  'getTaxes'(
    parameters?: Parameters<Paths.GetTaxes.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetTaxes.Responses.$200>
  /**
   * createTax
   */
  'createTax'(
    parameters?: Parameters<Paths.CreateTax.PathParameters> | null,
    data?: Paths.CreateTax.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.CreateTax.Responses.$200>
  /**
   * getExpenses
   */
  'getExpenses'(
    parameters?: Parameters<Paths.GetExpenses.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetExpenses.Responses.$200>
  /**
   * createExpense
   */
  'createExpense'(
    parameters?: Parameters<Paths.CreateExpense.PathParameters> | null,
    data?: Paths.CreateExpense.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.CreateExpense.Responses.$200>
  /**
   * getDocuments
   */
  'getDocuments'(
    parameters?: Parameters<Paths.GetDocuments.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetDocuments.Responses.$200>
  /**
   * uploadNewDocument
   */
  'uploadNewDocument'(
    parameters?: Parameters<Paths.UploadNewDocument.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.UploadNewDocument.Responses.$200>
  /**
   * getCustomers
   */
  'getCustomers'(
    parameters?: Parameters<Paths.GetCustomers.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetCustomers.Responses.$200>
  /**
   * createCustomer
   */
  'createCustomer'(
    parameters?: Parameters<Paths.CreateCustomer.PathParameters> | null,
    data?: Paths.CreateCustomer.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.CreateCustomer.Responses.$200>
  /**
   * getCategories
   */
  'getCategories'(
    parameters?: Parameters<Paths.GetCategories.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetCategories.Responses.$200>
  /**
   * createCategory
   */
  'createCategory'(
    parameters?: Parameters<Paths.CreateCategory.PathParameters> | null,
    data?: Paths.CreateCategory.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.CreateCategory.Responses.$200>
  /**
   * getUsers
   */
  'getUsers'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetUsers.Responses.$200>
  /**
   * createUser
   */
  'createUser'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.CreateUser.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.CreateUser.Responses.$200>
  /**
   * getSharedWorkspaces
   */
  'getSharedWorkspaces'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetSharedWorkspaces.Responses.$200>
  /**
   * saveSharedWorkspace
   */
  'saveSharedWorkspace'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.SaveSharedWorkspace.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.SaveSharedWorkspace.Responses.$200>
  /**
   * refreshToken
   */
  'refreshToken'(
    parameters?: Parameters<Paths.RefreshToken.CookieParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.RefreshToken.Responses.$200>
  /**
   * authCallback
   */
  'authCallback'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.AuthCallback.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * logout
   */
  'logout'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.Logout.Responses.$200>
  /**
   * login
   */
  'login'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.Login.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.Login.Responses.$200>
  /**
   * loginBySharedWorkspaceToken
   */
  'loginBySharedWorkspaceToken'(
    parameters?: Parameters<Paths.LoginBySharedWorkspaceToken.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.LoginBySharedWorkspaceToken.Responses.$200>
  /**
   * getIncomesStatistics
   */
  'getIncomesStatistics'(
    parameters?: Parameters<Paths.GetIncomesStatistics.PathParameters & Paths.GetIncomesStatistics.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetIncomesStatistics.Responses.$200>
  /**
   * getTaxPaymentsStatistics
   */
  'getTaxPaymentsStatistics'(
    parameters?: Parameters<Paths.GetTaxPaymentsStatistics.PathParameters & Paths.GetTaxPaymentsStatistics.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetTaxPaymentsStatistics.Responses.$200>
  /**
   * getExpensesStatistics
   */
  'getExpensesStatistics'(
    parameters?: Parameters<Paths.GetExpensesStatistics.PathParameters & Paths.GetExpensesStatistics.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetExpensesStatistics.Responses.$200>
  /**
   * getCurrenciesShortlist
   */
  'getCurrenciesShortlist'(
    parameters?: Parameters<Paths.GetCurrenciesShortlist.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetCurrenciesShortlist.Responses.$200>
  /**
   * getGeneralTaxReport
   */
  'getGeneralTaxReport'(
    parameters?: Parameters<Paths.GetGeneralTaxReport.PathParameters & Paths.GetGeneralTaxReport.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetGeneralTaxReport.Responses.$200>
  /**
   * getDownloadToken
   */
  'getDownloadToken'(
    parameters?: Parameters<Paths.GetDownloadToken.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetDownloadToken.Responses.$200>
  /**
   * getDocumentContent
   */
  'getDocumentContent'(
    parameters?: Parameters<Paths.GetDocumentContent.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetDocumentContent.Responses.$200>
  /**
   * getIntegrationStatus
   */
  'getIntegrationStatus'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetIntegrationStatus.Responses.$200>
  /**
   * getPushNotificationMessages
   */
  'getPushNotificationMessages'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetPushNotificationMessages.Responses.$200>
  /**
   * getDocumentsStorageStatus
   */
  'getDocumentsStorageStatus'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetDocumentsStorageStatus.Responses.$200>
  /**
   * getContent
   */
  'getContent'(
    parameters?: Parameters<Paths.GetContent.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<Paths.GetContent.Responses.$200>
}
export interface PathsDictionary {
  ['/api/workspaces/{workspaceId}']: {
    /**
     * editWorkspace
     */
    'put'(
      parameters?: Parameters<Paths.EditWorkspace.PathParameters> | null,
      data?: Paths.EditWorkspace.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.EditWorkspace.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/invoices/{invoiceId}']: {
    /**
     * getInvoice
     */
    'get'(
      parameters?: Parameters<Paths.GetInvoice.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetInvoice.Responses.$200>
    /**
     * updateInvoice
     */
    'put'(
      parameters?: Parameters<Paths.UpdateInvoice.PathParameters> | null,
      data?: Paths.UpdateInvoice.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.UpdateInvoice.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/incomes/{incomeId}']: {
    /**
     * getIncome
     */
    'get'(
      parameters?: Parameters<Paths.GetIncome.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetIncome.Responses.$200>
    /**
     * updateIncome
     */
    'put'(
      parameters?: Parameters<Paths.UpdateIncome.PathParameters> | null,
      data?: Paths.UpdateIncome.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.UpdateIncome.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/income-tax-payments/{taxPaymentId}']: {
    /**
     * getTaxPayment
     */
    'get'(
      parameters?: Parameters<Paths.GetTaxPayment.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetTaxPayment.Responses.$200>
    /**
     * updateTaxPayment
     */
    'put'(
      parameters?: Parameters<Paths.UpdateTaxPayment.PathParameters> | null,
      data?: Paths.UpdateTaxPayment.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.UpdateTaxPayment.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/general-taxes/{taxId}']: {
    /**
     * getTax
     */
    'get'(
      parameters?: Parameters<Paths.GetTax.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetTax.Responses.$200>
    /**
     * updateTax
     */
    'put'(
      parameters?: Parameters<Paths.UpdateTax.PathParameters> | null,
      data?: Paths.UpdateTax.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.UpdateTax.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/expenses/{expenseId}']: {
    /**
     * getExpense
     */
    'get'(
      parameters?: Parameters<Paths.GetExpense.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetExpense.Responses.$200>
    /**
     * updateExpense
     */
    'put'(
      parameters?: Parameters<Paths.UpdateExpense.PathParameters> | null,
      data?: Paths.UpdateExpense.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.UpdateExpense.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/customers/{customerId}']: {
    /**
     * getCustomer
     */
    'get'(
      parameters?: Parameters<Paths.GetCustomer.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetCustomer.Responses.$200>
    /**
     * updateCustomer
     */
    'put'(
      parameters?: Parameters<Paths.UpdateCustomer.PathParameters> | null,
      data?: Paths.UpdateCustomer.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.UpdateCustomer.Responses.$200>
  }
  ['/api/profile']: {
    /**
     * getProfile
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetProfile.Responses.$200>
    /**
     * updateProfile
     */
    'put'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.UpdateProfile.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.UpdateProfile.Responses.$200>
  }
  ['/api/workspaces']: {
    /**
     * getWorkspaces
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetWorkspaces.Responses.$200>
    /**
     * createWorkspace
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.CreateWorkspace.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.CreateWorkspace.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/workspace-access-tokens']: {
    /**
     * getAccessTokens
     */
    'get'(
      parameters?: Parameters<Paths.GetAccessTokens.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetAccessTokens.Responses.$200>
    /**
     * createAccessToken
     */
    'post'(
      parameters?: Parameters<Paths.CreateAccessToken.PathParameters> | null,
      data?: Paths.CreateAccessToken.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.CreateAccessToken.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/invoices']: {
    /**
     * getInvoices
     */
    'get'(
      parameters?: Parameters<Paths.GetInvoices.PathParameters & Paths.GetInvoices.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetInvoices.Responses.$200>
    /**
     * createInvoice
     */
    'post'(
      parameters?: Parameters<Paths.CreateInvoice.PathParameters> | null,
      data?: Paths.CreateInvoice.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.CreateInvoice.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/invoices/{invoiceId}/cancel']: {
    /**
     * cancelInvoice
     */
    'post'(
      parameters?: Parameters<Paths.CancelInvoice.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.CancelInvoice.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/incomes']: {
    /**
     * getIncomes
     */
    'get'(
      parameters?: Parameters<Paths.GetIncomes.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetIncomes.Responses.$200>
    /**
     * createIncome
     */
    'post'(
      parameters?: Parameters<Paths.CreateIncome.PathParameters> | null,
      data?: Paths.CreateIncome.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.CreateIncome.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/income-tax-payments']: {
    /**
     * getTaxPayments
     */
    'get'(
      parameters?: Parameters<Paths.GetTaxPayments.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetTaxPayments.Responses.$200>
    /**
     * createTaxPayment
     */
    'post'(
      parameters?: Parameters<Paths.CreateTaxPayment.PathParameters> | null,
      data?: Paths.CreateTaxPayment.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.CreateTaxPayment.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/general-taxes']: {
    /**
     * getTaxes
     */
    'get'(
      parameters?: Parameters<Paths.GetTaxes.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetTaxes.Responses.$200>
    /**
     * createTax
     */
    'post'(
      parameters?: Parameters<Paths.CreateTax.PathParameters> | null,
      data?: Paths.CreateTax.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.CreateTax.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/expenses']: {
    /**
     * getExpenses
     */
    'get'(
      parameters?: Parameters<Paths.GetExpenses.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetExpenses.Responses.$200>
    /**
     * createExpense
     */
    'post'(
      parameters?: Parameters<Paths.CreateExpense.PathParameters> | null,
      data?: Paths.CreateExpense.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.CreateExpense.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/documents']: {
    /**
     * getDocuments
     */
    'get'(
      parameters?: Parameters<Paths.GetDocuments.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetDocuments.Responses.$200>
    /**
     * uploadNewDocument
     */
    'post'(
      parameters?: Parameters<Paths.UploadNewDocument.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.UploadNewDocument.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/customers']: {
    /**
     * getCustomers
     */
    'get'(
      parameters?: Parameters<Paths.GetCustomers.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetCustomers.Responses.$200>
    /**
     * createCustomer
     */
    'post'(
      parameters?: Parameters<Paths.CreateCustomer.PathParameters> | null,
      data?: Paths.CreateCustomer.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.CreateCustomer.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/categories']: {
    /**
     * getCategories
     */
    'get'(
      parameters?: Parameters<Paths.GetCategories.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetCategories.Responses.$200>
    /**
     * createCategory
     */
    'post'(
      parameters?: Parameters<Paths.CreateCategory.PathParameters> | null,
      data?: Paths.CreateCategory.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.CreateCategory.Responses.$200>
  }
  ['/api/users']: {
    /**
     * getUsers
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetUsers.Responses.$200>
    /**
     * createUser
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.CreateUser.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.CreateUser.Responses.$200>
  }
  ['/api/shared-workspaces']: {
    /**
     * getSharedWorkspaces
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetSharedWorkspaces.Responses.$200>
    /**
     * saveSharedWorkspace
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.SaveSharedWorkspace.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.SaveSharedWorkspace.Responses.$200>
  }
  ['/api/auth/token']: {
    /**
     * refreshToken
     */
    'post'(
      parameters?: Parameters<Paths.RefreshToken.CookieParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.RefreshToken.Responses.$200>
  }
  ['/api/auth/oauth2/callback']: {
    /**
     * authCallback
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.AuthCallback.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/auth/logout']: {
    /**
     * logout
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.Logout.Responses.$200>
  }
  ['/api/auth/login']: {
    /**
     * login
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.Login.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.Login.Responses.$200>
  }
  ['/api/auth/login-by-token']: {
    /**
     * loginBySharedWorkspaceToken
     */
    'post'(
      parameters?: Parameters<Paths.LoginBySharedWorkspaceToken.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.LoginBySharedWorkspaceToken.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/statistics/incomes']: {
    /**
     * getIncomesStatistics
     */
    'get'(
      parameters?: Parameters<Paths.GetIncomesStatistics.PathParameters & Paths.GetIncomesStatistics.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetIncomesStatistics.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/statistics/income-tax-payments']: {
    /**
     * getTaxPaymentsStatistics
     */
    'get'(
      parameters?: Parameters<Paths.GetTaxPaymentsStatistics.PathParameters & Paths.GetTaxPaymentsStatistics.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetTaxPaymentsStatistics.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/statistics/expenses']: {
    /**
     * getExpensesStatistics
     */
    'get'(
      parameters?: Parameters<Paths.GetExpensesStatistics.PathParameters & Paths.GetExpensesStatistics.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetExpensesStatistics.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/statistics/currencies-shortlist']: {
    /**
     * getCurrenciesShortlist
     */
    'get'(
      parameters?: Parameters<Paths.GetCurrenciesShortlist.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetCurrenciesShortlist.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/reporting/general-taxes']: {
    /**
     * getGeneralTaxReport
     */
    'get'(
      parameters?: Parameters<Paths.GetGeneralTaxReport.PathParameters & Paths.GetGeneralTaxReport.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetGeneralTaxReport.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/documents/{documentId}/download-token']: {
    /**
     * getDownloadToken
     */
    'get'(
      parameters?: Parameters<Paths.GetDownloadToken.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetDownloadToken.Responses.$200>
  }
  ['/api/workspaces/{workspaceId}/documents/{documentId}/content']: {
    /**
     * getDocumentContent
     */
    'get'(
      parameters?: Parameters<Paths.GetDocumentContent.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetDocumentContent.Responses.$200>
  }
  ['/api/storage/google-drive/status']: {
    /**
     * getIntegrationStatus
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetIntegrationStatus.Responses.$200>
  }
  ['/api/push-notifications']: {
    /**
     * getPushNotificationMessages
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetPushNotificationMessages.Responses.$200>
  }
  ['/api/profile/documents-storage']: {
    /**
     * getDocumentsStorageStatus
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetDocumentsStorageStatus.Responses.$200>
  }
  ['/api/downloads']: {
    /**
     * getContent
     */
    'get'(
      parameters?: Parameters<Paths.GetContent.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<Paths.GetContent.Responses.$200>
  }
}
export type Client = OpenAPIClient<OperationMethods, PathsDictionary>
