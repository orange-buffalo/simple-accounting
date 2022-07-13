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
  ApiPageIncomeDto,
  EditIncomeDto,
  ErrorResponse,
  IncomeDto,
} from '../models';
import {
    ApiPageIncomeDtoFromJSON,
    ApiPageIncomeDtoToJSON,
    EditIncomeDtoFromJSON,
    EditIncomeDtoToJSON,
    ErrorResponseFromJSON,
    ErrorResponseToJSON,
    IncomeDtoFromJSON,
    IncomeDtoToJSON,
} from '../models';
import type { AdditionalRequestParameters, InitOverrideFunction } from '../runtime';

export interface CreateIncomeRequest {
    workspaceId: number;
    editIncomeDto: EditIncomeDto;
}

export interface GetIncomeRequest {
    workspaceId: number;
    incomeId: number;
}

export interface GetIncomesRequest {
    workspaceId: number;
}

export interface UpdateIncomeRequest {
    workspaceId: number;
    incomeId: number;
    editIncomeDto: EditIncomeDto;
}

/**
 * 
 */
export class IncomesApiControllerApi<RM = void> extends runtime.BaseAPI<RM> {

    /**
     */
    async createIncomeRaw(requestParameters: CreateIncomeRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<IncomeDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling createIncome.');
        }

        if (requestParameters.editIncomeDto === null || requestParameters.editIncomeDto === undefined) {
            throw new runtime.RequiredError('editIncomeDto','Required parameter requestParameters.editIncomeDto was null or undefined when calling createIncome.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/incomes`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: EditIncomeDtoToJSON(requestParameters.editIncomeDto),
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => IncomeDtoFromJSON(jsonValue));
    }

    /**
     */
    async createIncome(requestParameters: CreateIncomeRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<IncomeDto> {
        const response = await this.createIncomeRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async getIncomeRaw(requestParameters: GetIncomeRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<IncomeDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getIncome.');
        }

        if (requestParameters.incomeId === null || requestParameters.incomeId === undefined) {
            throw new runtime.RequiredError('incomeId','Required parameter requestParameters.incomeId was null or undefined when calling getIncome.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/incomes/{incomeId}`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))).replace(`{${"incomeId"}}`, encodeURIComponent(String(requestParameters.incomeId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => IncomeDtoFromJSON(jsonValue));
    }

    /**
     */
    async getIncome(requestParameters: GetIncomeRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<IncomeDto> {
        const response = await this.getIncomeRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async getIncomesRaw(requestParameters: GetIncomesRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<ApiPageIncomeDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getIncomes.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/incomes`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => ApiPageIncomeDtoFromJSON(jsonValue));
    }

    /**
     */
    async getIncomes(requestParameters: GetIncomesRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<ApiPageIncomeDto> {
        const response = await this.getIncomesRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async updateIncomeRaw(requestParameters: UpdateIncomeRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<IncomeDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling updateIncome.');
        }

        if (requestParameters.incomeId === null || requestParameters.incomeId === undefined) {
            throw new runtime.RequiredError('incomeId','Required parameter requestParameters.incomeId was null or undefined when calling updateIncome.');
        }

        if (requestParameters.editIncomeDto === null || requestParameters.editIncomeDto === undefined) {
            throw new runtime.RequiredError('editIncomeDto','Required parameter requestParameters.editIncomeDto was null or undefined when calling updateIncome.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/incomes/{incomeId}`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))).replace(`{${"incomeId"}}`, encodeURIComponent(String(requestParameters.incomeId))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: EditIncomeDtoToJSON(requestParameters.editIncomeDto),
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => IncomeDtoFromJSON(jsonValue));
    }

    /**
     */
    async updateIncome(requestParameters: UpdateIncomeRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<IncomeDto> {
        const response = await this.updateIncomeRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

}
