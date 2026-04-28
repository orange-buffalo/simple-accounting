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
  import { $t } from '@/services/i18n';
  import SaForm from '@/components/form/SaForm.vue';
  import useNavigation from '@/services/use-navigation';
  import { handleGqlApiBusinessError } from '@/services/api';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import useNotifications from '@/components/notifications/use-notifications.ts';
  import SaFormSelect from '@/components/form/SaFormSelect.vue';
  import {
    AsFormValues, ClientSideValidationError, toRequestArgs, updateFormValues,
  } from '@/components/form/sa-form-api.ts';
  import SaInputLoader from '@/components/SaInputLoader.vue';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import SaActionLink from '@/components/SaActionLink.vue';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery, useMutation } from '@/services/api/use-gql-api.ts';
  import {
    CreateUserErrorCodes, CreateUserMutationVariables, EditUserErrorCodes, EditUserMutationVariables,
  } from '@/services/api/gql/graphql.ts';

  const props = defineProps<{
    id?: number
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

  const getUserQuery = useLazyQuery(graphql(/* GraphQL */ `
    query getUserForEdit($userId: Long!) {
      user(id: $userId) {
        id
        userName
        activated
        admin
      }
    }
  `), 'user');

  const createUserMutation = useMutation(graphql(/* GraphQL */ `
    mutation createUser($userName: String!, $admin: Boolean!) {
      createUser(userName: $userName, admin: $admin) {
        id
        userName
      }
    }
  `), 'createUser');

  const editUserMutation = useMutation(graphql(/* GraphQL */ `
    mutation editUser($id: Long!, $userName: String!) {
      editUser(id: $id, userName: $userName) {
        id
        userName
      }
    }
  `), 'editUser');

  const tokenByUserQuery = useLazyQuery(graphql(/* GraphQL */ `
    query tokenByUser($userId: Long!) {
      tokenByUser(userId: $userId) {
        token
      }
    }
  `), 'tokenByUser');

  const createUserActivationTokenMutation = useMutation(graphql(/* GraphQL */ `
    mutation createUserActivationToken($userId: Long!) {
      createUserActivationToken(userId: $userId) {
        token
      }
    }
  `), 'createUserActivationToken');

  type UserFormValues = AsFormValues<[CreateUserMutationVariables, EditUserMutationVariables]>;

  const formValues = ref<UserFormValues>({
    admin: false,
    id: props.id,
  });

  const activationStatus = ref({
    loading: true,
    activationUrl: '',
  });

  const loadUser = editMode.value ? async () => {
    const userId = props.id!;
    const user = await getUserQuery({ userId });
    updateFormValues(formValues, user, loadedUser => ({
      userName: loadedUser.userName,
      admin: loadedUser.admin,
    }));

    if (user.activated) {
      activationStatus.value.loading = false;
    } else {
      const tokenFromQuery = await tokenByUserQuery({ userId });
      const token = tokenFromQuery ?? await createUserActivationTokenMutation({ userId });
      activationStatus.value.activationUrl = `${window.location.origin}/activate-account/${token.token}`;
      activationStatus.value.loading = false;
    }
  } : undefined;

  const saveUser = async () => {
    try {
      if (editMode.value) {
        await editUserMutation(toRequestArgs(formValues));
        await navigateToUsersOverview();
      } else {
        const createdUser = await createUserMutation(toRequestArgs(formValues));
        await navigateToEditUser(createdUser.id);
      }
      showSuccessNotification($t.value.editUser.successNotification(formValues.value.userName!));
    } catch (e: unknown) {
      const errorCode = handleGqlApiBusinessError<CreateUserErrorCodes | EditUserErrorCodes>(e);
      if (errorCode === CreateUserErrorCodes.UserAlreadyExists
        || errorCode === EditUserErrorCodes.UserAlreadyExists) {
        throw new ClientSideValidationError([{
          field: 'userName',
          message: $t.value.editUser.form.userName.errors.userAlreadyExists(formValues.value.userName!),
        }]);
      }
      throw e;
    }
  };

  const pageHeader = computed(() => (editMode.value
    ? $t.value.editUser.pageHeader.edit()
    : $t.value.editUser.pageHeader.create()));

  const copyActivationUrl = async () => {
    await navigator.clipboard.writeText(activationStatus.value.activationUrl);
    showSuccessNotification($t.value.editUser.form.activationStatus.copied());
  };

</script>
