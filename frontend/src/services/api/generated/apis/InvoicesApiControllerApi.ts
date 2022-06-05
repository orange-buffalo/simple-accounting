/* tslint:disable */
/* eslint-disable */
/**
 * OpenAPI definition
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


import * as runtime from '../runtime';
import type {
  ApiPageInvoiceDto,
  EditInvoiceDto,
  ErrorResponse,
  InvoiceDto,
} from '../models';
import {
    ApiPageInvoiceDtoFromJSON,
    ApiPageInvoiceDtoToJSON,
    EditInvoiceDtoFromJSON,
    EditInvoiceDtoToJSON,
    ErrorResponseFromJSON,
    ErrorResponseToJSON,
    InvoiceDtoFromJSON,
    InvoiceDtoToJSON,
} from '../models';

export interface CancelInvoiceRequest {
    workspaceId: number;
    invoiceId: number;
}

export interface CreateInvoiceRequest {
    workspaceId: number;
    editInvoiceDto: EditInvoiceDto;
}

export interface GetInvoiceRequest {
    workspaceId: number;
    invoiceId: number;
}

export interface GetInvoicesRequest {
    workspaceId: number;
    sortBy?: GetInvoicesSortByEnum;
    freeSearchTextEq?: string;
    statusIn?: Array<GetInvoicesStatusInEnum>;
    pageNumber?: number;
    pageSize?: number;
    sortOrder?: GetInvoicesSortOrderEnum;
}

export interface UpdateInvoiceRequest {
    workspaceId: number;
    invoiceId: number;
    editInvoiceDto: EditInvoiceDto;
}

/**
 * 
 */
export class InvoicesApiControllerApi extends runtime.BaseAPI {

    /**
     */
    async cancelInvoiceRaw(requestParameters: CancelInvoiceRequest, initOverrides?: RequestInit | runtime.InitOverideFunction): Promise<runtime.ApiResponse<InvoiceDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling cancelInvoice.');
        }

        if (requestParameters.invoiceId === null || requestParameters.invoiceId === undefined) {
            throw new runtime.RequiredError('invoiceId','Required parameter requestParameters.invoiceId was null or undefined when calling cancelInvoice.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/invoices/{invoiceId}/cancel`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))).replace(`{${"invoiceId"}}`, encodeURIComponent(String(requestParameters.invoiceId))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => InvoiceDtoFromJSON(jsonValue));
    }

    /**
     */
    async cancelInvoice(requestParameters: CancelInvoiceRequest, initOverrides?: RequestInit | runtime.InitOverideFunction): Promise<InvoiceDto> {
        const response = await this.cancelInvoiceRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async createInvoiceRaw(requestParameters: CreateInvoiceRequest, initOverrides?: RequestInit | runtime.InitOverideFunction): Promise<runtime.ApiResponse<InvoiceDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling createInvoice.');
        }

        if (requestParameters.editInvoiceDto === null || requestParameters.editInvoiceDto === undefined) {
            throw new runtime.RequiredError('editInvoiceDto','Required parameter requestParameters.editInvoiceDto was null or undefined when calling createInvoice.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/invoices`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: EditInvoiceDtoToJSON(requestParameters.editInvoiceDto),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => InvoiceDtoFromJSON(jsonValue));
    }

    /**
     */
    async createInvoice(requestParameters: CreateInvoiceRequest, initOverrides?: RequestInit | runtime.InitOverideFunction): Promise<InvoiceDto> {
        const response = await this.createInvoiceRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async getInvoiceRaw(requestParameters: GetInvoiceRequest, initOverrides?: RequestInit | runtime.InitOverideFunction): Promise<runtime.ApiResponse<InvoiceDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getInvoice.');
        }

        if (requestParameters.invoiceId === null || requestParameters.invoiceId === undefined) {
            throw new runtime.RequiredError('invoiceId','Required parameter requestParameters.invoiceId was null or undefined when calling getInvoice.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/invoices/{invoiceId}`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))).replace(`{${"invoiceId"}}`, encodeURIComponent(String(requestParameters.invoiceId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => InvoiceDtoFromJSON(jsonValue));
    }

    /**
     */
    async getInvoice(requestParameters: GetInvoiceRequest, initOverrides?: RequestInit | runtime.InitOverideFunction): Promise<InvoiceDto> {
        const response = await this.getInvoiceRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async getInvoicesRaw(requestParameters: GetInvoicesRequest, initOverrides?: RequestInit | runtime.InitOverideFunction): Promise<runtime.ApiResponse<ApiPageInvoiceDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getInvoices.');
        }

        const queryParameters: any = {};

        if (requestParameters.sortBy !== undefined) {
            queryParameters['sortBy'] = requestParameters.sortBy;
        }

        if (requestParameters.freeSearchTextEq !== undefined) {
            queryParameters['freeSearchText[eq]'] = requestParameters.freeSearchTextEq;
        }

        if (requestParameters.statusIn) {
            queryParameters['status[in]'] = requestParameters.statusIn;
        }

        if (requestParameters.pageNumber !== undefined) {
            queryParameters['pageNumber'] = requestParameters.pageNumber;
        }

        if (requestParameters.pageSize !== undefined) {
            queryParameters['pageSize'] = requestParameters.pageSize;
        }

        if (requestParameters.sortOrder !== undefined) {
            queryParameters['sortOrder'] = requestParameters.sortOrder;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/invoices`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ApiPageInvoiceDtoFromJSON(jsonValue));
    }

    /**
     */
    async getInvoices(requestParameters: GetInvoicesRequest, initOverrides?: RequestInit | runtime.InitOverideFunction): Promise<ApiPageInvoiceDto> {
        const response = await this.getInvoicesRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async updateInvoiceRaw(requestParameters: UpdateInvoiceRequest, initOverrides?: RequestInit | runtime.InitOverideFunction): Promise<runtime.ApiResponse<InvoiceDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling updateInvoice.');
        }

        if (requestParameters.invoiceId === null || requestParameters.invoiceId === undefined) {
            throw new runtime.RequiredError('invoiceId','Required parameter requestParameters.invoiceId was null or undefined when calling updateInvoice.');
        }

        if (requestParameters.editInvoiceDto === null || requestParameters.editInvoiceDto === undefined) {
            throw new runtime.RequiredError('editInvoiceDto','Required parameter requestParameters.editInvoiceDto was null or undefined when calling updateInvoice.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/invoices/{invoiceId}`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))).replace(`{${"invoiceId"}}`, encodeURIComponent(String(requestParameters.invoiceId))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: EditInvoiceDtoToJSON(requestParameters.editInvoiceDto),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => InvoiceDtoFromJSON(jsonValue));
    }

    /**
     */
    async updateInvoice(requestParameters: UpdateInvoiceRequest, initOverrides?: RequestInit | runtime.InitOverideFunction): Promise<InvoiceDto> {
        const response = await this.updateInvoiceRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

/**
 * @export
 */
export const GetInvoicesSortByEnum = {
    NotSupported: '_NOT_SUPPORTED'
} as const;
export type GetInvoicesSortByEnum = typeof GetInvoicesSortByEnum[keyof typeof GetInvoicesSortByEnum];
/**
 * @export
 */
export const GetInvoicesStatusInEnum = {
    Draft: 'DRAFT',
    Sent: 'SENT',
    Overdue: 'OVERDUE',
    Paid: 'PAID',
    Cancelled: 'CANCELLED'
} as const;
export type GetInvoicesStatusInEnum = typeof GetInvoicesStatusInEnum[keyof typeof GetInvoicesStatusInEnum];
/**
 * @export
 */
export const GetInvoicesSortOrderEnum = {
    Asc: 'asc',
    Desc: 'desc'
} as const;
export type GetInvoicesSortOrderEnum = typeof GetInvoicesSortOrderEnum[keyof typeof GetInvoicesSortOrderEnum];
