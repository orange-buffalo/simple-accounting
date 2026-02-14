<template>
  <SaForm
    v-model="formValues"
    :on-submit="submitChangePassword"
    :submit-button-label="$t.myProfile.changePassword.submit.label()"
    :submit-button-disabled="submitDisabled"
  >
    <h2>{{ $t.myProfile.changePassword.header() }}</h2>

    <div class="row">
      <div class="col col-xs-12 col-lg-6">
        <SaFormInput
          prop="currentPassword"
          :label="$t.myProfile.changePassword.currentPassword.label()"
          :placeholder="$t.myProfile.changePassword.currentPassword.placeholder()"
          type="password"
        />
      </div>
    </div>

    <div class="row">
      <div class="col col-xs-12 col-lg-6">
        <SaFormInput
          prop="newPassword"
          :label="$t.myProfile.changePassword.newPassword.label()"
          :placeholder="$t.myProfile.changePassword.newPassword.placeholder()"
          type="password"
        />
      </div>
    </div>

    <div class="row">
      <div class="col col-xs-12 col-lg-6">
        <SaFormInput
          prop="newPasswordConfirmation"
          :label="$t.myProfile.changePassword.newPasswordConfirmation.label()"
          :placeholder="$t.myProfile.changePassword.newPasswordConfirmation.placeholder()"
          type="password"
        />
      </div>
    </div>
  </SaForm>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue';
import SaForm from '@/components/form/SaForm.vue';
import SaFormInput from '@/components/form/SaFormInput.vue';
import { ClientSideValidationError, FieldError } from '@/components/form/sa-form-api';
import useNotifications from '@/components/notifications/use-notifications';
import { handleGqlApiBusinessError } from '@/services/api';
import { graphql } from '@/services/api/gql';
import { ChangePasswordErrorCodes } from '@/services/api/gql/graphql.ts';
import { useMutation } from '@/services/api/use-gql-api.ts';
import { $t } from '@/services/i18n';

interface PasswordFormValues {
  currentPassword: string;
  newPassword: string;
  newPasswordConfirmation: string;
}

const formValues = ref<PasswordFormValues>({
  currentPassword: '',
  newPassword: '',
  newPasswordConfirmation: '',
});

const submitDisabled = computed<boolean>(
  () =>
    formValues.value.currentPassword === '' ||
    formValues.value.newPassword === '' ||
    formValues.value.newPasswordConfirmation === '',
);

const { showSuccessNotification } = useNotifications();

const changePasswordMutation = useMutation(
  graphql(/* GraphQL */ `
    mutation changePassword($currentPassword: String!, $newPassword: String!) {
      changePassword(currentPassword: $currentPassword, newPassword: $newPassword) {
        success
      }
    }
  `),
  'changePassword',
);

const submitChangePassword = async () => {
  // Validate password confirmation
  if (formValues.value.newPasswordConfirmation !== formValues.value.newPassword) {
    const errors: FieldError[] = [
      {
        field: 'newPasswordConfirmation',
        message: $t.value.myProfile.changePassword.validations.confirmationDoesNotMatch(),
      },
    ];
    throw new ClientSideValidationError(errors);
  }

  try {
    await changePasswordMutation({
      currentPassword: formValues.value.currentPassword,
      newPassword: formValues.value.newPassword,
    });

    // Clear form after successful password change
    formValues.value = {
      currentPassword: '',
      newPassword: '',
      newPasswordConfirmation: '',
    };

    showSuccessNotification($t.value.myProfile.changePassword.feedback.success());
  } catch (e: unknown) {
    const errorCode = handleGqlApiBusinessError<ChangePasswordErrorCodes>(e);
    if (errorCode === ChangePasswordErrorCodes.CurrentPasswordMismatch) {
      const errors: FieldError[] = [
        {
          field: 'currentPassword',
          message: $t.value.myProfile.changePassword.validations.currentPasswordMismatch(),
        },
      ];
      throw new ClientSideValidationError(errors);
    } else {
      throw e;
    }
  }
};
</script>
