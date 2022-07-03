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
 * @interface SaveSharedWorkspaceRequestDto
 */
export interface SaveSharedWorkspaceRequestDto {
    /**
     * 
     * @type {string}
     * @memberof SaveSharedWorkspaceRequestDto
     */
    token: string;
}

/**
 * Check if a given object implements the SaveSharedWorkspaceRequestDto interface.
 */
export function instanceOfSaveSharedWorkspaceRequestDto(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "token" in value;

    return isInstance;
}

export function SaveSharedWorkspaceRequestDtoFromJSON(json: any): SaveSharedWorkspaceRequestDto {
    return SaveSharedWorkspaceRequestDtoFromJSONTyped(json, false);
}

export function SaveSharedWorkspaceRequestDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): SaveSharedWorkspaceRequestDto {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'token': json['token'],
    };
}

export function SaveSharedWorkspaceRequestDtoToJSON(value?: SaveSharedWorkspaceRequestDto | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'token': value.token,
    };
}

