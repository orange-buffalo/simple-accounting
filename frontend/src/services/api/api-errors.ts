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
  constructor(message: string) {
    super(message);
    this.name = 'ClientApiError';
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
    this.response = response;
  }
}

/**
 * Indicates a 404 error response.
 */
export class ApiEndpointNotFoundError extends ServerApiError {
  constructor(message: string, response: Response) {
    super(message, response);
    this.name = 'ApiEndpointNotFoundError';
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
