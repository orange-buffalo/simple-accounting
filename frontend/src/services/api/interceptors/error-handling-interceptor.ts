import {
  ApiAuthError,
  ApiBusinessError,
  ApiFieldLevelValidationError,
  ApiRequestCancelledError,
  ApiTimeoutError,
  ClientApiError,
  FatalApiError,
  ResourceNotFoundError,
} from '@/services/api/api-errors.ts';
import type { InvalidInputErrorDto, Middleware, SaApiErrorDto } from '@/services/api/generated';

/**
 * Processes responses according to RestApiControllerExceptionsHandler logic.
 */
export const errorHandlingInterceptor: Middleware = {
  async post({ response }): Promise<Response | void> {
    if (response.status === 404) {
      throw new ResourceNotFoundError(response);
    }

    if (response.status === 401) {
      throw new ApiAuthError(response);
    }

    const isJsonResponse = response.headers.get('content-type')?.includes('application/json');

    if (response.status === 400 && isJsonResponse) {
      const responseBody = await response.clone().json();
      if (responseBody.error === 'InvalidInput') {
        const invalidInputErrorDto = responseBody as InvalidInputErrorDto;
        throw new ApiFieldLevelValidationError(response, invalidInputErrorDto);
      } else {
        const saApiErrorDto = responseBody as SaApiErrorDto;
        throw new ApiBusinessError(response, saApiErrorDto);
      }
    }

    if (response.status >= 300) {
      throw new FatalApiError('Uncategorized API error happened', response);
    }
  },

  async onError({ error, response, init }): Promise<Response | void> {
    if (error instanceof DOMException) {
      if (error.name === 'AbortError') {
        const { reason } = init.signal;
        if (reason instanceof ApiRequestCancelledError) {
          // see useRequestConfig for the reason
          throw reason;
        } else if (reason?.name === 'TimeoutError') {
          // when useRequestConfig is used and timout is reached, it is wrapped into an AbortError
          throw new ApiTimeoutError('Request timed out');
        }
      } else if (error.name === 'TimeoutError') {
        // thrown by default global timeout
        throw new ApiTimeoutError('Request timed out');
      }
    }
    throw new ClientApiError(`Request failed with error: ${error}`, error, response);
  },
};
