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
      id?: number; // int64
      version: number; // int32
      name: string;
      description?: string;
      income: boolean;
      expense: boolean;
    }
    export interface CreateCategoryDto {
      name: string;
      description?: string;
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
    export interface DocumentDto {
      id?: number; // int64
      version: number; // int32
      name: string;
      timeUploaded: string; // date-time
      sizeInBytes?: number; // int64
    }
    export interface DocumentsStorageStatus {
      active: boolean;
    }
    export interface EditCustomerDto {
      name: string;
    }
    export interface EditExpenseDto {
      category?: number; // int64
      datePaid: string; // date
      title: string;
      currency: string;
      originalAmount: number; // int64
      convertedAmountInDefaultCurrency?: number; // int64
      useDifferentExchangeRateForIncomeTaxPurposes: boolean;
      incomeTaxableAmountInDefaultCurrency?: number; // int64
      attachments?: number /* int64 */ [];
      percentOnBusiness?: number; // int32
      notes?: string;
      generalTax?: number; // int64
    }
    export interface EditGeneralTaxDto {
      title: string;
      description?: string;
      rateInBps: number; // int32
    }
    export interface EditIncomeDto {
      category?: number; // int64
      dateReceived: string; // date
      title: string;
      currency: string;
      originalAmount: number; // int64
      convertedAmountInDefaultCurrency?: number; // int64
      useDifferentExchangeRateForIncomeTaxPurposes: boolean;
      incomeTaxableAmountInDefaultCurrency?: number; // int64
      attachments?: number /* int64 */ [];
      notes?: string;
      generalTax?: number; // int64
      linkedInvoice?: number; // int64
    }
    export interface EditIncomeTaxPaymentDto {
      datePaid: string; // date
      reportingDate?: string; // date
      amount: number; // int64
      attachments?: number /* int64 */ [];
      notes?: string;
      title: string;
    }
    export interface EditInvoiceDto {
      title: string;
      customer: number; // int64
      dateIssued: string; // date
      dateSent?: string; // date
      datePaid?: string; // date
      dateCancelled?: string; // date
      dueDate: string; // date
      currency: string;
      amount: number; // int64
      attachments?: number /* int64 */ [];
      notes?: string;
      generalTax?: number; // int64
    }
    export interface EditWorkspaceDto {
      name: string;
    }
    export interface ErrorResponse {
      errorId: string;
    }
    export interface ExpenseAmountsDto {
      originalAmountInDefaultCurrency?: number; // int64
      adjustedAmountInDefaultCurrency?: number; // int64
    }
    export interface ExpenseDto {
      category?: number; // int64
      title: string;
      timeRecorded: string; // date-time
      datePaid: string; // date
      currency: string;
      originalAmount: number; // int64
      attachments: number /* int64 */ [];
      percentOnBusiness: number; // int32
      notes?: string;
      id: number; // int64
      version: number; // int32
      status: "FINALIZED" | "PENDING_CONVERSION" | "PENDING_CONVERSION_FOR_TAXATION_PURPOSES";
      generalTax?: number; // int64
      generalTaxRateInBps?: number; // int32
      generalTaxAmount?: number; // int64
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
    export interface FluxDataBuffer {
      prefetch?: number; // int32
    }
    export interface GeneralTaxDto {
      title: string;
      id: number; // int64
      version: number; // int32
      description?: string;
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
      folderId?: string;
      folderName?: string;
      authorizationUrl?: string;
      authorizationRequired: boolean;
    }
    export interface I18nSettingsDto {
      locale: string;
      language: string;
    }
    export interface IncomeAmountsDto {
      originalAmountInDefaultCurrency?: number; // int64
      adjustedAmountInDefaultCurrency?: number; // int64
    }
    export interface IncomeDto {
      category?: number; // int64
      title: string;
      timeRecorded: string; // date-time
      dateReceived: string; // date
      currency: string;
      originalAmount: number; // int64
      attachments: number /* int64 */ [];
      notes?: string;
      id: number; // int64
      version: number; // int32
      status: "FINALIZED" | "PENDING_CONVERSION" | "PENDING_CONVERSION_FOR_TAXATION_PURPOSES";
      linkedInvoice?: number; // int64
      generalTax?: number; // int64
      generalTaxRateInBps?: number; // int32
      generalTaxAmount?: number; // int64
      convertedAmounts: IncomeAmountsDto;
      incomeTaxableAmounts: IncomeAmountsDto;
      useDifferentExchangeRateForIncomeTaxPurposes: boolean;
    }
    export interface IncomeExpensesStatisticsItemDto {
      categoryId?: number; // int64
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
      notes?: string;
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
      dateSent?: string; // date
      datePaid?: string; // date
      dueDate: string; // date
      currency: string;
      amount: number; // int64
      attachments: number /* int64 */ [];
      notes?: string;
      id: number; // int64
      version: number; // int32
      status: "DRAFT" | "SENT" | "OVERDUE" | "PAID" | "CANCELLED";
      generalTax?: number; // int64
    }
    export interface LoginRequest {
      userName: string;
      password: string;
      rememberMe: boolean;
    }
    export interface OAuth2AuthorizationCallbackRequest {
      code?: string;
      error?: string;
      state: string;
    }
    export interface PendingTaxSummaryItemDto {
      tax: number; // int64
      includedItemsNumber: number; // int64
    }
    export interface ProfileDto {
      userName: string;
      documentsStorage?: string;
      i18n: I18nSettingsDto;
    }
    export interface SaveSharedWorkspaceRequestDto {
      token: string;
    }
    export interface TokenResponse {
      token: string;
    }
    export interface UpdateProfileRequestDto {
      documentsStorage?: string;
      i18n: I18nSettingsDto;
    }
    export interface UserDto {
      userName: string;
      id?: number; // int64
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
      id?: number; // int64
      version: number; // int32
      name: string;
      taxEnabled: boolean;
      multiCurrencyEnabled: boolean;
      defaultCurrency: string;
      editable: boolean;
    }
  }
}
declare namespace Paths {
  namespace AuthCallback {
    export type RequestBody = Components.Schemas.OAuth2AuthorizationCallbackRequest;
  }
  namespace CancelInvoice {
    namespace Parameters {
      export type InvoiceId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      invoiceId: Parameters.InvoiceId; // int64
    }
  }
  namespace CreateCategory {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.CreateCategoryDto;
  }
  namespace CreateCustomer {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditCustomerDto;
  }
  namespace CreateExpense {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditExpenseDto;
  }
  namespace CreateIncome {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditIncomeDto;
  }
  namespace CreateInvoice {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditInvoiceDto;
  }
  namespace CreateTax {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditGeneralTaxDto;
  }
  namespace CreateTaxPayment {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditIncomeTaxPaymentDto;
  }
  namespace CreateToken {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.CreateWorkspaceAccessTokenDto;
  }
  namespace CreateUser {
    export type RequestBody = Components.Schemas.CreateUserDto;
  }
  namespace CreateWorkspace {
    export type RequestBody = Components.Schemas.CreateWorkspaceDto;
  }
  namespace EditWorkspace {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
    export type RequestBody = Components.Schemas.EditWorkspaceDto;
  }
  namespace GetAccessTokens {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
  }
  namespace GetCategories {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
  }
  namespace GetContent {
    namespace Parameters {
      export type Token = string;
    }
    export interface QueryParameters {
      token: Parameters.Token;
    }
  }
  namespace GetCurrenciesShortlist {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
  }
  namespace GetCustomer {
    namespace Parameters {
      export type CustomerId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      customerId: Parameters.CustomerId; // int64
    }
  }
  namespace GetCustomers {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
  }
  namespace GetDocumentContent {
    namespace Parameters {
      export type DocumentId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      documentId: Parameters.DocumentId; // int64
    }
  }
  namespace GetDocuments {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
  }
  namespace GetDownloadToken {
    namespace Parameters {
      export type DocumentId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      documentId: Parameters.DocumentId; // int64
    }
  }
  namespace GetExpense {
    namespace Parameters {
      export type ExpenseId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      expenseId: Parameters.ExpenseId; // int64
    }
  }
  namespace GetExpenses {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
  }
  namespace GetExpensesStatistics {
    namespace Parameters {
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
  }
  namespace GetGeneralTaxReport {
    namespace Parameters {
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
  }
  namespace GetIncome {
    namespace Parameters {
      export type IncomeId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      incomeId: Parameters.IncomeId; // int64
    }
  }
  namespace GetIncomes {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
  }
  namespace GetIncomesStatistics {
    namespace Parameters {
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
  }
  namespace GetInvoice {
    namespace Parameters {
      export type InvoiceId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      invoiceId: Parameters.InvoiceId; // int64
    }
  }
  namespace GetInvoices {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
  }
  namespace GetPushNotificationMessages {
    namespace Responses {
      export type $200 = Components.Schemas.CurrentUserPushNotificationMessage[];
    }
  }
  namespace GetTax {
    namespace Parameters {
      export type TaxId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      taxId: Parameters.TaxId; // int64
    }
  }
  namespace GetTaxPayment {
    namespace Parameters {
      export type TaxPaymentId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      taxPaymentId: Parameters.TaxPaymentId; // int64
    }
  }
  namespace GetTaxPayments {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
  }
  namespace GetTaxPaymentsStatistics {
    namespace Parameters {
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
  }
  namespace GetTaxes {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
    }
  }
  namespace Login1 {
    namespace Parameters {
      export type SharedWorkspaceToken = string;
    }
    export interface QueryParameters {
      sharedWorkspaceToken: Parameters.SharedWorkspaceToken;
    }
    export type RequestBody = Components.Schemas.LoginRequest;
  }
  namespace RefreshToken {
    export interface CookieParameters {
      refreshToken?: Parameters.RefreshToken;
    }
    namespace Parameters {
      export type RefreshToken = string;
    }
  }
  namespace SaveSharedWorkspace {
    export type RequestBody = Components.Schemas.SaveSharedWorkspaceRequestDto;
  }
  namespace UpdateCustomer {
    namespace Parameters {
      export type CustomerId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      customerId: Parameters.CustomerId; // int64
    }
    export type RequestBody = Components.Schemas.EditCustomerDto;
  }
  namespace UpdateExpense {
    namespace Parameters {
      export type ExpenseId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      expenseId: Parameters.ExpenseId; // int64
    }
    export type RequestBody = Components.Schemas.EditExpenseDto;
  }
  namespace UpdateIncome {
    namespace Parameters {
      export type IncomeId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      incomeId: Parameters.IncomeId; // int64
    }
    export type RequestBody = Components.Schemas.EditIncomeDto;
  }
  namespace UpdateInvoice {
    namespace Parameters {
      export type InvoiceId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      invoiceId: Parameters.InvoiceId; // int64
    }
    export type RequestBody = Components.Schemas.EditInvoiceDto;
  }
  namespace UpdateProfile {
    export type RequestBody = Components.Schemas.UpdateProfileRequestDto;
  }
  namespace UpdateTax {
    namespace Parameters {
      export type TaxId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      taxId: Parameters.TaxId; // int64
    }
    export type RequestBody = Components.Schemas.EditGeneralTaxDto;
  }
  namespace UpdateTaxPayment {
    namespace Parameters {
      export type TaxPaymentId = number; // int64
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
      taxPaymentId: Parameters.TaxPaymentId; // int64
    }
    export type RequestBody = Components.Schemas.EditIncomeTaxPaymentDto;
  }
  namespace UploadNewDocument {
    namespace Parameters {
      export type WorkspaceId = number; // int64
    }
    export interface PathParameters {
      workspaceId: Parameters.WorkspaceId; // int64
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
  ): OperationResponse<any>
  /**
   * getInvoice
   */
  'getInvoice'(
    parameters?: Parameters<Paths.GetInvoice.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * updateInvoice
   */
  'updateInvoice'(
    parameters?: Parameters<Paths.UpdateInvoice.PathParameters> | null,
    data?: Paths.UpdateInvoice.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getIncome
   */
  'getIncome'(
    parameters?: Parameters<Paths.GetIncome.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * updateIncome
   */
  'updateIncome'(
    parameters?: Parameters<Paths.UpdateIncome.PathParameters> | null,
    data?: Paths.UpdateIncome.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getTaxPayment
   */
  'getTaxPayment'(
    parameters?: Parameters<Paths.GetTaxPayment.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * updateTaxPayment
   */
  'updateTaxPayment'(
    parameters?: Parameters<Paths.UpdateTaxPayment.PathParameters> | null,
    data?: Paths.UpdateTaxPayment.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getTax
   */
  'getTax'(
    parameters?: Parameters<Paths.GetTax.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * updateTax
   */
  'updateTax'(
    parameters?: Parameters<Paths.UpdateTax.PathParameters> | null,
    data?: Paths.UpdateTax.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getExpense
   */
  'getExpense'(
    parameters?: Parameters<Paths.GetExpense.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * updateExpense
   */
  'updateExpense'(
    parameters?: Parameters<Paths.UpdateExpense.PathParameters> | null,
    data?: Paths.UpdateExpense.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getCustomer
   */
  'getCustomer'(
    parameters?: Parameters<Paths.GetCustomer.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * updateCustomer
   */
  'updateCustomer'(
    parameters?: Parameters<Paths.UpdateCustomer.PathParameters> | null,
    data?: Paths.UpdateCustomer.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getProfile
   */
  'getProfile'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * updateProfile
   */
  'updateProfile'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.UpdateProfile.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getWorkspaces
   */
  'getWorkspaces'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * createWorkspace
   */
  'createWorkspace'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.CreateWorkspace.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getAccessTokens
   */
  'getAccessTokens'(
    parameters?: Parameters<Paths.GetAccessTokens.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * createToken
   */
  'createToken'(
    parameters?: Parameters<Paths.CreateToken.PathParameters> | null,
    data?: Paths.CreateToken.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getInvoices
   */
  'getInvoices'(
    parameters?: Parameters<Paths.GetInvoices.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * createInvoice
   */
  'createInvoice'(
    parameters?: Parameters<Paths.CreateInvoice.PathParameters> | null,
    data?: Paths.CreateInvoice.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * cancelInvoice
   */
  'cancelInvoice'(
    parameters?: Parameters<Paths.CancelInvoice.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getIncomes
   */
  'getIncomes'(
    parameters?: Parameters<Paths.GetIncomes.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * createIncome
   */
  'createIncome'(
    parameters?: Parameters<Paths.CreateIncome.PathParameters> | null,
    data?: Paths.CreateIncome.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getTaxPayments
   */
  'getTaxPayments'(
    parameters?: Parameters<Paths.GetTaxPayments.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * createTaxPayment
   */
  'createTaxPayment'(
    parameters?: Parameters<Paths.CreateTaxPayment.PathParameters> | null,
    data?: Paths.CreateTaxPayment.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getTaxes
   */
  'getTaxes'(
    parameters?: Parameters<Paths.GetTaxes.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * createTax
   */
  'createTax'(
    parameters?: Parameters<Paths.CreateTax.PathParameters> | null,
    data?: Paths.CreateTax.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getExpenses
   */
  'getExpenses'(
    parameters?: Parameters<Paths.GetExpenses.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * createExpense
   */
  'createExpense'(
    parameters?: Parameters<Paths.CreateExpense.PathParameters> | null,
    data?: Paths.CreateExpense.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getDocuments
   */
  'getDocuments'(
    parameters?: Parameters<Paths.GetDocuments.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * uploadNewDocument
   */
  'uploadNewDocument'(
    parameters?: Parameters<Paths.UploadNewDocument.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getCustomers
   */
  'getCustomers'(
    parameters?: Parameters<Paths.GetCustomers.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * createCustomer
   */
  'createCustomer'(
    parameters?: Parameters<Paths.CreateCustomer.PathParameters> | null,
    data?: Paths.CreateCustomer.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getCategories
   */
  'getCategories'(
    parameters?: Parameters<Paths.GetCategories.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * createCategory
   */
  'createCategory'(
    parameters?: Parameters<Paths.CreateCategory.PathParameters> | null,
    data?: Paths.CreateCategory.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getUsers
   */
  'getUsers'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * createUser
   */
  'createUser'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.CreateUser.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getSharedWorkspaces
   */
  'getSharedWorkspaces'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * saveSharedWorkspace
   */
  'saveSharedWorkspace'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.SaveSharedWorkspace.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * refreshToken
   */
  'refreshToken'(
    parameters?: Parameters<Paths.RefreshToken.CookieParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
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
  ): OperationResponse<any>
  /**
   * login_1
   */
  'login_1'(
    parameters?: Parameters<Paths.Login1.QueryParameters> | null,
    data?: Paths.Login1.RequestBody,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getIncomesStatistics
   */
  'getIncomesStatistics'(
    parameters?: Parameters<Paths.GetIncomesStatistics.PathParameters & Paths.GetIncomesStatistics.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getTaxPaymentsStatistics
   */
  'getTaxPaymentsStatistics'(
    parameters?: Parameters<Paths.GetTaxPaymentsStatistics.PathParameters & Paths.GetTaxPaymentsStatistics.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getExpensesStatistics
   */
  'getExpensesStatistics'(
    parameters?: Parameters<Paths.GetExpensesStatistics.PathParameters & Paths.GetExpensesStatistics.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getCurrenciesShortlist
   */
  'getCurrenciesShortlist'(
    parameters?: Parameters<Paths.GetCurrenciesShortlist.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getGeneralTaxReport
   */
  'getGeneralTaxReport'(
    parameters?: Parameters<Paths.GetGeneralTaxReport.PathParameters & Paths.GetGeneralTaxReport.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getDownloadToken
   */
  'getDownloadToken'(
    parameters?: Parameters<Paths.GetDownloadToken.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getDocumentContent
   */
  'getDocumentContent'(
    parameters?: Parameters<Paths.GetDocumentContent.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
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
  ): OperationResponse<any>
  /**
   * getContent
   */
  'getContent'(
    parameters?: Parameters<Paths.GetContent.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getIntegrationStatus_1
   */
  'getIntegrationStatus_1'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getIntegrationStatus_6
   */
  'getIntegrationStatus_6'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getIntegrationStatus_3
   */
  'getIntegrationStatus_3'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getIntegrationStatus_4
   */
  'getIntegrationStatus_4'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getIntegrationStatus_5
   */
  'getIntegrationStatus_5'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getIntegrationStatus
   */
  'getIntegrationStatus'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
  /**
   * getIntegrationStatus_2
   */
  'getIntegrationStatus_2'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig
  ): OperationResponse<any>
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
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/invoices/{invoiceId}']: {
    /**
     * getInvoice
     */
    'get'(
      parameters?: Parameters<Paths.GetInvoice.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * updateInvoice
     */
    'put'(
      parameters?: Parameters<Paths.UpdateInvoice.PathParameters> | null,
      data?: Paths.UpdateInvoice.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/incomes/{incomeId}']: {
    /**
     * getIncome
     */
    'get'(
      parameters?: Parameters<Paths.GetIncome.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * updateIncome
     */
    'put'(
      parameters?: Parameters<Paths.UpdateIncome.PathParameters> | null,
      data?: Paths.UpdateIncome.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/income-tax-payments/{taxPaymentId}']: {
    /**
     * getTaxPayment
     */
    'get'(
      parameters?: Parameters<Paths.GetTaxPayment.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * updateTaxPayment
     */
    'put'(
      parameters?: Parameters<Paths.UpdateTaxPayment.PathParameters> | null,
      data?: Paths.UpdateTaxPayment.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/general-taxes/{taxId}']: {
    /**
     * getTax
     */
    'get'(
      parameters?: Parameters<Paths.GetTax.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * updateTax
     */
    'put'(
      parameters?: Parameters<Paths.UpdateTax.PathParameters> | null,
      data?: Paths.UpdateTax.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/expenses/{expenseId}']: {
    /**
     * getExpense
     */
    'get'(
      parameters?: Parameters<Paths.GetExpense.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * updateExpense
     */
    'put'(
      parameters?: Parameters<Paths.UpdateExpense.PathParameters> | null,
      data?: Paths.UpdateExpense.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/customers/{customerId}']: {
    /**
     * getCustomer
     */
    'get'(
      parameters?: Parameters<Paths.GetCustomer.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * updateCustomer
     */
    'put'(
      parameters?: Parameters<Paths.UpdateCustomer.PathParameters> | null,
      data?: Paths.UpdateCustomer.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/profile']: {
    /**
     * getProfile
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * updateProfile
     */
    'put'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.UpdateProfile.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces']: {
    /**
     * getWorkspaces
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * createWorkspace
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.CreateWorkspace.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/workspace-access-tokens']: {
    /**
     * getAccessTokens
     */
    'get'(
      parameters?: Parameters<Paths.GetAccessTokens.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * createToken
     */
    'post'(
      parameters?: Parameters<Paths.CreateToken.PathParameters> | null,
      data?: Paths.CreateToken.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/invoices']: {
    /**
     * getInvoices
     */
    'get'(
      parameters?: Parameters<Paths.GetInvoices.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * createInvoice
     */
    'post'(
      parameters?: Parameters<Paths.CreateInvoice.PathParameters> | null,
      data?: Paths.CreateInvoice.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/invoices/{invoiceId}/cancel']: {
    /**
     * cancelInvoice
     */
    'post'(
      parameters?: Parameters<Paths.CancelInvoice.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/incomes']: {
    /**
     * getIncomes
     */
    'get'(
      parameters?: Parameters<Paths.GetIncomes.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * createIncome
     */
    'post'(
      parameters?: Parameters<Paths.CreateIncome.PathParameters> | null,
      data?: Paths.CreateIncome.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/income-tax-payments']: {
    /**
     * getTaxPayments
     */
    'get'(
      parameters?: Parameters<Paths.GetTaxPayments.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * createTaxPayment
     */
    'post'(
      parameters?: Parameters<Paths.CreateTaxPayment.PathParameters> | null,
      data?: Paths.CreateTaxPayment.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/general-taxes']: {
    /**
     * getTaxes
     */
    'get'(
      parameters?: Parameters<Paths.GetTaxes.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * createTax
     */
    'post'(
      parameters?: Parameters<Paths.CreateTax.PathParameters> | null,
      data?: Paths.CreateTax.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/expenses']: {
    /**
     * getExpenses
     */
    'get'(
      parameters?: Parameters<Paths.GetExpenses.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * createExpense
     */
    'post'(
      parameters?: Parameters<Paths.CreateExpense.PathParameters> | null,
      data?: Paths.CreateExpense.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/documents']: {
    /**
     * getDocuments
     */
    'get'(
      parameters?: Parameters<Paths.GetDocuments.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * uploadNewDocument
     */
    'post'(
      parameters?: Parameters<Paths.UploadNewDocument.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/customers']: {
    /**
     * getCustomers
     */
    'get'(
      parameters?: Parameters<Paths.GetCustomers.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * createCustomer
     */
    'post'(
      parameters?: Parameters<Paths.CreateCustomer.PathParameters> | null,
      data?: Paths.CreateCustomer.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/categories']: {
    /**
     * getCategories
     */
    'get'(
      parameters?: Parameters<Paths.GetCategories.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * createCategory
     */
    'post'(
      parameters?: Parameters<Paths.CreateCategory.PathParameters> | null,
      data?: Paths.CreateCategory.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/users']: {
    /**
     * getUsers
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * createUser
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.CreateUser.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/shared-workspaces']: {
    /**
     * getSharedWorkspaces
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * saveSharedWorkspace
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.SaveSharedWorkspace.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/auth/token']: {
    /**
     * refreshToken
     */
    'post'(
      parameters?: Parameters<Paths.RefreshToken.CookieParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
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
    ): OperationResponse<any>
  }
  ['/api/auth/login']: {
    /**
     * login_1
     */
    'post'(
      parameters?: Parameters<Paths.Login1.QueryParameters> | null,
      data?: Paths.Login1.RequestBody,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/statistics/incomes']: {
    /**
     * getIncomesStatistics
     */
    'get'(
      parameters?: Parameters<Paths.GetIncomesStatistics.PathParameters & Paths.GetIncomesStatistics.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/statistics/income-tax-payments']: {
    /**
     * getTaxPaymentsStatistics
     */
    'get'(
      parameters?: Parameters<Paths.GetTaxPaymentsStatistics.PathParameters & Paths.GetTaxPaymentsStatistics.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/statistics/expenses']: {
    /**
     * getExpensesStatistics
     */
    'get'(
      parameters?: Parameters<Paths.GetExpensesStatistics.PathParameters & Paths.GetExpensesStatistics.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/statistics/currencies-shortlist']: {
    /**
     * getCurrenciesShortlist
     */
    'get'(
      parameters?: Parameters<Paths.GetCurrenciesShortlist.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/reporting/general-taxes']: {
    /**
     * getGeneralTaxReport
     */
    'get'(
      parameters?: Parameters<Paths.GetGeneralTaxReport.PathParameters & Paths.GetGeneralTaxReport.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/documents/{documentId}/download-token']: {
    /**
     * getDownloadToken
     */
    'get'(
      parameters?: Parameters<Paths.GetDownloadToken.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/workspaces/{workspaceId}/documents/{documentId}/content']: {
    /**
     * getDocumentContent
     */
    'get'(
      parameters?: Parameters<Paths.GetDocumentContent.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
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
    ): OperationResponse<any>
  }
  ['/api/downloads']: {
    /**
     * getContent
     */
    'get'(
      parameters?: Parameters<Paths.GetContent.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
  ['/api/storage/google-drive/status']: {
    /**
     * getIntegrationStatus_1
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * getIntegrationStatus_6
     */
    'put'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * getIntegrationStatus_3
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * getIntegrationStatus_5
     */
    'delete'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * getIntegrationStatus
     */
    'options'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * getIntegrationStatus_2
     */
    'head'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
    /**
     * getIntegrationStatus_4
     */
    'patch'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig
    ): OperationResponse<any>
  }
}
export type Client = OpenAPIClient<OperationMethods, PathsDictionary>
