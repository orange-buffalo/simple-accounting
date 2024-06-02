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
/**
 * 
 * @export
 * @interface FieldErrorDto
 */
export interface FieldErrorDto {
    /**
     * 
     * @type {string}
     * @memberof FieldErrorDto
     */
    field: string;
    /**
     * 
     * @type {string}
     * @memberof FieldErrorDto
     */
    error: string;
    /**
     * 
     * @type {string}
     * @memberof FieldErrorDto
     */
    message: string;
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof FieldErrorDto
     */
    params?: { [key: string]: string; };
}

/**
 * Check if a given object implements the FieldErrorDto interface.
 */
export function instanceOfFieldErrorDto(value: object): value is FieldErrorDto {
    if (!('field' in value) || value['field'] === undefined) return false;
    if (!('error' in value) || value['error'] === undefined) return false;
    if (!('message' in value) || value['message'] === undefined) return false;
    return true;
}

export function FieldErrorDtoFromJSON(json: any): FieldErrorDto {
    return FieldErrorDtoFromJSONTyped(json, false);
}

export function FieldErrorDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): FieldErrorDto {
    if (json == null) {
        return json;
    }
    return {
        
        'field': json['field'],
        'error': json['error'],
        'message': json['message'],
        'params': json['params'] == null ? undefined : json['params'],
    };
}

export function FieldErrorDtoToJSON(value?: FieldErrorDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'field': value['field'],
        'error': value['error'],
        'message': value['message'],
        'params': value['params'],
    };
}

