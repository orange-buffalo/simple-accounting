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
 * @interface CreateCategoryDto
 */
export interface CreateCategoryDto {
    /**
     * 
     * @type {string}
     * @memberof CreateCategoryDto
     */
    name: string;
    /**
     * 
     * @type {string}
     * @memberof CreateCategoryDto
     */
    description?: string;
    /**
     * 
     * @type {boolean}
     * @memberof CreateCategoryDto
     */
    income: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof CreateCategoryDto
     */
    expense: boolean;
}

/**
 * Check if a given object implements the CreateCategoryDto interface.
 */
export function instanceOfCreateCategoryDto(value: object): value is CreateCategoryDto {
    if (!('name' in value) || value['name'] === undefined) return false;
    if (!('income' in value) || value['income'] === undefined) return false;
    if (!('expense' in value) || value['expense'] === undefined) return false;
    return true;
}

export function CreateCategoryDtoFromJSON(json: any): CreateCategoryDto {
    return CreateCategoryDtoFromJSONTyped(json, false);
}

export function CreateCategoryDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): CreateCategoryDto {
    if (json == null) {
        return json;
    }
    return {
        
        'name': json['name'],
        'description': json['description'] == null ? undefined : json['description'],
        'income': json['income'],
        'expense': json['expense'],
    };
}

export function CreateCategoryDtoToJSON(value?: CreateCategoryDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'name': value['name'],
        'description': value['description'],
        'income': value['income'],
        'expense': value['expense'],
    };
}

