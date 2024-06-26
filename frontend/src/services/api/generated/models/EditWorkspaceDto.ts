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

/**
 * Check if a given object implements the EditWorkspaceDto interface.
 */
export function instanceOfEditWorkspaceDto(value: object): value is EditWorkspaceDto {
    if (!('name' in value) || value['name'] === undefined) return false;
    return true;
}

export function EditWorkspaceDtoFromJSON(json: any): EditWorkspaceDto {
    return EditWorkspaceDtoFromJSONTyped(json, false);
}

export function EditWorkspaceDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): EditWorkspaceDto {
    if (json == null) {
        return json;
    }
    return {
        
        'name': json['name'],
    };
}

export function EditWorkspaceDtoToJSON(value?: EditWorkspaceDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'name': value['name'],
    };
}

