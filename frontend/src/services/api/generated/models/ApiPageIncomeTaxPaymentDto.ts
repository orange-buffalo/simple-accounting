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
import type { IncomeTaxPaymentDto } from './IncomeTaxPaymentDto';
import {
    IncomeTaxPaymentDtoFromJSON,
    IncomeTaxPaymentDtoFromJSONTyped,
    IncomeTaxPaymentDtoToJSON,
} from './IncomeTaxPaymentDto';

/**
 * 
 * @export
 * @interface ApiPageIncomeTaxPaymentDto
 */
export interface ApiPageIncomeTaxPaymentDto {
    /**
     * 
     * @type {number}
     * @memberof ApiPageIncomeTaxPaymentDto
     */
    pageNumber: number;
    /**
     * 
     * @type {number}
     * @memberof ApiPageIncomeTaxPaymentDto
     */
    pageSize: number;
    /**
     * 
     * @type {number}
     * @memberof ApiPageIncomeTaxPaymentDto
     */
    totalElements: number;
    /**
     * 
     * @type {Array<IncomeTaxPaymentDto>}
     * @memberof ApiPageIncomeTaxPaymentDto
     */
    data: Array<IncomeTaxPaymentDto>;
}

/**
 * Check if a given object implements the ApiPageIncomeTaxPaymentDto interface.
 */
export function instanceOfApiPageIncomeTaxPaymentDto(value: object): value is ApiPageIncomeTaxPaymentDto {
    if (!('pageNumber' in value) || value['pageNumber'] === undefined) return false;
    if (!('pageSize' in value) || value['pageSize'] === undefined) return false;
    if (!('totalElements' in value) || value['totalElements'] === undefined) return false;
    if (!('data' in value) || value['data'] === undefined) return false;
    return true;
}

export function ApiPageIncomeTaxPaymentDtoFromJSON(json: any): ApiPageIncomeTaxPaymentDto {
    return ApiPageIncomeTaxPaymentDtoFromJSONTyped(json, false);
}

export function ApiPageIncomeTaxPaymentDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): ApiPageIncomeTaxPaymentDto {
    if (json == null) {
        return json;
    }
    return {
        
        'pageNumber': json['pageNumber'],
        'pageSize': json['pageSize'],
        'totalElements': json['totalElements'],
        'data': ((json['data'] as Array<any>).map(IncomeTaxPaymentDtoFromJSON)),
    };
}

export function ApiPageIncomeTaxPaymentDtoToJSON(value?: ApiPageIncomeTaxPaymentDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'pageNumber': value['pageNumber'],
        'pageSize': value['pageSize'],
        'totalElements': value['totalElements'],
        'data': ((value['data'] as Array<any>).map(IncomeTaxPaymentDtoToJSON)),
    };
}

