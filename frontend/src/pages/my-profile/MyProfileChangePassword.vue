<template>
  <SaForm
    :model="passwordData"
    ref="formRef"
    style="margin-top: 30px"
    :rules="formValidations"
  >
    <template #default>
      <h2>{{ $t.myProfile.changePassword.header() }}</h2>

      <div class="row">
        <div class="col col-xs-12 col-lg-6">
          <ElFormItem
            :label="$t.myProfile.changePassword.currentPassword.label()"
            prop="currentPassword"
            :ref="currentPasswordValidation.formItem"
          >
            <ElInput
              v-model="passwordData.currentPassword"
              :placeholder="$t.myProfile.changePassword.currentPassword.placeholder()"
              type="password"
              @keyup="currentPasswordValidation.resetErrors"
            />
          </ElFormItem>
        </div>
      </div>

      <div class="row">
        <div class="col col-xs-12 col-lg-6">
          <ElFormItem
            :label="$t.myProfile.changePassword.newPassword.label()"
            prop="newPassword"
          >
            <ElInput
              v-model="passwordData.newPassword"
              :placeholder="$t.myProfile.changePassword.newPassword.placeholder()"
              type="password"
            />
          </ElFormItem>
        </div>
      </div>

      <div class="row">
        <div class="col col-xs-12 col-lg-6">
          <ElFormItem
            :label="$t.myProfile.changePassword.newPasswordConfirmation.label()"
            prop="newPasswordConfirmation"
          >
            <ElInput
              v-model="passwordData.newPasswordConfirmation"
              :placeholder="$t.myProfile.changePassword.newPasswordConfirmation.placeholder()"
              type="password"
            />
          </ElFormItem>
        </div>
      </div>
    </template>

    <template #buttons-bar>
      <ElButton
        type="primary"
        @click="submitForm"
        :disabled="submitDisabled"
      >
        {{ $t.myProfile.changePassword.submit.label() }}
      </ElButton>
    </template>
  </SaForm>
</template>

<script lang="ts" setup>
  import { computed, reactive } from 'vue';
  import type { FormRules } from 'element-plus';
  import { $t } from '@/services/i18n';
  import SaForm from '@/components/form/SaForm.vue';
  import { useForm, useFormItemValidation } from '@/components/form/use-form';
  import useNotifications from '@/components/notifications/use-notifications';
  import {
    handleApiBusinessError,
    profileApi,
    ProfileApiBadRequestErrorsErrorEnum,
  } from '@/services/api';
  import type { ProfileApiBadRequestErrors } from '@/services/api';

  interface PasswordData {
    currentPassword: string,
    newPassword: string,
    newPasswordConfirmation: string,
  }

  const passwordData = reactive<PasswordData>({
    currentPassword: '',
    newPassword: '',
    newPasswordConfirmation: '',
  });

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const validateConfirmation = (_: any, value: any, callback: any) => {
    if (value !== passwordData.newPassword) {
      callback(new Error($t.value.myProfile.changePassword.validations.confirmationDoesNotMatch()));
    } else {
      callback();
    }
  };

  const formValidations = reactive<FormRules<PasswordData>>({
    newPasswordConfirmation: [{
      validator: validateConfirmation,
      trigger: 'submit',
    }],
  });

  const submitDisabled = computed<boolean>(() => passwordData.currentPassword === ''
    || passwordData.newPassword === ''
    || passwordData.newPasswordConfirmation === '');

  const { showSuccessNotification } = useNotifications();

  const currentPasswordValidation = useFormItemValidation();

  const updatePassword = async () => {
    try {
      await profileApi.changePassword({
        changePasswordRequestDto: {
          currentPassword: passwordData.currentPassword,
          newPassword: passwordData.newPassword,
        },
      });
      showSuccessNotification($t.value.myProfile.changePassword.feedback.success());
    } catch (e: unknown) {
      const errorResponse = handleApiBusinessError<ProfileApiBadRequestErrors>(e);
      if (errorResponse.error === ProfileApiBadRequestErrorsErrorEnum.CurrentPasswordMismatch) {
        currentPasswordValidation.setValidationError(
          $t.value.myProfile.changePassword.validations.currentPasswordMismatch(),
        );
      } else {
        throw e;
      }
    }
  };

  const {
    formRef,
    submitForm,
  } = useForm(async () => {
    // no op
  }, updatePassword);
</script>
