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
 * @interface EditInvoiceDto
 */
export interface EditInvoiceDto {
    /**
     * 
     * @type {string}
     * @memberof EditInvoiceDto
     */
    title: string;
    /**
     * 
     * @type {number}
     * @memberof EditInvoiceDto
     */
    customer: number;
    /**
     * 
     * @type {Date}
     * @memberof EditInvoiceDto
     */
    dateIssued: Date;
    /**
     * 
     * @type {Date}
     * @memberof EditInvoiceDto
     */
    dateSent?: Date;
    /**
     * 
     * @type {Date}
     * @memberof EditInvoiceDto
     */
    datePaid?: Date;
    /**
     * 
     * @type {Date}
     * @memberof EditInvoiceDto
     */
    dateCancelled?: Date;
    /**
     * 
     * @type {Date}
     * @memberof EditInvoiceDto
     */
    dueDate: Date;
    /**
     * 
     * @type {string}
     * @memberof EditInvoiceDto
     */
    currency: string;
    /**
     * 
     * @type {number}
     * @memberof EditInvoiceDto
     */
    amount: number;
    /**
     * 
     * @type {Array<number>}
     * @memberof EditInvoiceDto
     */
    attachments?: Array<number>;
    /**
     * 
     * @type {string}
     * @memberof EditInvoiceDto
     */
    notes?: string;
    /**
     * 
     * @type {number}
     * @memberof EditInvoiceDto
     */
    generalTax?: number;
}

/**
 * Check if a given object implements the EditInvoiceDto interface.
 */
export function instanceOfEditInvoiceDto(value: object): value is EditInvoiceDto {
    if (!('title' in value) || value['title'] === undefined) return false;
    if (!('customer' in value) || value['customer'] === undefined) return false;
    if (!('dateIssued' in value) || value['dateIssued'] === undefined) return false;
    if (!('dueDate' in value) || value['dueDate'] === undefined) return false;
    if (!('currency' in value) || value['currency'] === undefined) return false;
    if (!('amount' in value) || value['amount'] === undefined) return false;
    return true;
}

export function EditInvoiceDtoFromJSON(json: any): EditInvoiceDto {
    return EditInvoiceDtoFromJSONTyped(json, false);
}

export function EditInvoiceDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): EditInvoiceDto {
    if (json == null) {
        return json;
    }
    return {
        
        'title': json['title'],
        'customer': json['customer'],
        'dateIssued': (new Date(json['dateIssued'])),
        'dateSent': json['dateSent'] == null ? undefined : (new Date(json['dateSent'])),
        'datePaid': json['datePaid'] == null ? undefined : (new Date(json['datePaid'])),
        'dateCancelled': json['dateCancelled'] == null ? undefined : (new Date(json['dateCancelled'])),
        'dueDate': (new Date(json['dueDate'])),
        'currency': json['currency'],
        'amount': json['amount'],
        'attachments': json['attachments'] == null ? undefined : json['attachments'],
        'notes': json['notes'] == null ? undefined : json['notes'],
        'generalTax': json['generalTax'] == null ? undefined : json['generalTax'],
    };
}

export function EditInvoiceDtoToJSON(value?: EditInvoiceDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'title': value['title'],
        'customer': value['customer'],
        'dateIssued': ((value['dateIssued']).toISOString().substring(0,10)),
        'dateSent': value['dateSent'] == null ? undefined : ((value['dateSent']).toISOString().substring(0,10)),
        'datePaid': value['datePaid'] == null ? undefined : ((value['datePaid']).toISOString().substring(0,10)),
        'dateCancelled': value['dateCancelled'] == null ? undefined : ((value['dateCancelled']).toISOString().substring(0,10)),
        'dueDate': ((value['dueDate']).toISOString().substring(0,10)),
        'currency': value['currency'],
        'amount': value['amount'],
        'attachments': value['attachments'],
        'notes': value['notes'],
        'generalTax': value['generalTax'],
    };
}

