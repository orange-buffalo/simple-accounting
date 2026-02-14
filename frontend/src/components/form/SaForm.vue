<template>
  <div class="sa-form">
    <ElForm
      ref="elForm"
      label-position="top"
      :model="formValues"
    >
      <slot />

      <hr v-if="!props.hideButtons">

      <div class="sa-form__buttons-bar" v-if="!props.hideButtons">
        <ElButton
          type="primary"
          @click="submitForm"
          :disabled="props.submitButtonDisabled || loading"
        >
          {{ props.submitButtonLabel || $t.common.save() }}
        </ElButton>

        <ElButton @click="onCancel" v-if="onCancel" link :disabled="loading">
          {{ props.cancelButtonLabel || $t.common.cancel() }}
        </ElButton>
      </div>
    </ElForm>
  </div>
</template>

<script lang="ts" setup>
import { ElForm, type FormInstance, type FormItemContext } from 'element-plus';
import { computed, nextTick, onMounted, ref } from 'vue';
import {
  setFieldErrorsFromClientSideValidation,
  setFieldsErrorsFromApiResponse,
} from '@/components/form/api-field-error-messages.ts';
import { ClientSideValidationError } from '@/components/form/sa-form-api.ts';
import { type FormValues, provideSaFormComponentsApi } from '@/components/form/sa-form-components-api.ts';
import useNotifications from '@/components/notifications/use-notifications.ts';
import { ApiFieldLevelValidationError } from '@/services/api/api-errors.ts';
import { $t } from '@/services/i18n';

/**
 * Modern form component with declarative API.
 *
 * @example
 * ```vue
 * <SaForm v-model="formValues" :on-submit="saveData" :on-load="loadData" :on-cancel="cancel">
 *   <SaFormInput prop="name" label="Name" />
 *   <SaFormSelect prop="role" label="Role">
 *     <ElOption label="User" value="user" />
 *   </SaFormSelect>
 * </SaForm>
 * ```
 *
 * Key features:
 * - Uses `v-model` for two-way binding of form values
 * - Handles form submission via `:on-submit` callback
 * - Supports optional `:on-load` callback for data loading
 * - Supports optional `:on-cancel` callback for cancel action
 * - Automatically renders submit and cancel buttons
 * - Handles validation errors from API or client-side using `ClientSideValidationError`
 * - Shows loading state during submit and load operations
 *
 * For legacy forms with manual button control and rules-based validation,
 * use `SaLegacyForm` instead.
 */
type SaFormProps = {
  /**
   * Callback for handling cancel action. If not provided, cancel button will not be shown.
   */
  onCancel?: () => Promise<unknown> | unknown;
  /**
   * Label for the cancel button. Defaults to "Cancel" from translations.
   */
  cancelButtonLabel?: string;
  /**
   * Label for the submit button. Defaults to "Save" from translations.
   */
  submitButtonLabel?: string;
  /**
   * Whether to disable the submit button.
   */
  submitButtonDisabled?: boolean;
  /**
   * Callback for loading data when the form is mounted.
   * The form will show loading state until this completes.
   */
  onLoad?: () => Promise<unknown> | unknown;
  /**
   * Callback for handling form submission.
   * Should throw `ApiFieldLevelValidationError` or `ClientSideValidationError`
   * for field-level validation errors.
   */
  onSubmit: () => Promise<unknown> | unknown;
  /**
   * External loading state. When true, the form will show loading indicator.
   * This is merged with the internal loading state from onLoad and onSubmit.
   */
  externalLoading?: boolean;
  /**
   * Whether to hide the buttons bar (submit and cancel buttons).
   * Useful for forms that auto-submit on change.
   */
  hideButtons?: boolean;
};

// Modern API uses v-model
const formValues = defineModel<FormValues>({ required: true });

const props = withDefaults(defineProps<SaFormProps>(), {
  submitButtonDisabled: false,
  externalLoading: false,
  hideButtons: false,
});

const elForm = ref<FormInstance | undefined>(undefined);
const internalLoading = ref(false);
const loading = computed(() => internalLoading.value || props.externalLoading);

const formItems = new Map<string, FormItemContext>();

const { showWarningNotification } = useNotifications();
const submitForm = async () => {
  internalLoading.value = true;
  try {
    await props.onSubmit();
  } catch (e: unknown) {
    if (e instanceof ApiFieldLevelValidationError) {
      setFieldsErrorsFromApiResponse(e.fieldErrors, formItems);
      showWarningNotification($t.value.saForm.inputValidationFailed());
    } else if (e instanceof ClientSideValidationError) {
      setFieldErrorsFromClientSideValidation(e.fieldErrors, formItems);
      showWarningNotification($t.value.saForm.inputValidationFailed());
    } else {
      throw e;
    }
  } finally {
    internalLoading.value = false;
  }
};

provideSaFormComponentsApi({
  registerFormItem: (prop: string, formItem: FormItemContext) => {
    formItems.set(prop, formItem);
  },
  unregisterFormItem: (prop: string) => {
    formItems.delete(prop);
  },
  formValues,
  submitForm: async () => {
    // wait for Vue to propagate changes from form components
    await nextTick();
    await submitForm();
  },
  loading,
});

onMounted(async () => {
  if (props.onLoad) {
    internalLoading.value = true;
    try {
      await props.onLoad();
    } finally {
      internalLoading.value = false;
    }
  }
});
</script>

<style lang="scss">
  /*todo #73: common component refers to app styles - redesign dependencies  */
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .sa-form {
    padding: 20px;
    border: 1px solid $secondary-grey;
    background-color: $white;
    border-radius: 2px;
    overflow: hidden;
    margin-bottom: 30px;

    .el-form {
      width: 100%;
    }

    .el-form-item {
      margin-bottom: 15px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .sa-documents_upload {
      margin-top: 15px;
    }

    .el-select {
      @include input-width;
    }

    .el-input {
      @include input-width;
    }

    .el-form-item__label {
      padding: 0;
    }

    .el-input {
      .el-input-number & {
        width: 100%;
      }
    }

    h2 {
      margin-bottom: 10px;
    }

    hr {
      border: 1px solid $primary-grey;
      margin-top: 10px;
      margin-bottom: 10px;
    }

    &__buttons-bar {
      margin-top: 20px;
      display: flex;
      gap: 20px;
    }
  }
</style>
