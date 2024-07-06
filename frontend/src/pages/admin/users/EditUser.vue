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
    </SaForm>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { $t } from '@/services/i18n';
  import SaForm from '@/components/form/SaForm.vue';
  import useNavigation from '@/services/use-navigation';
  import {
    UsersApiCreateUserErrors,
    handleApiBusinessError, UsersApiUpdateUserErrors,
  } from '@/services/api';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import useNotifications from '@/components/notifications/use-notifications.ts';
  import { usersApi } from '@/services/api/api-client.ts';
  import SaFormSelect from '@/components/form/SaFormSelect.vue';
  import { ApiBusinessError } from '@/services/api/api-errors.ts';
  import { ClientSideValidationError } from '@/components/form/sa-form-api.ts';

  const props = defineProps<{
    id?: number
  }>();

  const editMode = computed(() => props.id !== undefined);

  const { showSuccessNotification } = useNotifications();

  const { navigateByViewName } = useNavigation();
  const navigateToUsersOverview = async () => {
    await navigateByViewName('users-overview');
  };

  type FormValues = {
    userName: string,
    admin: boolean,
  }

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
      } else {
        await usersApi.createUser({
          createUserRequestDto: formValues.value,
        });
      }
      showSuccessNotification($t.value.editUser.successNotification(formValues.value.userName));
      await navigateToUsersOverview();
    } catch (e: unknown) {
      if (e instanceof ApiBusinessError) {
        const error = handleApiBusinessError<UserApiErrors>(e);
        if (error.error === 'UserAlreadyExists') {
          throw new ClientSideValidationError([{
            field: 'userName',
            message: $t.value.editUser.form.userName.errors.userAlreadyExists(formValues.value.userName),
          }]);
        }
      }
      throw e;
    }
  };

  const loadUser = editMode.value ? async () => {
    formValues.value = await usersApi.getUser({
      userId: props.id!,
    });
  } : undefined;

  const pageHeader = editMode.value
    ? $t.value.editUser.pageHeader.edit()
    : $t.value.editUser.pageHeader.create();

</script>
