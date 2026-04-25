import { Ref } from 'vue';

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

type UnionToIntersection<T> =
  (T extends unknown ? (x: T) => void : never) extends (x: infer R) => void ? R : never;

type Optional<T> = {
  [K in keyof T]?: T[K] | null;
};

type StrictSubset<T extends object, TShape extends object> =
  T & Record<Exclude<keyof T, keyof TShape>, never>;

/**
 * Creates a form values type from API types by intersecting them and making all fields optional.
 *
 * Example:
 * type MyFormValues = AsFormValues<[ApiTypeA, ApiTypeB]>;
 */
export type AsFormValues<TApiTypes extends readonly [object, ...object[]]> =
  {
    [K in keyof UnionToIntersection<TApiTypes[number]>]?:
    UnionToIntersection<TApiTypes[number]>[K] | null;
  };

/**
 * Common properties for all form components.
 */
export type SaFormComponentProps = {
  label?: string;
  prop: string;
};

export function updateFormValues<
  FV extends object,
  RT extends object,
  TPostProcessed extends Partial<FV> = Partial<FV>,
>(
  formValues: Ref<FV>,
  apiResponse?: RT | null,
  postProcessFn?: (apiResponse: RT) => StrictSubset<TPostProcessed, FV>,
): void {
  if (apiResponse) {
    let updatedValues: FV = {
      ...formValues.value,
      ...(apiResponse as Partial<FV>),
    };
    if (postProcessFn) {
      updatedValues = {
        ...updatedValues,
        ...postProcessFn(apiResponse),
      };
    }
    formValues.value = updatedValues;
  } else {
    throw new Error('Expected API response but did not receive it');
  }
}

export function toRequestArgs<RT, FV extends Optional<RT>>(formValues: Ref<FV>): RT {
  return formValues.value as unknown as RT;
}
