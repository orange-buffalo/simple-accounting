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
 * @interface WorkspaceAccessTokenDto
 */
export interface WorkspaceAccessTokenDto {
    /**
     * 
     * @type {Date}
     * @memberof WorkspaceAccessTokenDto
     */
    validTill: Date;
    /**
     * 
     * @type {boolean}
     * @memberof WorkspaceAccessTokenDto
     */
    revoked: boolean;
    /**
     * 
     * @type {string}
     * @memberof WorkspaceAccessTokenDto
     */
    token: string;
    /**
     * 
     * @type {number}
     * @memberof WorkspaceAccessTokenDto
     */
    id: number;
    /**
     * 
     * @type {number}
     * @memberof WorkspaceAccessTokenDto
     */
    version: number;
}

/**
 * Check if a given object implements the WorkspaceAccessTokenDto interface.
 */
export function instanceOfWorkspaceAccessTokenDto(value: object): value is WorkspaceAccessTokenDto {
    if (!('validTill' in value) || value['validTill'] === undefined) return false;
    if (!('revoked' in value) || value['revoked'] === undefined) return false;
    if (!('token' in value) || value['token'] === undefined) return false;
    if (!('id' in value) || value['id'] === undefined) return false;
    if (!('version' in value) || value['version'] === undefined) return false;
    return true;
}

export function WorkspaceAccessTokenDtoFromJSON(json: any): WorkspaceAccessTokenDto {
    return WorkspaceAccessTokenDtoFromJSONTyped(json, false);
}

export function WorkspaceAccessTokenDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkspaceAccessTokenDto {
    if (json == null) {
        return json;
    }
    return {
        
        'validTill': (new Date(json['validTill'])),
        'revoked': json['revoked'],
        'token': json['token'],
        'id': json['id'],
        'version': json['version'],
    };
}

export function WorkspaceAccessTokenDtoToJSON(value?: WorkspaceAccessTokenDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'validTill': ((value['validTill']).toISOString()),
        'revoked': value['revoked'],
        'token': value['token'],
        'id': value['id'],
        'version': value['version'],
    };
}

