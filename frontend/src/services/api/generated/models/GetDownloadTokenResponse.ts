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
 * @interface GetDownloadTokenResponse
 */
export interface GetDownloadTokenResponse {
    /**
     * 
     * @type {string}
     * @memberof GetDownloadTokenResponse
     */
    token: string;
}

/**
 * Check if a given object implements the GetDownloadTokenResponse interface.
 */
export function instanceOfGetDownloadTokenResponse(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "token" in value;

    return isInstance;
}

export function GetDownloadTokenResponseFromJSON(json: any): GetDownloadTokenResponse {
    return GetDownloadTokenResponseFromJSONTyped(json, false);
}

export function GetDownloadTokenResponseFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetDownloadTokenResponse {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'token': json['token'],
    };
}

export function GetDownloadTokenResponseToJSON(value?: GetDownloadTokenResponse | null): any {
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

