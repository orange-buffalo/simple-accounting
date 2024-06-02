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
import type { I18nSettingsDto } from './I18nSettingsDto';
import {
    I18nSettingsDtoFromJSON,
    I18nSettingsDtoFromJSONTyped,
    I18nSettingsDtoToJSON,
} from './I18nSettingsDto';

/**
 * 
 * @export
 * @interface UpdateProfileRequestDto
 */
export interface UpdateProfileRequestDto {
    /**
     * 
     * @type {string}
     * @memberof UpdateProfileRequestDto
     */
    documentsStorage?: string;
    /**
     * 
     * @type {I18nSettingsDto}
     * @memberof UpdateProfileRequestDto
     */
    i18n: I18nSettingsDto;
}

/**
 * Check if a given object implements the UpdateProfileRequestDto interface.
 */
export function instanceOfUpdateProfileRequestDto(value: object): value is UpdateProfileRequestDto {
    if (!('i18n' in value) || value['i18n'] === undefined) return false;
    return true;
}

export function UpdateProfileRequestDtoFromJSON(json: any): UpdateProfileRequestDto {
    return UpdateProfileRequestDtoFromJSONTyped(json, false);
}

export function UpdateProfileRequestDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): UpdateProfileRequestDto {
    if (json == null) {
        return json;
    }
    return {
        
        'documentsStorage': json['documentsStorage'] == null ? undefined : json['documentsStorage'],
        'i18n': I18nSettingsDtoFromJSON(json['i18n']),
    };
}

export function UpdateProfileRequestDtoToJSON(value?: UpdateProfileRequestDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'documentsStorage': value['documentsStorage'],
        'i18n': I18nSettingsDtoToJSON(value['i18n']),
    };
}

