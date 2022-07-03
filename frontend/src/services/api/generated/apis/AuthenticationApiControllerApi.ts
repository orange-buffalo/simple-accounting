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
  ErrorResponse,
  LoginRequest,
  TokenResponse,
} from '../models';
import {
    ErrorResponseFromJSON,
    ErrorResponseToJSON,
    LoginRequestFromJSON,
    LoginRequestToJSON,
    TokenResponseFromJSON,
    TokenResponseToJSON,
} from '../models';

export interface LoginOperationRequest {
    loginRequest: LoginRequest;
}

export interface LoginBySharedWorkspaceTokenRequest {
    sharedWorkspaceToken: string;
}

export interface RefreshTokenRequest {
    refreshToken?: string;
}

/**
 * 
 */
export class AuthenticationApiControllerApi extends runtime.BaseAPI {

    /**
     */
    async loginRaw(requestParameters: LoginOperationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<runtime.ApiResponse<TokenResponse>> {
        if (requestParameters.loginRequest === null || requestParameters.loginRequest === undefined) {
            throw new runtime.RequiredError('loginRequest','Required parameter requestParameters.loginRequest was null or undefined when calling login.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/auth/login`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: LoginRequestToJSON(requestParameters.loginRequest),
        }, initOverrides, metadata);

        return new runtime.JSONApiResponse(response, (jsonValue) => TokenResponseFromJSON(jsonValue));
    }

    /**
     */
    async login(requestParameters: LoginOperationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<TokenResponse> {
        const response = await this.loginRaw(requestParameters, initOverrides, metadata);
        return await response.value();
    }

    /**
     */
    async loginBySharedWorkspaceTokenRaw(requestParameters: LoginBySharedWorkspaceTokenRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<runtime.ApiResponse<TokenResponse>> {
        if (requestParameters.sharedWorkspaceToken === null || requestParameters.sharedWorkspaceToken === undefined) {
            throw new runtime.RequiredError('sharedWorkspaceToken','Required parameter requestParameters.sharedWorkspaceToken was null or undefined when calling loginBySharedWorkspaceToken.');
        }

        const queryParameters: any = {};

        if (requestParameters.sharedWorkspaceToken !== undefined) {
            queryParameters['sharedWorkspaceToken'] = requestParameters.sharedWorkspaceToken;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/auth/login-by-token`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, metadata);

        return new runtime.JSONApiResponse(response, (jsonValue) => TokenResponseFromJSON(jsonValue));
    }

    /**
     */
    async loginBySharedWorkspaceToken(requestParameters: LoginBySharedWorkspaceTokenRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<TokenResponse> {
        const response = await this.loginBySharedWorkspaceTokenRaw(requestParameters, initOverrides, metadata);
        return await response.value();
    }

    /**
     */
    async logoutRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<runtime.ApiResponse<string>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/auth/logout`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, metadata);

        return new runtime.TextApiResponse(response) as any;
    }

    /**
     */
    async logout(initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<string> {
        const response = await this.logoutRaw(initOverrides, metadata);
        return await response.value();
    }

    /**
     */
    async refreshTokenRaw(requestParameters: RefreshTokenRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<runtime.ApiResponse<TokenResponse>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/auth/token`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, metadata);

        return new runtime.JSONApiResponse(response, (jsonValue) => TokenResponseFromJSON(jsonValue));
    }

    /**
     */
    async refreshToken(requestParameters: RefreshTokenRequest = {}, initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<TokenResponse> {
        const response = await this.refreshTokenRaw(requestParameters, initOverrides, metadata);
        return await response.value();
    }

}
