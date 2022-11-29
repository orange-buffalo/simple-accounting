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


import * as runtime from '../runtime';
import type {
  CurrentUserPushNotificationMessage,
} from '../models';
import {
    CurrentUserPushNotificationMessageFromJSON,
    CurrentUserPushNotificationMessageToJSON,
} from '../models';
import type { AdditionalRequestParameters, InitOverrideFunction } from '../runtime';

/**
 * 
 */
export class PushNotificationsApiControllerApi<RM = void> extends runtime.BaseAPI<RM> {

    /**
     */
    async getPushNotificationMessagesRaw(initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<runtime.ApiResponse<Array<CurrentUserPushNotificationMessage>>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/api/push-notifications`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides, additionalParameters);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(CurrentUserPushNotificationMessageFromJSON));
    }

    /**
     */
    async getPushNotificationMessages(initOverrides?: RequestInit | InitOverrideFunction, additionalParameters?: AdditionalRequestParameters<RM>): Promise<Array<CurrentUserPushNotificationMessage>> {
        const response = await this.getPushNotificationMessagesRaw(initOverrides, additionalParameters);
        return await response.value();
    }

}
