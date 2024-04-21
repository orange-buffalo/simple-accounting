/**
 * A custom error for a field.
 */
export type FieldError = {
  /**
   * Field name or path if nested.
   */
  field: string;
  /**
   * Localized message.
   */
  message: string;
};

/**
 * Indicates a client-side validation error.
 * SaForm processes this error and sets the validation state on the form items.
 */
export class ClientSideValidationError extends Error {
  fieldErrors: FieldError[];

  constructor(fieldErrors: FieldError[]) {
    super('Client side validation failed');
    this.name = 'ClientSideValidationError';
    this.fieldErrors = fieldErrors;
  }
}
