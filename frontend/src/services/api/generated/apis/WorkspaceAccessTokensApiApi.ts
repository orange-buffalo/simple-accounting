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
  ApiPageWorkspaceAccessTokenDto,
  CreateWorkspaceAccessTokenDto,
  WorkspaceAccessTokenDto,
} from '../models/index';
import {
    ApiPageWorkspaceAccessTokenDtoFromJSON,
    ApiPageWorkspaceAccessTokenDtoToJSON,
    CreateWorkspaceAccessTokenDtoFromJSON,
    CreateWorkspaceAccessTokenDtoToJSON,
    WorkspaceAccessTokenDtoFromJSON,
    WorkspaceAccessTokenDtoToJSON,
} from '../models/index';

export interface CreateAccessTokenRequest {
    workspaceId: number;
    createWorkspaceAccessTokenDto: CreateWorkspaceAccessTokenDto;
}

export interface GetAccessTokensRequest {
    workspaceId: number;
}

/**
 * 
 */
export class WorkspaceAccessTokensApiApi extends runtime.BaseAPI {

    /**
     */
    async createAccessTokenRaw(requestParameters: CreateAccessTokenRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<WorkspaceAccessTokenDto>> {
        if (requestParameters['workspaceId'] == null) {
            throw new runtime.RequiredError(
                'workspaceId',
                'Required parameter "workspaceId" was null or undefined when calling createAccessToken().'
            );
        }

        if (requestParameters['createWorkspaceAccessTokenDto'] == null) {
            throw new runtime.RequiredError(
                'createWorkspaceAccessTokenDto',
                'Required parameter "createWorkspaceAccessTokenDto" was null or undefined when calling createAccessToken().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/workspace-access-tokens`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters['workspaceId']))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: CreateWorkspaceAccessTokenDtoToJSON(requestParameters['createWorkspaceAccessTokenDto']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => WorkspaceAccessTokenDtoFromJSON(jsonValue));
    }

    /**
     */
    async createAccessToken(requestParameters: CreateAccessTokenRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<WorkspaceAccessTokenDto> {
        const response = await this.createAccessTokenRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async getAccessTokensRaw(requestParameters: GetAccessTokensRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ApiPageWorkspaceAccessTokenDto>> {
        if (requestParameters['workspaceId'] == null) {
            throw new runtime.RequiredError(
                'workspaceId',
                'Required parameter "workspaceId" was null or undefined when calling getAccessTokens().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/workspaces/{workspaceId}/workspace-access-tokens`.replace(`{${"workspaceId"}}`, encodeURIComponent(String(requestParameters['workspaceId']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ApiPageWorkspaceAccessTokenDtoFromJSON(jsonValue));
    }

    /**
     */
    async getAccessTokens(requestParameters: GetAccessTokensRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ApiPageWorkspaceAccessTokenDto> {
        const response = await this.getAccessTokensRaw(requestParameters, initOverrides);
        return await response.value();
    }

}
