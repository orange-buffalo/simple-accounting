import { FormItemContext } from 'element-plus';
import { FieldError } from '@/components/form/sa-form-api.ts';
import { FieldErrorDto } from '@/services/api';
import { $t } from '@/services/i18n';

/**
 * Sets form item errors based on the API response.
 */
export function setFieldsErrorsFromApiResponse(
  apiFieldErrors: FieldErrorDto[],
  formItems: Map<string, FormItemContext>,
) {
  setFieldErrorsFromClientSideValidation(
    apiFieldErrors.map((apiError) => ({
      field: apiError.field,
      message: getApiFieldErrorMessage(apiError),
    })),
    formItems,
  );
}

/**
 * Sets form item errors based on the client-side validation.
 */
export function setFieldErrorsFromClientSideValidation(
  fieldErrors: FieldError[],
  formItems: Map<string, FormItemContext>,
) {
  fieldErrors.forEach((fieldError) => {
    const formItem = formItems.get(fieldError.field);
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
function getApiFieldErrorMessage(fieldError: FieldErrorDto) {
  switch (fieldError.error) {
    case 'SizeConstraintViolated':
      return fieldError.params.min && Number(fieldError.params.min) > 1
        ? $t.value.formValidationMessages.sizeMinMax(Number(fieldError.params.min), Number(fieldError.params.max))
        : $t.value.formValidationMessages.sizeMax(Number(fieldError.params.max));
    case 'MustNotBeBlank':
      return $t.value.formValidationMessages.notBlank();
    default:
      return fieldError.message;
  }
}
