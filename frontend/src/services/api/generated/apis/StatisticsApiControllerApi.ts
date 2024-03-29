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
  IncomeTaxPaymentsStatisticsDto,
  IncomesExpensesStatisticsDto,
} from '../models';
import {
    IncomeTaxPaymentsStatisticsDtoFromJSON,
    IncomeTaxPaymentsStatisticsDtoToJSON,
    IncomesExpensesStatisticsDtoFromJSON,
    IncomesExpensesStatisticsDtoToJSON,
} from '../models';
import type { AdditionalRequestParameters, InitOverrideFunction } from '../runtime';

export interface GetCurrenciesShortlistRequest {
    workspaceId: number;
}

export interface GetExpensesStatisticsRequest {
    workspaceId: number;
    fromDate: Date;
    toDate: Date;
}

export interface GetIncomesStatisticsRequest {
    workspaceId: number;
    fromDate: Date;
    toDate: Date;
}

export interface GetTaxPaymentsStatisticsRequest {
    workspaceId: number;
    fromDate: Date;
    toDate: Date;
}

/**
 * 
 */
export class StatisticsApiControllerApi<RM = void> extends runtime.BaseAPI<RM> {

    /**
     */
    async getCurrenciesShortlistRaw(requestParameters: GetCurrenciesShortlistRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<Array<string>>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getCurrenciesShortlist.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/statistics/currencies-shortlist`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse<any>(response);
    }

    /**
     */
    async getCurrenciesShortlist(requestParameters: GetCurrenciesShortlistRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<Array<string>> {
        const response = await this.getCurrenciesShortlistRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async getExpensesStatisticsRaw(requestParameters: GetExpensesStatisticsRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<IncomesExpensesStatisticsDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getExpensesStatistics.');
        }

        if (requestParameters.fromDate === null || requestParameters.fromDate === undefined) {
            throw new runtime.RequiredError('fromDate','Required parameter requestParameters.fromDate was null or undefined when calling getExpensesStatistics.');
        }

        if (requestParameters.toDate === null || requestParameters.toDate === undefined) {
            throw new runtime.RequiredError('toDate','Required parameter requestParameters.toDate was null or undefined when calling getExpensesStatistics.');
        }

        const queryParameters: any = {};

        if (requestParameters.fromDate !== undefined) {
            queryParameters['fromDate'] = (requestParameters.fromDate as any).toISOString().substr(0,10);
        }

        if (requestParameters.toDate !== undefined) {
            queryParameters['toDate'] = (requestParameters.toDate as any).toISOString().substr(0,10);
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/statistics/expenses`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => IncomesExpensesStatisticsDtoFromJSON(jsonValue));
    }

    /**
     */
    async getExpensesStatistics(requestParameters: GetExpensesStatisticsRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<IncomesExpensesStatisticsDto> {
        const response = await this.getExpensesStatisticsRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async getIncomesStatisticsRaw(requestParameters: GetIncomesStatisticsRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<IncomesExpensesStatisticsDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getIncomesStatistics.');
        }

        if (requestParameters.fromDate === null || requestParameters.fromDate === undefined) {
            throw new runtime.RequiredError('fromDate','Required parameter requestParameters.fromDate was null or undefined when calling getIncomesStatistics.');
        }

        if (requestParameters.toDate === null || requestParameters.toDate === undefined) {
            throw new runtime.RequiredError('toDate','Required parameter requestParameters.toDate was null or undefined when calling getIncomesStatistics.');
        }

        const queryParameters: any = {};

        if (requestParameters.fromDate !== undefined) {
            queryParameters['fromDate'] = (requestParameters.fromDate as any).toISOString().substr(0,10);
        }

        if (requestParameters.toDate !== undefined) {
            queryParameters['toDate'] = (requestParameters.toDate as any).toISOString().substr(0,10);
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/statistics/incomes`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => IncomesExpensesStatisticsDtoFromJSON(jsonValue));
    }

    /**
     */
    async getIncomesStatistics(requestParameters: GetIncomesStatisticsRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<IncomesExpensesStatisticsDto> {
        const response = await this.getIncomesStatisticsRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async getTaxPaymentsStatisticsRaw(requestParameters: GetTaxPaymentsStatisticsRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<IncomeTaxPaymentsStatisticsDto>> {
        if (requestParameters.workspaceId === null || requestParameters.workspaceId === undefined) {
            throw new runtime.RequiredError('workspaceId','Required parameter requestParameters.workspaceId was null or undefined when calling getTaxPaymentsStatistics.');
        }

        if (requestParameters.fromDate === null || requestParameters.fromDate === undefined) {
            throw new runtime.RequiredError('fromDate','Required parameter requestParameters.fromDate was null or undefined when calling getTaxPaymentsStatistics.');
        }

        if (requestParameters.toDate === null || requestParameters.toDate === undefined) {
            throw new runtime.RequiredError('toDate','Required parameter requestParameters.toDate was null or undefined when calling getTaxPaymentsStatistics.');
        }

        const queryParameters: any = {};

        if (requestParameters.fromDate !== undefined) {
            queryParameters['fromDate'] = (requestParameters.fromDate as any).toISOString().substr(0,10);
        }

        if (requestParameters.toDate !== undefined) {
            queryParameters['toDate'] = (requestParameters.toDate as any).toISOString().substr(0,10);
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/statistics/income-tax-payments`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters.workspaceId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => IncomeTaxPaymentsStatisticsDtoFromJSON(jsonValue));
    }

    /**
     */
    async getTaxPaymentsStatistics(requestParameters: GetTaxPaymentsStatisticsRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<IncomeTaxPaymentsStatisticsDto> {
        const response = await this.getTaxPaymentsStatisticsRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

}
