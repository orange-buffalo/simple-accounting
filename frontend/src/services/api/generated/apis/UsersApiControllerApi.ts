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
  ApiPagePlatformUserDto,
  CreateUserRequestDto,
  PlatformUserDto,
  UpdateUserRequestDto,
  UsersApiCreateUserErrors,
  UsersApiUpdateUserErrors,
} from '../models/index';
import {
    ApiPagePlatformUserDtoFromJSON,
    ApiPagePlatformUserDtoToJSON,
    CreateUserRequestDtoFromJSON,
    CreateUserRequestDtoToJSON,
    PlatformUserDtoFromJSON,
    PlatformUserDtoToJSON,
    UpdateUserRequestDtoFromJSON,
    UpdateUserRequestDtoToJSON,
    UsersApiCreateUserErrorsFromJSON,
    UsersApiCreateUserErrorsToJSON,
    UsersApiUpdateUserErrorsFromJSON,
    UsersApiUpdateUserErrorsToJSON,
} from '../models/index';

export interface CreateUserRequest {
    createUserRequestDto: CreateUserRequestDto;
}

export interface GetUserRequest {
    userId: number;
}

export interface GetUsersRequest {
    sortBy?: GetUsersSortByEnum;
    freeSearchTextEq?: string;
    pageNumber?: number;
    pageSize?: number;
    sortOrder?: GetUsersSortOrderEnum;
}

export interface UpdateUserRequest {
    userId: number;
    updateUserRequestDto: UpdateUserRequestDto;
}

/**
 * 
 */
export class UsersApiControllerApi extends runtime.BaseAPI {

    /**
     */
    async createUserRaw(requestParameters: CreateUserRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<PlatformUserDto>> {
        if (requestParameters['createUserRequestDto'] == null) {
            throw new runtime.RequiredError(
                'createUserRequestDto',
                'Required parameter "createUserRequestDto" was null or undefined when calling createUser().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/users`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: CreateUserRequestDtoToJSON(requestParameters['createUserRequestDto']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => PlatformUserDtoFromJSON(jsonValue));
    }

    /**
     */
    async createUser(requestParameters: CreateUserRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<PlatformUserDto> {
        const response = await this.createUserRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async getUserRaw(requestParameters: GetUserRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<PlatformUserDto>> {
        if (requestParameters['userId'] == null) {
            throw new runtime.RequiredError(
                'userId',
                'Required parameter "userId" was null or undefined when calling getUser().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/users/{userId}`.replace(`{${"userId"}}`, encodeURIComponent(String(requestParameters['userId']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => PlatformUserDtoFromJSON(jsonValue));
    }

    /**
     */
    async getUser(requestParameters: GetUserRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<PlatformUserDto> {
        const response = await this.getUserRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async getUsersRaw(requestParameters: GetUsersRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ApiPagePlatformUserDto>> {
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
            path: `/api/users`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ApiPagePlatformUserDtoFromJSON(jsonValue));
    }

    /**
     */
    async getUsers(requestParameters: GetUsersRequest = {}, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ApiPagePlatformUserDto> {
        const response = await this.getUsersRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     */
    async updateUserRaw(requestParameters: UpdateUserRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<PlatformUserDto>> {
        if (requestParameters['userId'] == null) {
            throw new runtime.RequiredError(
                'userId',
                'Required parameter "userId" was null or undefined when calling updateUser().'
            );
        }

        if (requestParameters['updateUserRequestDto'] == null) {
            throw new runtime.RequiredError(
                'updateUserRequestDto',
                'Required parameter "updateUserRequestDto" was null or undefined when calling updateUser().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/users/{userId}`.replace(`{${"userId"}}`, encodeURIComponent(String(requestParameters['userId']))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: UpdateUserRequestDtoToJSON(requestParameters['updateUserRequestDto']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => PlatformUserDtoFromJSON(jsonValue));
    }

    /**
     */
    async updateUser(requestParameters: UpdateUserRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<PlatformUserDto> {
        const response = await this.updateUserRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

/**
 * @export
 */
export const GetUsersSortByEnum = {
    NotSupported: '_NOT_SUPPORTED'
} as const;
export type GetUsersSortByEnum = typeof GetUsersSortByEnum[keyof typeof GetUsersSortByEnum];
/**
 * @export
 */
export const GetUsersSortOrderEnum = {
    Asc: 'asc',
    Desc: 'desc'
} as const;
export type GetUsersSortOrderEnum = typeof GetUsersSortOrderEnum[keyof typeof GetUsersSortOrderEnum];
