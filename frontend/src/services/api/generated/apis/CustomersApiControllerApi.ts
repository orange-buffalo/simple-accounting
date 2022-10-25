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
  ApiPageCustomerDto,
  CustomerDto,
  EditCustomerDto,
  ErrorResponse,
} from '../models';
import {
    ApiPageCustomerDtoFromJSON,
    ApiPageCustomerDtoToJSON,
    CustomerDtoFromJSON,
    CustomerDtoToJSON,
    EditCustomerDtoFromJSON,
    EditCustomerDtoToJSON,
    ErrorResponseFromJSON,
    ErrorResponseToJSON,
} from '../models';
import type { AdditionalRequestParameters, InitOverrideFunction } from '../runtime';

export interface CreateCustomerRequest {
    workspaceId: number;
    editCustomerDto: EditCustomerDto;
}

export interface GetCustomerRequest {
    workspaceId: number;
    customerId: number;
}

export interface GetCustomersRequest {
    workspaceId: number;
    sortBy?: GetCustomersSortByEnum;
    pageNumber?: number;
    pageSize?: number;
    sortOrder?: GetCustomersSortOrderEnum;
}

export interface UpdateCustomerRequest {
    workspaceId: number;
    customerId: number;
    editCustomerDto: EditCustomerDto;
}

/**
 * 
 */
export class CustomersApiControllerApi<RM = void> extends runtime.BaseAPI<RM> {

    /**
     */
    async createCustomerRaw(requestParameters: CreateCustomerRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<CustomerDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling createCustomer.');
        }

        if (requestParameters.editCustomerDto === null || requestParameters.editCustomerDto === undefined) {
            throw new runtime.RequiredError('editCustomerDto','Required parameter requestParameters.editCustomerDto was null or undefined when calling createCustomer.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/customers`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: EditCustomerDtoToJSON(requestParameters.editCustomerDto),
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => CustomerDtoFromJSON(jsonValue));
    }

    /**
     */
    async createCustomer(requestParameters: CreateCustomerRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<CustomerDto> {
        const response = await this.createCustomerRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async getCustomerRaw(requestParameters: GetCustomerRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<CustomerDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getCustomer.');
        }

        if (requestParameters.customerId === null || requestParameters.customerId === undefined) {
            throw new runtime.RequiredError('customerId','Required parameter requestParameters.customerId was null or undefined when calling getCustomer.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/customers/{customerId}`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))).replace(`{${"customerId"}}`, encodeURIComponent(String(requestParameters.customerId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => CustomerDtoFromJSON(jsonValue));
    }

    /**
     */
    async getCustomer(requestParameters: GetCustomerRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<CustomerDto> {
        const response = await this.getCustomerRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async getCustomersRaw(requestParameters: GetCustomersRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<ApiPageCustomerDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getCustomers.');
        }

        const queryParameters: any = {};

        if (requestParameters.sortBy !== undefined) {
            queryParameters['sortBy'] = requestParameters.sortBy;
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
            path: `/api/workspaces/{workspaceId}/customers`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => ApiPageCustomerDtoFromJSON(jsonValue));
    }

    /**
     */
    async getCustomers(requestParameters: GetCustomersRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<ApiPageCustomerDto> {
        const response = await this.getCustomersRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async updateCustomerRaw(requestParameters: UpdateCustomerRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<CustomerDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling updateCustomer.');
        }

        if (requestParameters.customerId === null || requestParameters.customerId === undefined) {
            throw new runtime.RequiredError('customerId','Required parameter requestParameters.customerId was null or undefined when calling updateCustomer.');
        }

        if (requestParameters.editCustomerDto === null || requestParameters.editCustomerDto === undefined) {
            throw new runtime.RequiredError('editCustomerDto','Required parameter requestParameters.editCustomerDto was null or undefined when calling updateCustomer.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/customers/{customerId}`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))).replace(`{${"customerId"}}`, encodeURIComponent(String(requestParameters.customerId))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: EditCustomerDtoToJSON(requestParameters.editCustomerDto),
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => CustomerDtoFromJSON(jsonValue));
    }

    /**
     */
    async updateCustomer(requestParameters: UpdateCustomerRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<CustomerDto> {
        const response = await this.updateCustomerRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

}

/**
 * @export
 */
export const GetCustomersSortByEnum = {
    NotSupported: '_NOT_SUPPORTED'
} as const;
export type GetCustomersSortByEnum = typeof GetCustomersSortByEnum[keyof typeof GetCustomersSortByEnum];
/**
 * @export
 */
export const GetCustomersSortOrderEnum = {
    Asc: 'asc',
    Desc: 'desc'
} as const;
export type GetCustomersSortOrderEnum = typeof GetCustomersSortOrderEnum[keyof typeof GetCustomersSortOrderEnum];
