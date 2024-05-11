import { $t } from '@/services/i18n';
import { FieldErrorDto } from '@/services/api';

/**
 * Translates field validation messages from the API response into UI messages, taking current user locale into
 * account. In case there is no mapping defined, fallbacks to API-provided non-localized message.
 */
export function getApiFieldErrorMessage(fieldError: FieldErrorDto) {
  switch (fieldError.error) {
  case 'SizeConstraintViolated':
    return (fieldError.params.min && (Number(fieldError.params.min) > 1))
      ? $t.value.formValidationMessages.sizeMinMax(Number(fieldError.params.min), Number(fieldError.params.max))
      : $t.value.formValidationMessages.sizeMax(Number(fieldError.params.max));
  default:
    return fieldError.message;
  }
}
