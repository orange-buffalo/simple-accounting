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
 * @interface ProfileDto
 */
export interface ProfileDto {
    /**
     * 
     * @type {string}
     * @memberof ProfileDto
     */
    userName: string;
    /**
     * 
     * @type {string}
     * @memberof ProfileDto
     */
    documentsStorage?: string;
    /**
     * 
     * @type {I18nSettingsDto}
     * @memberof ProfileDto
     */
    i18n: I18nSettingsDto;
}

/**
 * Check if a given object implements the ProfileDto interface.
 */
export function instanceOfProfileDto(value: object): value is ProfileDto {
    if (!('userName' in value) || value['userName'] === undefined) return false;
    if (!('i18n' in value) || value['i18n'] === undefined) return false;
    return true;
}

export function ProfileDtoFromJSON(json: any): ProfileDto {
    return ProfileDtoFromJSONTyped(json, false);
}

export function ProfileDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): ProfileDto {
    if (json == null) {
        return json;
    }
    return {
        
        'userName': json['userName'],
        'documentsStorage': json['documentsStorage'] == null ? undefined : json['documentsStorage'],
        'i18n': I18nSettingsDtoFromJSON(json['i18n']),
    };
}

export function ProfileDtoToJSON(value?: ProfileDto | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'userName': value['userName'],
        'documentsStorage': value['documentsStorage'],
        'i18n': I18nSettingsDtoToJSON(value['i18n']),
    };
}

