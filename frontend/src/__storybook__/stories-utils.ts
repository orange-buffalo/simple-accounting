import { FieldErrorDto, InvalidInputErrorDto } from '@/services/api';
import { ApiFieldLevelValidationError } from '@/services/api/api-errors.ts';

export function delay(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export function throwApiFieldLevelValidationError(...errors: FieldErrorDto[]) {
  const responseBody = {
    error: 'InvalidInput',
    requestErrors: errors,
  } as InvalidInputErrorDto;
  const response = Response.json(responseBody, {
    status: 400,
  });
  throw new ApiFieldLevelValidationError(response, responseBody);
}
