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
 * @interface UserActivationTokensApiBadRequestErrors
 */
export interface UserActivationTokensApiBadRequestErrors {
    /**
     * 
     * @type {string}
     * @memberof UserActivationTokensApiBadRequestErrors
     */
    error: UserActivationTokensApiBadRequestErrorsErrorEnum;
    /**
     * 
     * @type {string}
     * @memberof UserActivationTokensApiBadRequestErrors
     */
    message?: string;
}


/**
 * @export
 */
export const UserActivationTokensApiBadRequestErrorsErrorEnum = {
    UserAlreadyActivated: 'UserAlreadyActivated',
    TokenExpired: 'TokenExpired'
} as const;
export type UserActivationTokensApiBadRequestErrorsErrorEnum = typeof UserActivationTokensApiBadRequestErrorsErrorEnum[keyof typeof UserActivationTokensApiBadRequestErrorsErrorEnum];


/**
 * Check if a given object implements the UserActivationTokensApiBadRequestErrors interface.
 */
export function instanceOfUserActivationTokensApiBadRequestErrors(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "error" in value;

    return isInstance;
}

export function UserActivationTokensApiBadRequestErrorsFromJSON(json: any): UserActivationTokensApiBadRequestErrors {
    return UserActivationTokensApiBadRequestErrorsFromJSONTyped(json, false);
}

export function UserActivationTokensApiBadRequestErrorsFromJSONTyped(json: any, ignoreDiscriminator: boolean): UserActivationTokensApiBadRequestErrors {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': json['error'],
        'message': !exists(json, 'message') ? undefined : json['message'],
    };
}

export function UserActivationTokensApiBadRequestErrorsToJSON(value?: UserActivationTokensApiBadRequestErrors | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'error': value.error,
        'message': value.message,
    };
}
