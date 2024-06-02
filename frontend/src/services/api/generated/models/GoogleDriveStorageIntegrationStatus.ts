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
export function instanceOfGoogleDriveStorageIntegrationStatus(value: object): value is GoogleDriveStorageIntegrationStatus {
    if (!('authorizationRequired' in value) || value['authorizationRequired'] === undefined) return false;
    return true;
}

export function GoogleDriveStorageIntegrationStatusFromJSON(json: any): GoogleDriveStorageIntegrationStatus {
    return GoogleDriveStorageIntegrationStatusFromJSONTyped(json, false);
}

export function GoogleDriveStorageIntegrationStatusFromJSONTyped(json: any, ignoreDiscriminator: boolean): GoogleDriveStorageIntegrationStatus {
    if (json == null) {
        return json;
    }
    return {
        
        'folderId': json['folderId'] == null ? undefined : json['folderId'],
        'folderName': json['folderName'] == null ? undefined : json['folderName'],
        'authorizationUrl': json['authorizationUrl'] == null ? undefined : json['authorizationUrl'],
        'authorizationRequired': json['authorizationRequired'],
    };
}

export function GoogleDriveStorageIntegrationStatusToJSON(value?: GoogleDriveStorageIntegrationStatus | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'folderId': value['folderId'],
        'folderName': value['folderName'],
        'authorizationUrl': value['authorizationUrl'],
        'authorizationRequired': value['authorizationRequired'],
    };
}

