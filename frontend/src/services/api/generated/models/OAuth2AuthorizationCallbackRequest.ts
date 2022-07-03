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
 * @interface OAuth2AuthorizationCallbackRequest
 */
export interface OAuth2AuthorizationCallbackRequest {
    /**
     * 
     * @type {string}
     * @memberof OAuth2AuthorizationCallbackRequest
     */
    code?: string;
    /**
     * 
     * @type {string}
     * @memberof OAuth2AuthorizationCallbackRequest
     */
    error?: string;
    /**
     * 
     * @type {string}
     * @memberof OAuth2AuthorizationCallbackRequest
     */
    state: string;
}

/**
 * Check if a given object implements the OAuth2AuthorizationCallbackRequest interface.
 */
export function instanceOfOAuth2AuthorizationCallbackRequest(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "state" in value;

    return isInstance;
}

export function OAuth2AuthorizationCallbackRequestFromJSON(json: any): OAuth2AuthorizationCallbackRequest {
    return OAuth2AuthorizationCallbackRequestFromJSONTyped(json, false);
}

export function OAuth2AuthorizationCallbackRequestFromJSONTyped(json: any, ignoreDiscriminator: boolean): OAuth2AuthorizationCallbackRequest {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'code': !exists(json, 'code') ? undefined : json['code'],
        'error': !exists(json, 'error') ? undefined : json['error'],
        'state': json['state'],
    };
}

export function OAuth2AuthorizationCallbackRequestToJSON(value?: OAuth2AuthorizationCallbackRequest | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'code': value.code,
        'error': value.error,
        'state': value.state,
    };
}

