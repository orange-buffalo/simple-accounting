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
 * @interface UserActivationTokenDto
 */
export interface UserActivationTokenDto {
    /**
     * 
     * @type {string}
     * @memberof UserActivationTokenDto
     */
    token: string;
    /**
     * 
     * @type {Date}
     * @memberof UserActivationTokenDto
     */
    expiresAt: Date;
}

/**
 * Check if a given object implements the UserActivationTokenDto interface.
 */
export function instanceOfUserActivationTokenDto(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "token" in value;
    isInstance = isInstance && "expiresAt" in value;

    return isInstance;
}

export function UserActivationTokenDtoFromJSON(json: any): UserActivationTokenDto {
    return UserActivationTokenDtoFromJSONTyped(json, false);
}

export function UserActivationTokenDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): UserActivationTokenDto {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'token': json['token'],
        'expiresAt': (new Date(json['expiresAt'])),
    };
}

export function UserActivationTokenDtoToJSON(value?: UserActivationTokenDto | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'token': value.token,
        'expiresAt': (value.expiresAt.toISOString()),
    };
}
