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
import type { ExpenseAmountsDto } from './ExpenseAmountsDto';
import {
    ExpenseAmountsDtoFromJSON,
    ExpenseAmountsDtoFromJSONTyped,
    ExpenseAmountsDtoToJSON,
} from './ExpenseAmountsDto';

/**
 * 
 * @export
 * @interface ExpenseDto
 */
export interface ExpenseDto {
    /**
     * 
     * @type {number}
     * @memberof ExpenseDto
     */
    category?: number;
    /**
     * 
     * @type {string}
     * @memberof ExpenseDto
     */
    title: string;
    /**
     * 
     * @type {Date}
     * @memberof ExpenseDto
     */
    timeRecorded: Date;
    /**
     * 
     * @type {Date}
     * @memberof ExpenseDto
     */
    datePaid: Date;
    /**
     * 
     * @type {string}
     * @memberof ExpenseDto
     */
    currency: string;
    /**
     * 
     * @type {number}
     * @memberof ExpenseDto
     */
    originalAmount: number;
    /**
     * 
     * @type {Array<number>}
     * @memberof ExpenseDto
     */
    attachments: Array<number>;
    /**
     * 
     * @type {number}
     * @memberof ExpenseDto
     */
    percentOnBusiness: number;
    /**
     * 
     * @type {string}
     * @memberof ExpenseDto
     */
    notes?: string;
    /**
     * 
     * @type {number}
     * @memberof ExpenseDto
     */
    id: number;
    /**
     * 
     * @type {number}
     * @memberof ExpenseDto
     */
    version: number;
    /**
     * 
     * @type {string}
     * @memberof ExpenseDto
     */
    status: ExpenseDtoStatusEnum;
    /**
     * 
     * @type {number}
     * @memberof ExpenseDto
     */
    generalTax?: number;
    /**
     * 
     * @type {number}
     * @memberof ExpenseDto
     */
    generalTaxRateInBps?: number;
    /**
     * 
     * @type {number}
     * @memberof ExpenseDto
     */
    generalTaxAmount?: number;
    /**
     * 
     * @type {ExpenseAmountsDto}
     * @memberof ExpenseDto
     */
    convertedAmounts: ExpenseAmountsDto;
    /**
     * 
     * @type {ExpenseAmountsDto}
     * @memberof ExpenseDto
     */
    incomeTaxableAmounts: ExpenseAmountsDto;
    /**
     * 
     * @type {boolean}
     * @memberof ExpenseDto
     */
    useDifferentExchangeRateForIncomeTaxPurposes: boolean;
}


/**
 * @export
 */
export const ExpenseDtoStatusEnum = {
    Finalized: 'FINALIZED',
    PendingConversion: 'PENDING_CONVERSION',
    PendingConversionForTaxationPurposes: 'PENDING_CONVERSION_FOR_TAXATION_PURPOSES'
} as const;
export type ExpenseDtoStatusEnum = typeof ExpenseDtoStatusEnum[keyof typeof ExpenseDtoStatusEnum];


/**
 * Check if a given object implements the ExpenseDto interface.
 */
export function instanceOfExpenseDto(value: object): value is ExpenseDto {
    if (!('title' in value) || value['title'] === undefined) return false;
    if (!('timeRecorded' in value) || value['timeRecorded'] === undefined) return false;
    if (!('datePaid' in value) || value['datePaid'] === undefined) return false;
    if (!('currency' in value) || value['currency'] === undefined) return false;
    if (!('originalAmount' in value) || value['originalAmount'] === undefined) return false;
    if (!('attachments' in value) || value['attachments'] === undefined) return false;
    if (!('percentOnBusiness' in value) || value['percentOnBusiness'] === undefined) return false;
    if (!('id' in value) || value['id'] === undefined) return false;
    if (!('version' in value) || value['version'] === undefined) return false;
    if (!('status' in value) || value['status'] === undefined) return false;
    if (!('convertedAmounts' in value) || value['convertedAmounts'] === undefined) return false;
    if (!('incomeTaxableAmounts' in value) || value['incomeTaxableAmounts'] === undefined) return false;
    if (!('useDifferentExchangeRateForIncomeTaxPurposes' in value) || value['useDifferentExchangeRateForIncomeTaxPurposes'] === undefined) return false;
    return true;
}

export function ExpenseDtoFromJSON(json: any): ExpenseDto {
    return ExpenseDtoFromJSONTyped(json, false);
}

export function ExpenseDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): ExpenseDto {
    if (json == null) {
        return json;
    }
    return {
        
        'category': json['category'] == null ? undefined : json['category'],
        'title': json['title'],
        'timeRecorded': (new Date(json['timeRecorded'])),
        'datePaid': (new Date(json['datePaid'])),
        'currency': json['currency'],
        'originalAmount': json['originalAmount'],
        'attachments': json['attachments'],
        'percentOnBusiness': json['percentOnBusiness'],
        'notes': json['notes'] == null ? undefined : json['notes'],
        'id': json['id'],
        'version': json['version'],
        'status': json['status'],
        'generalTax': json['generalTax'] == null ? undefined : json['generalTax'],
        'generalTaxRateInBps': json['generalTaxRateInBps'] == null ? undefined : json['generalTaxRateInBps'],
        'generalTaxAmount': json['generalTaxAmount'] == null ? undefined : json['generalTaxAmount'],
        'convertedAmounts': ExpenseAmountsDtoFromJSON(json['convertedAmounts']),
        'incomeTaxableAmounts': ExpenseAmountsDtoFromJSON(json['incomeTaxableAmounts']),
        'useDifferentExchangeRateForIncomeTaxPurposes': json['useDifferentExchangeRateForIncomeTaxPurposes'],
    };
}

export function ExpenseDtoToJSON(value?: ExpenseDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'category': value['category'],
        'title': value['title'],
        'timeRecorded': ((value['timeRecorded']).toISOString()),
        'datePaid': ((value['datePaid']).toISOString().substring(0,10)),
        'currency': value['currency'],
        'originalAmount': value['originalAmount'],
        'attachments': value['attachments'],
        'percentOnBusiness': value['percentOnBusiness'],
        'notes': value['notes'],
        'id': value['id'],
        'version': value['version'],
        'status': value['status'],
        'generalTax': value['generalTax'],
        'generalTaxRateInBps': value['generalTaxRateInBps'],
        'generalTaxAmount': value['generalTaxAmount'],
        'convertedAmounts': ExpenseAmountsDtoToJSON(value['convertedAmounts']),
        'incomeTaxableAmounts': ExpenseAmountsDtoToJSON(value['incomeTaxableAmounts']),
        'useDifferentExchangeRateForIncomeTaxPurposes': value['useDifferentExchangeRateForIncomeTaxPurposes'],
    };
}

