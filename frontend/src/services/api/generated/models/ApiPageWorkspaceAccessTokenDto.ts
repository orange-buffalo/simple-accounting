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

import { exists, mapValues } from '../runtime';
import type { WorkspaceAccessTokenDto } from './WorkspaceAccessTokenDto';
import {
    WorkspaceAccessTokenDtoFromJSON,
    WorkspaceAccessTokenDtoFromJSONTyped,
    WorkspaceAccessTokenDtoToJSON,
} from './WorkspaceAccessTokenDto';

/**
 * 
 * @export
 * @interface ApiPageWorkspaceAccessTokenDto
 */
export interface ApiPageWorkspaceAccessTokenDto {
    /**
     * 
     * @type {number}
     * @memberof ApiPageWorkspaceAccessTokenDto
     */
    pageNumber: number;
    /**
     * 
     * @type {number}
     * @memberof ApiPageWorkspaceAccessTokenDto
     */
    pageSize: number;
    /**
     * 
     * @type {number}
     * @memberof ApiPageWorkspaceAccessTokenDto
     */
    totalElements: number;
    /**
     * 
     * @type {Array<WorkspaceAccessTokenDto>}
     * @memberof ApiPageWorkspaceAccessTokenDto
     */
    data: Array<WorkspaceAccessTokenDto>;
}

export function ApiPageWorkspaceAccessTokenDtoFromJSON(json: any): ApiPageWorkspaceAccessTokenDto {
    return ApiPageWorkspaceAccessTokenDtoFromJSONTyped(json, false);
}

export function ApiPageWorkspaceAccessTokenDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): ApiPageWorkspaceAccessTokenDto {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'pageNumber': json['pageNumber'],
        'pageSize': json['pageSize'],
        'totalElements': json['totalElements'],
        'data': ((json['data'] as Array<any>).map(WorkspaceAccessTokenDtoFromJSON)),
    };
}

export function ApiPageWorkspaceAccessTokenDtoToJSON(value?: ApiPageWorkspaceAccessTokenDto | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'pageNumber': value.pageNumber,
        'pageSize': value.pageSize,
        'totalElements': value.totalElements,
        'data': ((value.data as Array<any>).map(WorkspaceAccessTokenDtoToJSON)),
    };
}

