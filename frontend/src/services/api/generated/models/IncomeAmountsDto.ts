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
/**
 * 
 * @export
 * @interface IncomeAmountsDto
 */
export interface IncomeAmountsDto {
    /**
     * 
     * @type {number}
     * @memberof IncomeAmountsDto
     */
    originalAmountInDefaultCurrency?: number;
    /**
     * 
     * @type {number}
     * @memberof IncomeAmountsDto
     */
    adjustedAmountInDefaultCurrency?: number;
}

/**
 * Check if a given object implements the IncomeAmountsDto interface.
 */
export function instanceOfIncomeAmountsDto(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function IncomeAmountsDtoFromJSON(json: any): IncomeAmountsDto {
    return IncomeAmountsDtoFromJSONTyped(json, false);
}

export function IncomeAmountsDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): IncomeAmountsDto {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'originalAmountInDefaultCurrency': !exists(json, 'originalAmountInDefaultCurrency') ? undefined : json['originalAmountInDefaultCurrency'],
        'adjustedAmountInDefaultCurrency': !exists(json, 'adjustedAmountInDefaultCurrency') ? undefined : json['adjustedAmountInDefaultCurrency'],
    };
}

export function IncomeAmountsDtoToJSON(value?: IncomeAmountsDto | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'originalAmountInDefaultCurrency': value.originalAmountInDefaultCurrency,
        'adjustedAmountInDefaultCurrency': value.adjustedAmountInDefaultCurrency,
    };
}

