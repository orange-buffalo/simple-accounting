import qs from 'qs';
import { OpenAPIClientAxios, Document } from 'openapi-client-axios';
import { Client } from '@/services/api/api-client-definition';
import { initAuth, applyAuthorization, handleErrorResponse } from '@/services/api/auth';
import {
  API_FATAL_ERROR_EVENT,
  LOADING_FINISHED_EVENT,
  LOADING_STARTED_EVENT,
} from '@/services/events';
import apiDefinition from './api-spec.json';

export type SimpleAccountingClient = Client;

const api = new OpenAPIClientAxios({
  definition: apiDefinition as any as Document,
  axiosConfigDefaults: {
    baseURL: '/',
    timeout: 10000,
    paramsSerializer(params) {
      return qs.stringify(params, { arrayFormat: 'repeat' });
    },
  },
});

export const apiClient = api.initSync<SimpleAccountingClient>();

initAuth(apiClient);

function emitLoadingStartedEvent() {
  LOADING_STARTED_EVENT.emit();
}

function emitLoadingFinishedEvent() {
  LOADING_FINISHED_EVENT.emit();
}

apiClient.interceptors.request.use(
  (config) => {
    emitLoadingStartedEvent();
    return applyAuthorization(config);
  },
  async (error) => {
    emitLoadingFinishedEvent();
    throw error;
  },
);

apiClient.interceptors.response.use(
  (response) => {
    emitLoadingFinishedEvent();
    return response;
  },
  async (error) => {
    const errorHandlingResponse = await handleErrorResponse(error);
    if (errorHandlingResponse) {
      return errorHandlingResponse;
    }
    if (error.response && error.response.status !== 401
      && error.response.status >= 400
      && !error.config.skipGlobalErrorHandler) {
      API_FATAL_ERROR_EVENT.emit(error);
    }
    emitLoadingFinishedEvent();
    throw error;
  },
);
