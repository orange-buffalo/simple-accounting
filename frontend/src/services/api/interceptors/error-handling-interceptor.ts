import type { RequestMetadata } from '@/services/api/api-client';
import type { Middleware } from '@/services/api/generated';
import { API_BAD_REQUEST_EVENT, API_FATAL_ERROR_EVENT } from '@/services/events';
import { FetchError, ResponseError } from '@/services/api/generated';

function shouldSkipGlobalErrorHandler(metadata?: RequestMetadata) {
  return metadata && metadata.skipGlobalErrorHandler;
}

export const errorHandlingInterceptor: Middleware<RequestMetadata> = {
  async post({
    response,
    metadata,
  }): Promise<Response | void> {
    if (response.status === 400 && !shouldSkipGlobalErrorHandler(metadata)) {
      API_BAD_REQUEST_EVENT.emit(new ResponseError(response));
    } else if (response.status > 401 && !shouldSkipGlobalErrorHandler(metadata)) {
      API_FATAL_ERROR_EVENT.emit(new ResponseError(response));
    }
  },

  async onError({
    error,
    response,
    metadata,
  }): Promise<Response | void> {
    if (!response && !shouldSkipGlobalErrorHandler(metadata)) {
      if (error instanceof Error) {
        API_FATAL_ERROR_EVENT.emit(new FetchError(error));
      } else {
        API_FATAL_ERROR_EVENT.emit(new Error('Request execution failed'));
      }
    }
  },
};
