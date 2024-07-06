<template>
  <div class="sa-form">
    <ElForm
      ref="elForm"
      v-loading="loading"
      label-position="top"
      :model="isLegacyApi ? props.model : formValues"
      :rules="rules"
    >
      <slot />

      <hr>

      <div class="sa-form__buttons-bar-legacy" v-if="isLegacyApi">
        <slot name="buttons-bar" />
      </div>
      <div class="sa-form__buttons-bar" v-else>
        <ElButton
          type="primary"
          @click="submitForm"
          :disabled="props.submitButtonDisabled"
        >
          {{ props.submitButtonLabel || $t.common.save() }}
        </ElButton>

        <ElButton @click="onCancel" v-if="onCancel" link>
          {{ props.cancelButtonLabel || $t.common.cancel() }}
        </ElButton>
      </div>
    </ElForm>
  </div>
</template>

<script lang="ts" setup>
  import {
    ElForm, FormInstance, FormItemContext, FormRules,
  } from 'element-plus';
  import { onMounted, ref } from 'vue';
  import { $t } from '@/services/i18n';
  import { FormValues, provideSaFormComponentsApi } from '@/components/form/sa-form-components-api.ts';
  import { ensureDefined, hasValue } from '@/services/utils.ts';
  import { ApiFieldLevelValidationError } from '@/services/api/api-errors.ts';
  import {
    setFieldErrorsFromClientSideValidation,
    setFieldsErrorsFromApiResponse,
  } from '@/components/form/api-field-error-messages.ts';
  import { ClientSideValidationError } from '@/components/form/sa-form-api.ts';
  import useNotifications from '@/components/notifications/use-notifications.ts';

  type SaFormProps = {
    // legacy API will provide this, while new API v-model
    model?: FormValues,
    // legacy API
    rules?: FormRules,
    onCancel?: () => Promise<unknown> | unknown,
    cancelButtonLabel?: string,
    submitButtonLabel?: string,
    submitButtonDisabled?: boolean,
    onLoad?: () => Promise<unknown> | unknown,
    // optional to support legacy API
    onSubmit?: () => Promise<unknown> | unknown,
  };

  // new API uses v-model
  const formValues = defineModel<FormValues>();

  const props = withDefaults(defineProps<SaFormProps>(), {
    submitButtonDisabled: false,
  });

  const isLegacyApi = !hasValue(props.onSubmit);

  if (!isLegacyApi && props.rules) {
    throw new Error('Rules are not supported in the new API, use ClientSideValidationError instead.');
  }
  if (!isLegacyApi && (!formValues.value || props.model)) {
    throw new Error('v-model must be used in the new API.');
  }
  if (isLegacyApi && (!props.model || formValues.value)) {
    throw new Error('Model prop must be used in the legacy API.');
  }

  const elForm = ref<FormInstance | undefined>(undefined);
  const loading = ref(isLegacyApi);

  const formItems = new Map<string, FormItemContext>();
  provideSaFormComponentsApi({
    registerFormItem: (prop: string, formItem: FormItemContext) => {
      formItems.set(prop, formItem);
    },
    unregisterFormItem: (prop: string) => {
      formItems.delete(prop);
    },
    formValues,
  });

  const { showWarningNotification } = useNotifications();
  const submitForm = async () => {
    // legacy API - validations via rules

    // form validation throws an exception if validation fails
    if (props.rules) {
      try {
        await ensureDefined(elForm.value)
          .validate();
      } catch (e: unknown) {
        return;
      }
    }

    // modern API - validations via ClientSideValidationError in onSubmit
    loading.value = true;
    try {
      if (props.onSubmit) {
        await props.onSubmit();
      }
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
      loading.value = false;
    }
  };

  onMounted(async () => {
    if (props.onLoad) {
      loading.value = true;
      try {
        await props.onLoad();
      } finally {
        loading.value = false;
      }
    }
  });

  // legacy API - should not be used any longer
  const validate = async (): Promise<boolean> => {
    if (elForm.value) {
      return elForm.value.validate();
    }
    return false;
  };
  const startLoading = () => {
    loading.value = true;
  };
  const stopLoading = () => {
    loading.value = false;
  };
  defineExpose({
    validate,
    startLoading,
    stopLoading,
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

    &__buttons-bar-legacy {
      margin-top: 20px;
      display: flex;
      justify-content: space-between;
    }

    &__buttons-bar {
      margin-top: 20px;
      display: flex;
      gap: 20px;
    }
  }
</style>
