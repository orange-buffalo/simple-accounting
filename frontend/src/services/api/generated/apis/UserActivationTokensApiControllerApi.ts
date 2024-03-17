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
  CreateUserActivationTokenRequestDto,
  UserActivationRequestDto,
  UserActivationTokenDto,
  UserActivationTokensApiBadRequestErrors,
} from '../models';
import {
    CreateUserActivationTokenRequestDtoFromJSON,
    CreateUserActivationTokenRequestDtoToJSON,
    UserActivationRequestDtoFromJSON,
    UserActivationRequestDtoToJSON,
    UserActivationTokenDtoFromJSON,
    UserActivationTokenDtoToJSON,
    UserActivationTokensApiBadRequestErrorsFromJSON,
    UserActivationTokensApiBadRequestErrorsToJSON,
} from '../models';
import type { AdditionalRequestParameters, InitOverrideFunction } from '../runtime';

export interface ActivateUserRequest {
    token: string;
    userActivationRequestDto: UserActivationRequestDto;
}

export interface CreateTokenRequest {
    createUserActivationTokenRequestDto: CreateUserActivationTokenRequestDto;
}

export interface GetTokenRequest {
    token: string;
}

export interface GetTokenByUserRequest {
    userId: number;
}

/**
 * 
 */
export class UserActivationTokensApiControllerApi<RM = void> extends runtime.BaseAPI<RM> {

    /**
     */
    async activateUserRaw(requestParameters: ActivateUserRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.token === null || requestParameters.token === undefined) {
            throw new runtime.RequiredError('token','Required parameter requestParameters.token was null or undefined when calling activateUser.');
        }

        if (requestParameters.userActivationRequestDto === null || requestParameters.userActivationRequestDto === undefined) {
            throw new runtime.RequiredError('userActivationRequestDto','Required parameter requestParameters.userActivationRequestDto was null or undefined when calling activateUser.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/user-activation-tokens/{token}/activate`.replace(`{${"token"}}`, encodeURIComponent(String(requestParameters.token))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: UserActivationRequestDtoToJSON(requestParameters.userActivationRequestDto),
        }, initOverrides, additionalParameters);

        return new runtime.VoidApiResponse(response);
    }

    /**
     */
    async activateUser(requestParameters: ActivateUserRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<void> {
        await this.activateUserRaw(requestParameters, initOverrides, additionalParameters);
    }

    /**
     */
    async createTokenRaw(requestParameters: CreateTokenRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<UserActivationTokenDto>> {
        if (requestParameters.createUserActivationTokenRequestDto === null || requestParameters.createUserActivationTokenRequestDto === undefined) {
            throw new runtime.RequiredError('createUserActivationTokenRequestDto','Required parameter requestParameters.createUserActivationTokenRequestDto was null or undefined when calling createToken.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/user-activation-tokens`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: CreateUserActivationTokenRequestDtoToJSON(requestParameters.createUserActivationTokenRequestDto),
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => UserActivationTokenDtoFromJSON(jsonValue));
    }

    /**
     */
    async createToken(requestParameters: CreateTokenRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<UserActivationTokenDto> {
        const response = await this.createTokenRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async getTokenRaw(requestParameters: GetTokenRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<UserActivationTokenDto>> {
        if (requestParameters.token === null || requestParameters.token === undefined) {
            throw new runtime.RequiredError('token','Required parameter requestParameters.token was null or undefined when calling getToken.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/user-activation-tokens/{token}`.replace(`{${"token"}}`, encodeURIComponent(String(requestParameters.token))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => UserActivationTokenDtoFromJSON(jsonValue));
    }

    /**
     */
    async getToken(requestParameters: GetTokenRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<UserActivationTokenDto> {
        const response = await this.getTokenRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

    /**
     */
    async getTokenByUserRaw(requestParameters: GetTokenByUserRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<UserActivationTokenDto>> {
        if (requestParameters.userId === null || requestParameters.userId === undefined) {
            throw new runtime.RequiredError('userId','Required parameter requestParameters.userId was null or undefined when calling getTokenByUser.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/user-activation-tokens/{userId}`.replace(`{${"userId"}}`, encodeURIComponent(String(requestParameters.userId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => UserActivationTokenDtoFromJSON(jsonValue));
    }

    /**
     */
    async getTokenByUser(requestParameters: GetTokenByUserRequest, initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<UserActivationTokenDto> {
        const response = await this.getTokenByUserRaw(requestParameters, initOverrides, additionalParameters);
        return await response.value();
    }

}
