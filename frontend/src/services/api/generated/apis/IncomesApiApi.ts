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
  IncomeDto,
} from '../models/index';
import {
    ApiPageIncomeDtoFromJSON,
    ApiPageIncomeDtoToJSON,
    EditIncomeDtoFromJSON,
    EditIncomeDtoToJSON,
    IncomeDtoFromJSON,
    IncomeDtoToJSON,
} from '../models/index';

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
    sortBy?: GetIncomesSortByEnum;
    freeSearchTextEq?: string;
    pageNumber?: number;
    pageSize?: number;
    sortOrder?: GetIncomesSortOrderEnum;
}

export interface UpdateIncomeRequest {
    workspaceId: number;
    incomeId: number;
    editIncomeDto: EditIncomeDto;
}

/**
 * 
 */
export class IncomesApiApi extends runtime.BaseAPI {

    /**
     */
    async createIncomeRaw(requestParameters: CreateIncomeRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<IncomeDto>> {
        if (requestParameters['workspaceId'] == null) {
            throw new runtime.RequiredError(
                'workspaceId',
                'Required parameter "workspaceId" was null or undefined when calling createIncome().'
            );
        }

        if (requestParameters['editIncomeDto'] == null) {
            throw new runtime.RequiredError(
                'editIncomeDto',
                'Required parameter "editIncomeDto" was null or undefined when calling createIncome().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/incomes`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters['workspaceId']))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: EditIncomeDtoToJSON(requestParameters['editIncomeDto']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => IncomeDtoFromJSON(jsonValue));
    }

    /**
     */
    async createIncome(requestParameters: CreateIncomeRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<IncomeDto> {
        const response = await this.createIncomeRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async getIncomeRaw(requestParameters: GetIncomeRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<IncomeDto>> {
        if (requestParameters['workspaceId'] == null) {
            throw new runtime.RequiredError(
                'workspaceId',
                'Required parameter "workspaceId" was null or undefined when calling getIncome().'
            );
        }

        if (requestParameters['incomeId'] == null) {
            throw new runtime.RequiredError(
                'incomeId',
                'Required parameter "incomeId" was null or undefined when calling getIncome().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/incomes/{incomeId}`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters['workspaceId']))).replace(`{${"incomeId"}}`, encodeURIComponent(String(requestParameters['incomeId']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => IncomeDtoFromJSON(jsonValue));
    }

    /**
     */
    async getIncome(requestParameters: GetIncomeRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<IncomeDto> {
        const response = await this.getIncomeRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async getIncomesRaw(requestParameters: GetIncomesRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ApiPageIncomeDto>> {
        if (requestParameters['workspaceId'] == null) {
            throw new runtime.RequiredError(
                'workspaceId',
                'Required parameter "workspaceId" was null or undefined when calling getIncomes().'
            );
        }

        const queryParameters: any = {};

        if (requestParameters['sortBy'] != null) {
            queryParameters['sortBy'] = requestParameters['sortBy'];
        }

        if (requestParameters['freeSearchTextEq'] != null) {
            queryParameters['freeSearchText[eq]'] = requestParameters['freeSearchTextEq'];
        }

        if (requestParameters['pageNumber'] != null) {
            queryParameters['pageNumber'] = requestParameters['pageNumber'];
        }

        if (requestParameters['pageSize'] != null) {
            queryParameters['pageSize'] = requestParameters['pageSize'];
        }

        if (requestParameters['sortOrder'] != null) {
            queryParameters['sortOrder'] = requestParameters['sortOrder'];
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/incomes`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters['workspaceId']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ApiPageIncomeDtoFromJSON(jsonValue));
    }

    /**
     */
    async getIncomes(requestParameters: GetIncomesRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ApiPageIncomeDto> {
        const response = await this.getIncomesRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async updateIncomeRaw(requestParameters: UpdateIncomeRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<IncomeDto>> {
        if (requestParameters['workspaceId'] == null) {
            throw new runtime.RequiredError(
                'workspaceId',
                'Required parameter "workspaceId" was null or undefined when calling updateIncome().'
            );
        }

        if (requestParameters['incomeId'] == null) {
            throw new runtime.RequiredError(
                'incomeId',
                'Required parameter "incomeId" was null or undefined when calling updateIncome().'
            );
        }

        if (requestParameters['editIncomeDto'] == null) {
            throw new runtime.RequiredError(
                'editIncomeDto',
                'Required parameter "editIncomeDto" was null or undefined when calling updateIncome().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/incomes/{incomeId}`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters['workspaceId']))).replace(`{${"incomeId"}}`, encodeURIComponent(String(requestParameters['incomeId']))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: EditIncomeDtoToJSON(requestParameters['editIncomeDto']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => IncomeDtoFromJSON(jsonValue));
    }

    /**
     */
    async updateIncome(requestParameters: UpdateIncomeRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<IncomeDto> {
        const response = await this.updateIncomeRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

/**
 * @export
 */
export const GetIncomesSortByEnum = {
    NotSupported: '_NOT_SUPPORTED'
} as const;
export type GetIncomesSortByEnum = typeof GetIncomesSortByEnum[keyof typeof GetIncomesSortByEnum];
/**
 * @export
 */
export const GetIncomesSortOrderEnum = {
    Asc: 'asc',
    Desc: 'desc'
} as const;
export type GetIncomesSortOrderEnum = typeof GetIncomesSortOrderEnum[keyof typeof GetIncomesSortOrderEnum];