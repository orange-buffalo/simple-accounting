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
  ApiPageGeneralTaxDto,
  EditGeneralTaxDto,
  GeneralTaxDto,
} from '../models';
import {
    ApiPageGeneralTaxDtoFromJSON,
    ApiPageGeneralTaxDtoToJSON,
    EditGeneralTaxDtoFromJSON,
    EditGeneralTaxDtoToJSON,
    GeneralTaxDtoFromJSON,
    GeneralTaxDtoToJSON,
} from '../models';
import type { AdditionalRequestParameters, InitOverrideFunction } from '../runtime';

export interface CreateTaxRequest {
    workspaceId: number;
    editGeneralTaxDto: EditGeneralTaxDto;
}

export interface GetTaxRequest {
    workspaceId: number;
    taxId: number;
}

export interface GetTaxesRequest {
    workspaceId: number;
    sortBy?: GetTaxesSortByEnum;
    pageNumber?: number;
    pageSize?: number;
    sortOrder?: GetTaxesSortOrderEnum;
}

export interface UpdateTaxRequest {
    workspaceId: number;
    taxId: number;
    editGeneralTaxDto: EditGeneralTaxDto;
}

/**
 * 
 */
export class GeneralTaxApiControllerApi<RM = void> extends runtime.BaseAPI<RM> {

    /**
     */
    async createTaxRaw(requestParameters: CreateTaxRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<GeneralTaxDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling createTax.');
        }

        if (requestParameters.editGeneralTaxDto === null || requestParameters.editGeneralTaxDto === undefined) {
            throw new runtime.RequiredError('editGeneralTaxDto','Required parameter requestParameters.editGeneralTaxDto was null or undefined when calling createTax.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/general-taxes`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: EditGeneralTaxDtoToJSON(requestParameters.editGeneralTaxDto),
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => GeneralTaxDtoFromJSON(jsonValue));
    }

    /**
     */
    async createTax(requestParameters: CreateTaxRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<GeneralTaxDto> {
        const response = await this.createTaxRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async getTaxRaw(requestParameters: GetTaxRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<GeneralTaxDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getTax.');
        }

        if (requestParameters.taxId === null || requestParameters.taxId === undefined) {
            throw new runtime.RequiredError('taxId','Required parameter requestParameters.taxId was null or undefined when calling getTax.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/general-taxes/{taxId}`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))).replace(`{${"taxId"}}`, encodeURIComponent(String(requestParameters.taxId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => GeneralTaxDtoFromJSON(jsonValue));
    }

    /**
     */
    async getTax(requestParameters: GetTaxRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<GeneralTaxDto> {
        const response = await this.getTaxRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async getTaxesRaw(requestParameters: GetTaxesRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<ApiPageGeneralTaxDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getTaxes.');
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
            path: `/api/workspaces/{workspaceId}/general-taxes`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => ApiPageGeneralTaxDtoFromJSON(jsonValue));
    }

    /**
     */
    async getTaxes(requestParameters: GetTaxesRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<ApiPageGeneralTaxDto> {
        const response = await this.getTaxesRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async updateTaxRaw(requestParameters: UpdateTaxRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<GeneralTaxDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling updateTax.');
        }

        if (requestParameters.taxId === null || requestParameters.taxId === undefined) {
            throw new runtime.RequiredError('taxId','Required parameter requestParameters.taxId was null or undefined when calling updateTax.');
        }

        if (requestParameters.editGeneralTaxDto === null || requestParameters.editGeneralTaxDto === undefined) {
            throw new runtime.RequiredError('editGeneralTaxDto','Required parameter requestParameters.editGeneralTaxDto was null or undefined when calling updateTax.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/general-taxes/{taxId}`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))).replace(`{${"taxId"}}`, encodeURIComponent(String(requestParameters.taxId))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: EditGeneralTaxDtoToJSON(requestParameters.editGeneralTaxDto),
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => GeneralTaxDtoFromJSON(jsonValue));
    }

    /**
     */
    async updateTax(requestParameters: UpdateTaxRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<GeneralTaxDto> {
        const response = await this.updateTaxRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

}

/**
 * @export
 */
export const GetTaxesSortByEnum = {
    NotSupported: '_NOT_SUPPORTED'
} as const;
export type GetTaxesSortByEnum = typeof GetTaxesSortByEnum[keyof typeof GetTaxesSortByEnum];
/**
 * @export
 */
export const GetTaxesSortOrderEnum = {
    Asc: 'asc',
    Desc: 'desc'
} as const;
export type GetTaxesSortOrderEnum = typeof GetTaxesSortOrderEnum[keyof typeof GetTaxesSortOrderEnum];
