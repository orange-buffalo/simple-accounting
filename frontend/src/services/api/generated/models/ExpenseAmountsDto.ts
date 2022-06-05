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
 * @interface ExpenseAmountsDto
 */
export interface ExpenseAmountsDto {
    /**
     * 
     * @type {number}
     * @memberof ExpenseAmountsDto
     */
    originalAmountInDefaultCurrency?: number;
    /**
     * 
     * @type {number}
     * @memberof ExpenseAmountsDto
     */
    adjustedAmountInDefaultCurrency?: number;
}

export function ExpenseAmountsDtoFromJSON(json: any): ExpenseAmountsDto {
    return ExpenseAmountsDtoFromJSONTyped(json, false);
}

export function ExpenseAmountsDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): ExpenseAmountsDto {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'originalAmountInDefaultCurrency': !exists(json, 'originalAmountInDefaultCurrency') ? undefined : json['originalAmountInDefaultCurrency'],
        'adjustedAmountInDefaultCurrency': !exists(json, 'adjustedAmountInDefaultCurrency') ? undefined : json['adjustedAmountInDefaultCurrency'],
    };
}

export function ExpenseAmountsDtoToJSON(value?: ExpenseAmountsDto | null): any {
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

