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
import type { InvoiceDto } from './InvoiceDto';
import {
    InvoiceDtoFromJSON,
    InvoiceDtoFromJSONTyped,
    InvoiceDtoToJSON,
} from './InvoiceDto';

/**
 * 
 * @export
 * @interface ApiPageInvoiceDto
 */
export interface ApiPageInvoiceDto {
    /**
     * 
     * @type {number}
     * @memberof ApiPageInvoiceDto
     */
    pageNumber: number;
    /**
     * 
     * @type {number}
     * @memberof ApiPageInvoiceDto
     */
    pageSize: number;
    /**
     * 
     * @type {number}
     * @memberof ApiPageInvoiceDto
     */
    totalElements: number;
    /**
     * 
     * @type {Array<InvoiceDto>}
     * @memberof ApiPageInvoiceDto
     */
    data: Array<InvoiceDto>;
}

/**
 * Check if a given object implements the ApiPageInvoiceDto interface.
 */
export function instanceOfApiPageInvoiceDto(value: object): value is ApiPageInvoiceDto {
    if (!('pageNumber' in value) || value['pageNumber'] === undefined) return false;
    if (!('pageSize' in value) || value['pageSize'] === undefined) return false;
    if (!('totalElements' in value) || value['totalElements'] === undefined) return false;
    if (!('data' in value) || value['data'] === undefined) return false;
    return true;
}

export function ApiPageInvoiceDtoFromJSON(json: any): ApiPageInvoiceDto {
    return ApiPageInvoiceDtoFromJSONTyped(json, false);
}

export function ApiPageInvoiceDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): ApiPageInvoiceDto {
    if (json == null) {
        return json;
    }
    return {
        
        'pageNumber': json['pageNumber'],
        'pageSize': json['pageSize'],
        'totalElements': json['totalElements'],
        'data': ((json['data'] as Array<any>).map(InvoiceDtoFromJSON)),
    };
}

export function ApiPageInvoiceDtoToJSON(value?: ApiPageInvoiceDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'pageNumber': value['pageNumber'],
        'pageSize': value['pageSize'],
        'totalElements': value['totalElements'],
        'data': ((value['data'] as Array<any>).map(InvoiceDtoToJSON)),
    };
}

