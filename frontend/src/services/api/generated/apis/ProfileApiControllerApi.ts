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
  DocumentsStorageStatus,
  ErrorResponse,
  ProfileDto,
  UpdateProfileRequestDto,
} from '../models';
import {
    DocumentsStorageStatusFromJSON,
    DocumentsStorageStatusToJSON,
    ErrorResponseFromJSON,
    ErrorResponseToJSON,
    ProfileDtoFromJSON,
    ProfileDtoToJSON,
    UpdateProfileRequestDtoFromJSON,
    UpdateProfileRequestDtoToJSON,
} from '../models';

export interface UpdateProfileRequest {
    updateProfileRequestDto: UpdateProfileRequestDto;
}

/**
 * 
 */
export class ProfileApiControllerApi extends runtime.BaseAPI {

    /**
     */
    async getDocumentsStorageStatusRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<runtime.ApiResponse<DocumentsStorageStatus>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/profile/documents-storage`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, metadata);

        return new runtime.JSONApiResponse(response, (jsonValue) => DocumentsStorageStatusFromJSON(jsonValue));
    }

    /**
     */
    async getDocumentsStorageStatus(initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<DocumentsStorageStatus> {
        const response = await this.getDocumentsStorageStatusRaw(initOverrides, metadata);
        return await response.value();
    }

    /**
     */
    async getProfileRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<runtime.ApiResponse<ProfileDto>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/profile`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, metadata);

        return new runtime.JSONApiResponse(response, (jsonValue) => ProfileDtoFromJSON(jsonValue));
    }

    /**
     */
    async getProfile(initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<ProfileDto> {
        const response = await this.getProfileRaw(initOverrides, metadata);
        return await response.value();
    }

    /**
     */
    async updateProfileRaw(requestParameters: UpdateProfileRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<runtime.ApiResponse<ProfileDto>> {
        if (requestParameters.updateProfileRequestDto === null || requestParameters.updateProfileRequestDto === undefined) {
            throw new runtime.RequiredError('updateProfileRequestDto','Required parameter requestParameters.updateProfileRequestDto was null or undefined when calling updateProfile.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/api/profile`,
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: UpdateProfileRequestDtoToJSON(requestParameters.updateProfileRequestDto),
        }, initOverrides, metadata);

        return new runtime.JSONApiResponse(response, (jsonValue) => ProfileDtoFromJSON(jsonValue));
    }

    /**
     */
    async updateProfile(requestParameters: UpdateProfileRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction, metadata?: any): Promise<ProfileDto> {
        const response = await this.updateProfileRaw(requestParameters, initOverrides, metadata);
        return await response.value();
    }

}
