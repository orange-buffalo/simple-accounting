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
 * @interface CreateWorkspaceAccessTokenDto
 */
export interface CreateWorkspaceAccessTokenDto {
    /**
     * 
     * @type {Date}
     * @memberof CreateWorkspaceAccessTokenDto
     */
    validTill: Date;
}

/**
 * Check if a given object implements the CreateWorkspaceAccessTokenDto interface.
 */
export function instanceOfCreateWorkspaceAccessTokenDto(value: object): value is CreateWorkspaceAccessTokenDto {
    if (!('validTill' in value) || value['validTill'] === undefined) return false;
    return true;
}

export function CreateWorkspaceAccessTokenDtoFromJSON(json: any): CreateWorkspaceAccessTokenDto {
    return CreateWorkspaceAccessTokenDtoFromJSONTyped(json, false);
}

export function CreateWorkspaceAccessTokenDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): CreateWorkspaceAccessTokenDto {
    if (json == null) {
        return json;
    }
    return {
        
        'validTill': (new Date(json['validTill'])),
    };
}

export function CreateWorkspaceAccessTokenDtoToJSON(value?: CreateWorkspaceAccessTokenDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'validTill': ((value['validTill']).toISOString()),
    };
}

