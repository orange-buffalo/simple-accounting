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
  LoginRequest,
  TokenResponse,
} from '../models/index';
import {
    LoginRequestFromJSON,
    LoginRequestToJSON,
    TokenResponseFromJSON,
    TokenResponseToJSON,
} from '../models/index';

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
export class AuthenticationApiApi extends runtime.BaseAPI {

    /**
     */
    async loginRaw(requestParameters: LoginOperationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<TokenResponse>> {
        if (requestParameters['loginRequest'] == null) {
            throw new runtime.RequiredError(
                'loginRequest',
                'Required parameter "loginRequest" was null or undefined when calling login().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/auth/login`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: LoginRequestToJSON(requestParameters['loginRequest']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => TokenResponseFromJSON(jsonValue));
    }

    /**
     */
    async login(requestParameters: LoginOperationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<TokenResponse> {
        const response = await this.loginRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async loginBySharedWorkspaceTokenRaw(requestParameters: LoginBySharedWorkspaceTokenRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<TokenResponse>> {
        if (requestParameters['sharedWorkspaceToken'] == null) {
            throw new runtime.RequiredError(
                'sharedWorkspaceToken',
                'Required parameter "sharedWorkspaceToken" was null or undefined when calling loginBySharedWorkspaceToken().'
            );
        }

        const queryParameters: any = {};

        if (requestParameters['sharedWorkspaceToken'] != null) {
            queryParameters['sharedWorkspaceToken'] = requestParameters['sharedWorkspaceToken'];
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/auth/login-by-token`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => TokenResponseFromJSON(jsonValue));
    }

    /**
     */
    async loginBySharedWorkspaceToken(requestParameters: LoginBySharedWorkspaceTokenRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<TokenResponse> {
        const response = await this.loginBySharedWorkspaceTokenRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async logoutRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<string>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/auth/logout`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        if (this.isJsonMime(response.headers.get('content-type'))) {
            return new runtime.JSONApiResponse<string>(response);
        } else {
            return new runtime.TextApiResponse(response) as any;
        }
    }

    /**
     */
    async logout(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<string> {
        const response = await this.logoutRaw(initOverrides);
        return await response.value();
    }

    /**
     */
    async refreshTokenRaw(requestParameters: RefreshTokenRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<TokenResponse>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/auth/token`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => TokenResponseFromJSON(jsonValue));
    }

    /**
     */
    async refreshToken(requestParameters: RefreshTokenRequest = {}, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<TokenResponse> {
        const response = await this.refreshTokenRaw(requestParameters, initOverrides);
        return await response.value();
    }

}
