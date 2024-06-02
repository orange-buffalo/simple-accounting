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

import { mapValues } from '../runtime';
import type { GeneralTaxDto } from './GeneralTaxDto';
import {
    GeneralTaxDtoFromJSON,
    GeneralTaxDtoFromJSONTyped,
    GeneralTaxDtoToJSON,
} from './GeneralTaxDto';

/**
 * 
 * @export
 * @interface ApiPageGeneralTaxDto
 */
export interface ApiPageGeneralTaxDto {
    /**
     * 
     * @type {number}
     * @memberof ApiPageGeneralTaxDto
     */
    pageNumber: number;
    /**
     * 
     * @type {number}
     * @memberof ApiPageGeneralTaxDto
     */
    pageSize: number;
    /**
     * 
     * @type {number}
     * @memberof ApiPageGeneralTaxDto
     */
    totalElements: number;
    /**
     * 
     * @type {Array<GeneralTaxDto>}
     * @memberof ApiPageGeneralTaxDto
     */
    data: Array<GeneralTaxDto>;
}

/**
 * Check if a given object implements the ApiPageGeneralTaxDto interface.
 */
export function instanceOfApiPageGeneralTaxDto(value: object): value is ApiPageGeneralTaxDto {
    if (!('pageNumber' in value) || value['pageNumber'] === undefined) return false;
    if (!('pageSize' in value) || value['pageSize'] === undefined) return false;
    if (!('totalElements' in value) || value['totalElements'] === undefined) return false;
    if (!('data' in value) || value['data'] === undefined) return false;
    return true;
}

export function ApiPageGeneralTaxDtoFromJSON(json: any): ApiPageGeneralTaxDto {
    return ApiPageGeneralTaxDtoFromJSONTyped(json, false);
}

export function ApiPageGeneralTaxDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): ApiPageGeneralTaxDto {
    if (json == null) {
        return json;
    }
    return {
        
        'pageNumber': json['pageNumber'],
        'pageSize': json['pageSize'],
        'totalElements': json['totalElements'],
        'data': ((json['data'] as Array<any>).map(GeneralTaxDtoFromJSON)),
    };
}

export function ApiPageGeneralTaxDtoToJSON(value?: ApiPageGeneralTaxDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'pageNumber': value['pageNumber'],
        'pageSize': value['pageSize'],
        'totalElements': value['totalElements'],
        'data': ((value['data'] as Array<any>).map(GeneralTaxDtoToJSON)),
    };
}

