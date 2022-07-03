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
 * @interface GoogleDriveStorageIntegrationStatus
 */
export interface GoogleDriveStorageIntegrationStatus {
    /**
     * 
     * @type {string}
     * @memberof GoogleDriveStorageIntegrationStatus
     */
    folderId?: string;
    /**
     * 
     * @type {string}
     * @memberof GoogleDriveStorageIntegrationStatus
     */
    folderName?: string;
    /**
     * 
     * @type {string}
     * @memberof GoogleDriveStorageIntegrationStatus
     */
    authorizationUrl?: string;
    /**
     * 
     * @type {boolean}
     * @memberof GoogleDriveStorageIntegrationStatus
     */
    authorizationRequired: boolean;
}

/**
 * Check if a given object implements the GoogleDriveStorageIntegrationStatus interface.
 */
export function instanceOfGoogleDriveStorageIntegrationStatus(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "authorizationRequired" in value;

    return isInstance;
}

export function GoogleDriveStorageIntegrationStatusFromJSON(json: any): GoogleDriveStorageIntegrationStatus {
    return GoogleDriveStorageIntegrationStatusFromJSONTyped(json, false);
}

export function GoogleDriveStorageIntegrationStatusFromJSONTyped(json: any, ignoreDiscriminator: boolean): GoogleDriveStorageIntegrationStatus {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'folderId': !exists(json, 'folderId') ? undefined : json['folderId'],
        'folderName': !exists(json, 'folderName') ? undefined : json['folderName'],
        'authorizationUrl': !exists(json, 'authorizationUrl') ? undefined : json['authorizationUrl'],
        'authorizationRequired': json['authorizationRequired'],
    };
}

export function GoogleDriveStorageIntegrationStatusToJSON(value?: GoogleDriveStorageIntegrationStatus | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'folderId': value.folderId,
        'folderName': value.folderName,
        'authorizationUrl': value.authorizationUrl,
        'authorizationRequired': value.authorizationRequired,
    };
}

