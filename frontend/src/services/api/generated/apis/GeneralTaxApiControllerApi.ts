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
  ErrorResponse,
  GeneralTaxDto,
} from '../models';
import {
    ApiPageGeneralTaxDtoFromJSON,
    ApiPageGeneralTaxDtoToJSON,
    EditGeneralTaxDtoFromJSON,
    EditGeneralTaxDtoToJSON,
    ErrorResponseFromJSON,
    ErrorResponseToJSON,
    GeneralTaxDtoFromJSON,
    GeneralTaxDtoToJSON,
} from '../models';

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
    async createTaxRaw<T extends RequestInit & RM>(requestParameters: CreateTaxRequest, initOverrides?: T | runtime.InitOverrideFunction<T, RM>): Promise<runtime.ApiResponse<GeneralTaxDto>> {
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
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => GeneralTaxDtoFromJSON(jsonValue));
    }

    /**
     */
    async createTax<T extends RequestInit & RM>(requestParameters: CreateTaxRequest, initOverrides?: T | runtime.InitOverrideFunction<T, RM>): Promise<GeneralTaxDto> {
        const response = await this.createTaxRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async getTaxRaw<T extends RequestInit & RM>(requestParameters: GetTaxRequest, initOverrides?: T | runtime.InitOverrideFunction<T, RM>): Promise<runtime.ApiResponse<GeneralTaxDto>> {
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
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => GeneralTaxDtoFromJSON(jsonValue));
    }

    /**
     */
    async getTax<T extends RequestInit & RM>(requestParameters: GetTaxRequest, initOverrides?: T | runtime.InitOverrideFunction<T, RM>): Promise<GeneralTaxDto> {
        const response = await this.getTaxRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async getTaxesRaw<T extends RequestInit & RM>(requestParameters: GetTaxesRequest, initOverrides?: T | runtime.InitOverrideFunction<T, RM>): Promise<runtime.ApiResponse<ApiPageGeneralTaxDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getTaxes.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/general-taxes`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ApiPageGeneralTaxDtoFromJSON(jsonValue));
    }

    /**
     */
    async getTaxes<T extends RequestInit & RM>(requestParameters: GetTaxesRequest, initOverrides?: T | runtime.InitOverrideFunction<T, RM>): Promise<ApiPageGeneralTaxDto> {
        const response = await this.getTaxesRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async updateTaxRaw<T extends RequestInit & RM>(requestParameters: UpdateTaxRequest, initOverrides?: T | runtime.InitOverrideFunction<T, RM>): Promise<runtime.ApiResponse<GeneralTaxDto>> {
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
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => GeneralTaxDtoFromJSON(jsonValue));
    }

    /**
     */
    async updateTax<T extends RequestInit & RM>(requestParameters: UpdateTaxRequest, initOverrides?: T | runtime.InitOverrideFunction<T, RM>): Promise<GeneralTaxDto> {
        const response = await this.updateTaxRaw(requestParameters, initOverrides);
        return await response.value();
    }

}
