<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>
    <SaForm v-model="formValues" :on-submit="saveUser" :on-load="loadUser" :on-cancel="navigateToUsersOverview">
      <SaFormInput prop="userName" :label="$t.editUser.form.userName.label()" />
      <SaFormSelect
        :label="$t.editUser.form.role.label()"
        prop="admin"
        :disabled="editMode"
      >
        <ElOption :label="$t.editUser.form.role.options.user()" :value="false" />
        <ElOption :label="$t.editUser.form.role.options.admin()" :value="true" />
      </SaFormSelect>
      <ElFormItem :label="$t.editUser.form.activationStatus.label()" v-if="editMode">
        <SaInputLoader v-if="activationStatus.loading" loading />
        <SaStatusLabel v-else-if="!activationStatus.activationUrl" status="success" simplified>
          {{ $t.editUser.form.activationStatus.activated() }}
        </SaStatusLabel>
        <template v-else>
          <SaStatusLabel status="pending" simplified>
            {{ $t.editUser.form.activationStatus.notActivated() }}
          </SaStatusLabel>
          <br />
          <SaActionLink icon="copy" @click="copyActivationUrl">
            {{ activationStatus.activationUrl}}
          </SaActionLink>
        </template>
      </ElFormItem>
    </SaForm>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue';
import SaForm from '@/components/form/SaForm.vue';
import SaFormInput from '@/components/form/SaFormInput.vue';
import SaFormSelect from '@/components/form/SaFormSelect.vue';
import { ClientSideValidationError } from '@/components/form/sa-form-api.ts';
import useNotifications from '@/components/notifications/use-notifications.ts';
import SaActionLink from '@/components/SaActionLink.vue';
import SaInputLoader from '@/components/SaInputLoader.vue';
import SaStatusLabel from '@/components/SaStatusLabel.vue';
import {
  handleApiBusinessError,
  UserActivationTokenDto,
  UsersApiCreateUserErrors,
  UsersApiUpdateUserErrors,
  userActivationTokensApi,
} from '@/services/api';
import { usersApi } from '@/services/api/api-client.ts';
import { ApiBusinessError, ResourceNotFoundError } from '@/services/api/api-errors.ts';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';

const props = defineProps<{
  id?: number;
}>();

const editMode = computed(() => props.id !== undefined);

const { showSuccessNotification } = useNotifications();

const { navigateByViewName, navigateToView } = useNavigation();
const navigateToUsersOverview = async () => {
  await navigateByViewName('users-overview');
};
const navigateToEditUser = async (id: number) => {
  await navigateToView({
    name: 'edit-user',
    params: {
      id,
    },
  });
};

type FormValues = {
  userName: string;
  admin: boolean;
};

const formValues = ref<FormValues>({
  admin: false,
  userName: '',
});

type UserApiErrors = UsersApiCreateUserErrors | UsersApiUpdateUserErrors;
const saveUser = async () => {
  try {
    if (editMode.value) {
      await usersApi.updateUser({
        userId: props.id!,
        updateUserRequestDto: formValues.value,
      });
      await navigateToUsersOverview();
    } else {
      const createdUser = await usersApi.createUser({
        createUserRequestDto: formValues.value,
      });
      await navigateToEditUser(createdUser.id);
    }
    showSuccessNotification($t.value.editUser.successNotification(formValues.value.userName));
  } catch (e: unknown) {
    if (e instanceof ApiBusinessError) {
      const error = handleApiBusinessError<UserApiErrors>(e);
      if (error.error === 'UserAlreadyExists') {
        throw new ClientSideValidationError([
          {
            field: 'userName',
            message: $t.value.editUser.form.userName.errors.userAlreadyExists(formValues.value.userName),
          },
        ]);
      }
    }
    throw e;
  }
};

const activationStatus = ref({
  loading: true,
  activationUrl: '',
});

const loadUser = editMode.value
  ? async () => {
      const userId = props.id!;
      const user = await usersApi.getUser({
        userId,
      });
      formValues.value = user;

      if (user.activated) {
        activationStatus.value.loading = false;
      } else {
        let token: UserActivationTokenDto;
        try {
          token = await userActivationTokensApi.getTokenByUser({
            userId,
          });
        } catch (e: unknown) {
          // expired token - recreate
          if (e instanceof ResourceNotFoundError) {
            token = await userActivationTokensApi.createToken({
              createUserActivationTokenRequestDto: {
                userId,
              },
            });
          } else {
            throw e;
          }
        }
        activationStatus.value.activationUrl = `${window.location.origin}/activate-account/${token.token}`;
        activationStatus.value.loading = false;
      }
    }
  : undefined;

const pageHeader = computed(() =>
  editMode.value ? $t.value.editUser.pageHeader.edit() : $t.value.editUser.pageHeader.create(),
);

const copyActivationUrl = async () => {
  await navigator.clipboard.writeText(activationStatus.value.activationUrl);
  showSuccessNotification($t.value.editUser.form.activationStatus.copied());
};
</script>
