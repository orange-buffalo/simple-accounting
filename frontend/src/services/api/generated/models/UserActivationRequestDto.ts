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
 * @interface UserActivationRequestDto
 */
export interface UserActivationRequestDto {
    /**
     * 
     * @type {string}
     * @memberof UserActivationRequestDto
     */
    password: string;
}

/**
 * Check if a given object implements the UserActivationRequestDto interface.
 */
export function instanceOfUserActivationRequestDto(value: object): value is UserActivationRequestDto {
    if (!('password' in value) || value['password'] === undefined) return false;
    return true;
}

export function UserActivationRequestDtoFromJSON(json: any): UserActivationRequestDto {
    return UserActivationRequestDtoFromJSONTyped(json, false);
}

export function UserActivationRequestDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): UserActivationRequestDto {
    if (json == null) {
        return json;
    }
    return {
        
        'password': json['password'],
    };
}

export function UserActivationRequestDtoToJSON(value?: UserActivationRequestDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'password': value['password'],
    };
}

