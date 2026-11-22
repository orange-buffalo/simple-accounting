import { $t } from '@/services/i18n';
import { FieldError } from '@/services/api/api-errors.ts';
import { FieldError as ClientFieldError } from '@/components/form/sa-form-api.ts';
import { SaFormItemRegistration } from '@/components/form/sa-form-components-api.ts';

/**
 * Sets form item errors based on the API response.
 */
export function setFieldsErrorsFromApiResponse(
  apiFieldErrors: FieldError[],
  formItems: ReadonlyMap<string, SaFormItemRegistration>,
) {
  setFieldErrorsFromClientSideValidation(
    apiFieldErrors.map((apiError) => ({
      field: apiError.field,
      message: getApiFieldErrorMessage(formItems.get(apiError.field)?.adaptApiError?.(apiError) ?? apiError),
    })),
    formItems,
  );
}

/**
 * Sets form item errors based on the client-side validation.
 */
export function setFieldErrorsFromClientSideValidation(
  fieldErrors: ClientFieldError[],
  formItems: ReadonlyMap<string, SaFormItemRegistration>,
) {
  fieldErrors.forEach((fieldError) => {
    const formItem = formItems.get(fieldError.field)?.context;
    if (formItem) {
      formItem.validateState = 'error';
      // @ts-ignore
      formItem.validateMessage = fieldError.message;
    } else {
      throw new Error(`Form item not found for field ${fieldError.field}`);
    }
  });
}

/**
 * Translates field validation messages from the API response into UI messages, taking current user locale into
 * account. In case there is no mapping defined, fallbacks to API-provided non-localized message.
 */
function getApiFieldErrorMessage(fieldError: FieldError) {
  switch (fieldError.error) {
  case 'SizeConstraintViolated':
    return (fieldError.params?.min && (Number(fieldError.params.min) > 1))
      ? $t.value.formValidationMessages.sizeMinMax(Number(fieldError.params.min), Number(fieldError.params.max))
      : $t.value.formValidationMessages.sizeMax(Number(fieldError.params?.max));
  case 'MustNotBeBlank':
    return $t.value.formValidationMessages.notBlank();
  case 'MustNotBeNull':
    return $t.value.formValidationMessages.notNull();
  case 'MinConstraintViolated':
    return $t.value.formValidationMessages.minConstraintViolated(Number(fieldError.params?.value));
  case 'MaxConstraintViolated':
    return $t.value.formValidationMessages.maxConstraintViolated(Number(fieldError.params?.value));
  case 'MustBeValidEndpointUrl':
    return $t.value.formValidationMessages.mustBeValidEndpointUrl();
  case 'MustBeValidCountryCode':
    return $t.value.formValidationMessages.mustBeValidCountryCode();
  default:
    return fieldError.message;
  }
}
