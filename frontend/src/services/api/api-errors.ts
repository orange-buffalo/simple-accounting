import { FieldErrorDto, InvalidInputErrorDto, SaApiErrorDto } from '@/services/api/generated';

/**
 * Base class for API errors. All API errors should extend this class.
 */
export class ApiError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

/**
 * Indicates an error that occurred on the client side, essentially when the response
 * is not available.
 */
export class ClientApiError extends ApiError {
  error: unknown;

  response?: Response;

  constructor(message: string, error: unknown, response?: Response) {
    super(message);
    this.error = error;
    this.name = 'ClientApiError';
    this.response = response;
  }
}

/**
 * Indicates an error that occurred on the server side, when the response is available.
 */
export class ServerApiError extends ApiError {
  response: Response;

  constructor(message: string, response: Response) {
    super(message);
    this.name = 'ServerApiError';
    this.response = response.clone();
  }
}

/**
 * Indicates a 404 error response.
 */
export class ResourceNotFoundError extends ServerApiError {
  constructor(response: Response) {
    super('Resource or endpoint is not found', response);
    this.name = 'ResourceNotFoundError';
  }
}

/**
 * Indicates a field-level validation error (400 HTTP status with detailed information about failing fields).
 */
export class ApiFieldLevelValidationError extends ServerApiError {
  fieldErrors: Array<FieldErrorDto>;

  constructor(response: Response, responseBody: InvalidInputErrorDto) {
    super('Request failed with invalid input', response);
    this.name = 'ApiFieldLevelValidationError';
    this.fieldErrors = responseBody.requestErrors;
  }
}

/**
 * Indicates a business error (400 HTTP status with custom body).
 */
export class ApiBusinessError extends ServerApiError {
  error: SaApiErrorDto;

  constructor(response: Response, error: SaApiErrorDto) {
    super(`Business error: ${error.error}`, response);
    this.name = 'ApiBusinessError';
    this.error = error;
  }

  errorAs<T extends SaApiErrorDto>(): T {
    return this.error as T;
  }
}

/**
 * Indicates an 5xx error response or unsupported 4xx response (e.g. non-standard JSON body).
 */
export class FatalApiError extends ServerApiError {
  constructor(message: string, response: Response) {
    super(message, response);
    this.name = 'FatalApiError';
  }
}

/**
 * Indicates a timeout error during API request.
 */
export class ApiTimeoutError extends ApiError {
  constructor(message: string) {
    super(message);
    this.name = 'ApiTimeoutError';
  }
}

/**
 * Indicates a request was cancelled before it was completed (programmatically).
 */
export class ApiRequestCancelledError extends ApiError {
  constructor() {
    super('Request was cancelled before it was completed');
    this.name = 'ApiRequestCancelledError';
  }
}

/**
 * Indicates that authentication is required for the API.
 */
export class ApiAuthError extends ServerApiError {
  constructor(response: Response) {
    super('Authentication is required', response);
    this.name = 'ApiAuthError';
  }
}
