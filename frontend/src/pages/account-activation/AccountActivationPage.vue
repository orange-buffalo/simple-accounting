<template>
  <SaStatusLabel
    v-if="status === 'LOADING'"
    status="regular"
    custom-icon="loading"
  >
    {{ $t.accountActivationPage.loading() }}
  </SaStatusLabel>

  <SaStatusLabel
    status="failure"
    v-if="status === 'BAD_TOKEN'"
  >
    {{ $t.accountActivationPage.badToken() }}
  </SaStatusLabel>

  <template v-if="status === 'TOKEN_VALIDATED'">
    <SaStatusLabel status="regular" hide-icon>
      {{ $t.accountActivationPage.instructions() }}
    </SaStatusLabel>
    <br />
    <br />
    <SaForm
      v-model="form"
      :on-submit="activateAccount"
      :submit-button-label="$t.accountActivationPage.form.submit()"
    >
      <template #default>
        <SaFormInput
          :label="$t.accountActivationPage.form.password.label()"
          prop="password"
          type="password"
          :placeholder="$t.accountActivationPage.form.password.placeholder()"
        />

        <SaFormInput
          :label="$t.accountActivationPage.form.passwordConfirmation.label()"
          prop="passwordConfirmation"
          type="password"
          :placeholder="$t.accountActivationPage.form.passwordConfirmation.placeholder()"
        />
      </template>
    </SaForm>
  </template>

  <template v-if="status === 'ACCOUNT_ACTIVATED'">
    <SaStatusLabel status="success">
      {{ $t.accountActivationPage.success.message() }}
    </SaStatusLabel>
    <div class="account-activation-page__success-panel__button-panel">
      <ElButton type="primary" @click="$router.push({ name: 'login' })">
        {{ $t.accountActivationPage.success.button() }}
      </ElButton>
    </div>
  </template>
</template>

<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import SaForm from '@/components/form/SaForm.vue';
import SaFormInput from '@/components/form/SaFormInput.vue';
import { ClientSideValidationError } from '@/components/form/sa-form-api.ts';
import SaStatusLabel from '@/components/SaStatusLabel.vue';
import {
  UserActivationRequestDto,
  UserActivationTokensApiActivateUserErrors,
  userActivationTokensApi,
} from '@/services/api';
import { ApiBusinessError, ResourceNotFoundError } from '@/services/api/api-errors.ts';
import { $t } from '@/services/i18n';

const props = defineProps<{
  token: string;
}>();

type Status = 'LOADING' | 'BAD_TOKEN' | 'TOKEN_VALIDATED' | 'ACCOUNT_ACTIVATED';
const status = ref<Status>('LOADING');

const executeTokenApiRequest = async (spec: () => Promise<void>) => {
  try {
    await spec();
  } catch (e: unknown) {
    if (e instanceof ResourceNotFoundError) {
      status.value = 'BAD_TOKEN';
    } else if (e instanceof ApiBusinessError) {
      const { error } = e.errorAs<UserActivationTokensApiActivateUserErrors>();
      if (error === 'TokenExpired') {
        status.value = 'BAD_TOKEN';
      } else {
        throw e;
      }
    } else {
      throw e;
    }
  }
};

onMounted(async () => {
  await executeTokenApiRequest(async () => {
    await userActivationTokensApi.getToken({
      token: props.token,
    });
    status.value = 'TOKEN_VALIDATED';
  });
});

type FormValues = Partial<{
  password: string;
  passwordConfirmation: string;
}>;
const form = ref<FormValues>({});

const activateAccount = async () => {
  if (form.value.password !== form.value.passwordConfirmation) {
    throw new ClientSideValidationError([
      {
        field: 'passwordConfirmation',
        message: $t.value.accountActivationPage.form.passwordConfirmation.notMatchingError(),
      },
    ]);
  }

  await executeTokenApiRequest(async () => {
    await userActivationTokensApi.activateUser({
      token: props.token,
      userActivationRequestDto: form.value as UserActivationRequestDto,
    });
    status.value = 'ACCOUNT_ACTIVATED';
  });
};
</script>

<style lang="scss">
  .account-activation-page {
    &__success-panel {
      &__button-panel {
        margin-top: 20px;
        display: flex;
        justify-content: center;
      }
    }
  }
</style>
