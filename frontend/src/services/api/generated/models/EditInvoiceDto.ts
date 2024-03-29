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
export function instanceOfEditInvoiceDto(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "title" in value;
    isInstance = isInstance && "customer" in value;
    isInstance = isInstance && "dateIssued" in value;
    isInstance = isInstance && "dueDate" in value;
    isInstance = isInstance && "currency" in value;
    isInstance = isInstance && "amount" in value;

    return isInstance;
}

export function EditInvoiceDtoFromJSON(json: any): EditInvoiceDto {
    return EditInvoiceDtoFromJSONTyped(json, false);
}

export function EditInvoiceDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): EditInvoiceDto {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'title': json['title'],
        'customer': json['customer'],
        'dateIssued': (new Date(json['dateIssued'])),
        'dateSent': !exists(json, 'dateSent') ? undefined : (new Date(json['dateSent'])),
        'datePaid': !exists(json, 'datePaid') ? undefined : (new Date(json['datePaid'])),
        'dateCancelled': !exists(json, 'dateCancelled') ? undefined : (new Date(json['dateCancelled'])),
        'dueDate': (new Date(json['dueDate'])),
        'currency': json['currency'],
        'amount': json['amount'],
        'attachments': !exists(json, 'attachments') ? undefined : json['attachments'],
        'notes': !exists(json, 'notes') ? undefined : json['notes'],
        'generalTax': !exists(json, 'generalTax') ? undefined : json['generalTax'],
    };
}

export function EditInvoiceDtoToJSON(value?: EditInvoiceDto | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'title': value.title,
        'customer': value.customer,
        'dateIssued': (new Date(value.dateIssued.getTime() - value.dateIssued.getTimezoneOffset()*60*1000).toISOString().substr(0,10)),
        'dateSent': value.dateSent === undefined ? undefined : (new Date(value.dateSent.getTime() - value.dateSent.getTimezoneOffset()*60*1000).toISOString().substr(0,10)),
        'datePaid': value.datePaid === undefined ? undefined : (new Date(value.datePaid.getTime() - value.datePaid.getTimezoneOffset()*60*1000).toISOString().substr(0,10)),
        'dateCancelled': value.dateCancelled === undefined ? undefined : (new Date(value.dateCancelled.getTime() - value.dateCancelled.getTimezoneOffset()*60*1000).toISOString().substr(0,10)),
        'dueDate': (new Date(value.dueDate.getTime() - value.dueDate.getTimezoneOffset()*60*1000).toISOString().substr(0,10)),
        'currency': value.currency,
        'amount': value.amount,
        'attachments': value.attachments,
        'notes': value.notes,
        'generalTax': value.generalTax,
    };
}

