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
 * @interface EditWorkspaceDto
 */
export interface EditWorkspaceDto {
    /**
     * 
     * @type {string}
     * @memberof EditWorkspaceDto
     */
    name: string;
}

export function EditWorkspaceDtoFromJSON(json: any): EditWorkspaceDto {
    return EditWorkspaceDtoFromJSONTyped(json, false);
}

export function EditWorkspaceDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): EditWorkspaceDto {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'name': json['name'],
    };
}

export function EditWorkspaceDtoToJSON(value?: EditWorkspaceDto | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'name': value.name,
    };
}

