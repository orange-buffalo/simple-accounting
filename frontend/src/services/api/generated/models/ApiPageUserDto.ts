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
import type { UserDto } from './UserDto';
import {
    UserDtoFromJSON,
    UserDtoFromJSONTyped,
    UserDtoToJSON,
} from './UserDto';

/**
 * 
 * @export
 * @interface ApiPageUserDto
 */
export interface ApiPageUserDto {
    /**
     * 
     * @type {number}
     * @memberof ApiPageUserDto
     */
    pageNumber: number;
    /**
     * 
     * @type {number}
     * @memberof ApiPageUserDto
     */
    pageSize: number;
    /**
     * 
     * @type {number}
     * @memberof ApiPageUserDto
     */
    totalElements: number;
    /**
     * 
     * @type {Array<UserDto>}
     * @memberof ApiPageUserDto
     */
    data: Array<UserDto>;
}

export function ApiPageUserDtoFromJSON(json: any): ApiPageUserDto {
    return ApiPageUserDtoFromJSONTyped(json, false);
}

export function ApiPageUserDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): ApiPageUserDto {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'pageNumber': json['pageNumber'],
        'pageSize': json['pageSize'],
        'totalElements': json['totalElements'],
        'data': ((json['data'] as Array<any>).map(UserDtoFromJSON)),
    };
}

export function ApiPageUserDtoToJSON(value?: ApiPageUserDto | null): any {
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
        'data': ((value.data as Array<any>).map(UserDtoToJSON)),
    };
}

