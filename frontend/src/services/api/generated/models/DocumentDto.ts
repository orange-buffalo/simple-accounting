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
 * @interface DocumentDto
 */
export interface DocumentDto {
    /**
     * 
     * @type {number}
     * @memberof DocumentDto
     */
    id: number;
    /**
     * 
     * @type {number}
     * @memberof DocumentDto
     */
    version: number;
    /**
     * 
     * @type {string}
     * @memberof DocumentDto
     */
    name: string;
    /**
     * 
     * @type {Date}
     * @memberof DocumentDto
     */
    timeUploaded: Date;
    /**
     * 
     * @type {number}
     * @memberof DocumentDto
     */
    sizeInBytes?: number;
}

/**
 * Check if a given object implements the DocumentDto interface.
 */
export function instanceOfDocumentDto(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "id" in value;
    isInstance = isInstance && "version" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "timeUploaded" in value;

    return isInstance;
}

export function DocumentDtoFromJSON(json: any): DocumentDto {
    return DocumentDtoFromJSONTyped(json, false);
}

export function DocumentDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): DocumentDto {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'id': json['id'],
        'version': json['version'],
        'name': json['name'],
        'timeUploaded': (new Date(json['timeUploaded'])),
        'sizeInBytes': !exists(json, 'sizeInBytes') ? undefined : json['sizeInBytes'],
    };
}

export function DocumentDtoToJSON(value?: DocumentDto | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'id': value.id,
        'version': value.version,
        'name': value.name,
        'timeUploaded': (value.timeUploaded.toISOString()),
        'sizeInBytes': value.sizeInBytes,
    };
}

