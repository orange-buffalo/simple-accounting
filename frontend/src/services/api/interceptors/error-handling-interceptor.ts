import type { RequestMetadata } from '@/services/api/api-client';
import type { Middleware } from '@/services/api/generated';
import { API_FATAL_ERROR_EVENT } from '@/services/events';
import { FetchError, ResponseError } from '@/services/api/generated';

function shouldSkipGlobalErrorHandler(metadata?: RequestMetadata) {
  return metadata && metadata.skipGlobalErrorHandler;
}

export const errorHandlingInterceptor: Middleware<RequestMetadata> = {
  async post({
    response,
    metadata,
  }): Promise<Response | void> {
    if (response.status >= 400 && !shouldSkipGlobalErrorHandler(metadata) && response.status !== 401) {
      API_FATAL_ERROR_EVENT.emit(new ResponseError(response));
    }
  },

  async onError({
    error,
    response,
    metadata,
  }): Promise<Response | void> {
    if (!response && !shouldSkipGlobalErrorHandler(metadata)) {
      API_FATAL_ERROR_EVENT.emit(new FetchError(error));
    }
  },
};
