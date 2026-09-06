<template>
  <SaPage :header="pageHeader">
    <section v-if="!editMode">
      <h3>{{ $t.editOAuthProvider.oidcDiscovery.title() }}</h3>
      <p>{{ $t.editOAuthProvider.oidcDiscovery.description() }}</p>
      <SaForm
        v-model="oidcDiscoveryFormValues"
        :on-submit="discoverOidcProvider"
        :submit-button-label="$t.editOAuthProvider.oidcDiscovery.load()"
      >
        <SaFormInput
          prop="baseUrl"
          :label="$t.editOAuthProvider.oidcDiscovery.baseUrl.label()"
          :placeholder="$t.editOAuthProvider.oidcDiscovery.baseUrl.placeholder()"
        />
      </SaForm>
      <ElDivider />
    </section>

    <SaForm
      v-model="formValues"
      :on-submit="saveProvider"
      :on-load="loadProvider"
      :on-cancel="navigateToProvidersOverview"
    >
      <SaFormInput
        prop="name"
        :label="$t.editOAuthProvider.form.name.label()"
        :placeholder="$t.editOAuthProvider.form.name.placeholder()"
      />
      <SaFormInput
        prop="clientId"
        :label="$t.editOAuthProvider.form.clientId.label()"
        :placeholder="$t.editOAuthProvider.form.clientId.placeholder()"
      />
      <SaFormInput
        prop="clientSecret"
        type="password"
        :label="$t.editOAuthProvider.form.clientSecret.label()"
        :placeholder="editMode
          ? $t.editOAuthProvider.form.clientSecret.keepPlaceholder()
          : $t.editOAuthProvider.form.clientSecret.placeholder()"
      />
      <SaFormInput
        prop="authorizationUrl"
        :label="$t.editOAuthProvider.form.authorizationUrl.label()"
        :placeholder="$t.editOAuthProvider.form.authorizationUrl.placeholder()"
      />
      <SaFormInput
        prop="tokenUrl"
        :label="$t.editOAuthProvider.form.tokenUrl.label()"
        :placeholder="$t.editOAuthProvider.form.tokenUrl.placeholder()"
      />
      <SaFormInput
        prop="userInfoUrl"
        :label="$t.editOAuthProvider.form.userInfoUrl.label()"
        :placeholder="$t.editOAuthProvider.form.userInfoUrl.placeholder()"
      />
      <SaFormInput
        prop="userIdAttribute"
        :label="$t.editOAuthProvider.form.userIdAttribute.label()"
        :placeholder="$t.editOAuthProvider.form.userIdAttribute.placeholder()"
      />
      <SaFormInput
        prop="scopes"
        :label="$t.editOAuthProvider.form.scopes.label()"
        :placeholder="$t.editOAuthProvider.form.scopes.placeholder()"
      />

      <ElFormItem :label="$t.editOAuthProvider.form.callbackUrl.label()">
        <SaInputLoader v-if="callbackUrlLoading" loading />
        <SaActionLink
          v-else
          icon="copy"
          @click="copyCallbackUrl"
        >
          {{ callbackUrl }}
        </SaActionLink>
      </ElFormItem>
    </SaForm>
  </SaPage>
</template>

<script lang="ts" setup>
  import copy from 'copy-to-clipboard';
  import { computed, ref } from 'vue';
  import { $t } from '@/services/i18n';
  import SaPage from '@/components/SaPage.vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaInputLoader from '@/components/SaInputLoader.vue';
  import SaActionLink from '@/components/SaActionLink.vue';
  import useNavigation from '@/services/use-navigation';
  import { handleGqlApiBusinessError } from '@/services/api';
  import useNotifications from '@/components/notifications/use-notifications.ts';
  import { ClientSideValidationError } from '@/components/form/sa-form-api.ts';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery, useMutation, useQuery } from '@/services/api/use-gql-api.ts';
  import {
    CreateOAuthProviderErrorCodes,
    DiscoverOidcProviderConfigurationErrorCodes,
    EditOAuthProviderErrorCodes,
  } from '@/services/api/gql/schema-types.ts';

  const props = defineProps<{
    id?: string
  }>();

  const editMode = computed(() => props.id !== undefined);

  const { showSuccessNotification } = useNotifications();

  const { navigateByViewName, navigateToView } = useNavigation();
  const navigateToProvidersOverview = async () => {
    await navigateByViewName('oauth-providers-overview');
  };
  const navigateToEditProvider = async (id: string) => {
    await navigateToView({
      name: 'edit-oauth-provider',
      params: { id },
    });
  };

  const getProviderQuery = useLazyQuery(graphql(/* GraphQL */ `
    query getOAuthProviderForEdit($providerId: String!) {
      oauthProvider(id: $providerId) {
        id
        version
        name
        clientId
        authorizationUrl
        tokenUrl
        userInfoUrl
        userIdAttribute
        scopes
      }
    }
  `), 'oauthProvider');

  const createProviderMutation = useMutation(graphql(/* GraphQL */ `
    mutation createOAuthProvider(
      $name: String!
      $clientId: String!
      $clientSecret: String!
      $authorizationUrl: String!
      $tokenUrl: String!
      $userInfoUrl: String!
      $userIdAttribute: String!
      $scopes: [String!]!
    ) {
      createOAuthProvider(
        name: $name
        clientId: $clientId
        clientSecret: $clientSecret
        authorizationUrl: $authorizationUrl
        tokenUrl: $tokenUrl
        userInfoUrl: $userInfoUrl
        userIdAttribute: $userIdAttribute
        scopes: $scopes
      ) {
        id
        name
      }
    }
  `), 'createOAuthProvider');

  const editProviderMutation = useMutation(graphql(/* GraphQL */ `
    mutation editOAuthProvider(
      $id: String!
      $version: Int!
      $name: String!
      $clientId: String!
      $clientSecret: String
      $authorizationUrl: String!
      $tokenUrl: String!
      $userInfoUrl: String!
      $userIdAttribute: String!
      $scopes: [String!]!
    ) {
      editOAuthProvider(
        id: $id
        version: $version
        name: $name
        clientId: $clientId
        clientSecret: $clientSecret
        authorizationUrl: $authorizationUrl
        tokenUrl: $tokenUrl
        userInfoUrl: $userInfoUrl
        userIdAttribute: $userIdAttribute
        scopes: $scopes
      ) {
        id
        name
      }
    }
  `), 'editOAuthProvider');

  const discoverOidcProviderQuery = useLazyQuery(graphql(/* GraphQL */ `
    query discoverOidcProviderConfiguration($baseUrl: String!) {
      discoverOidcProviderConfiguration(baseUrl: $baseUrl) {
        authorizationUrl
        tokenUrl
        userInfoUrl
        userIdAttribute
        scopes
      }
    }
  `), 'discoverOidcProviderConfiguration');

  const [callbackUrlLoading, callbackUrlData] = useQuery(graphql(/* GraphQL */ `
    query oauthCallbackUrl {
      systemSettings {
        oauthCallbackUrl
      }
    }
  `), 'systemSettings');

  const callbackUrl = computed(() => callbackUrlData.value?.oauthCallbackUrl ?? '');

  type OAuthProviderFormValues = {
    id?: string,
    version?: number,
    name: string,
    clientId: string,
    clientSecret: string,
    authorizationUrl: string,
    tokenUrl: string,
    userInfoUrl: string,
    userIdAttribute: string,
    scopes: string,
  };

  const formValues = ref<OAuthProviderFormValues>({
    id: props.id,
    name: '',
    clientId: '',
    clientSecret: '',
    authorizationUrl: '',
    tokenUrl: '',
    userInfoUrl: '',
    userIdAttribute: 'sub',
    scopes: 'openid',
  });

  const oidcDiscoveryFormValues = ref({ baseUrl: '' });

  const discoverOidcProvider = async () => {
    try {
      const configuration = await discoverOidcProviderQuery({
        baseUrl: oidcDiscoveryFormValues.value.baseUrl,
      });
      formValues.value = {
        ...formValues.value,
        authorizationUrl: configuration.authorizationUrl,
        tokenUrl: configuration.tokenUrl,
        userInfoUrl: configuration.userInfoUrl,
        userIdAttribute: configuration.userIdAttribute,
        scopes: configuration.scopes.join(' '),
      };
    } catch (e: unknown) {
      const errorCode = handleGqlApiBusinessError<DiscoverOidcProviderConfigurationErrorCodes>(e);
      if (errorCode === DiscoverOidcProviderConfigurationErrorCodes.DiscoveryFailed) {
        throw new ClientSideValidationError([{
          field: 'baseUrl',
          message: $t.value.editOAuthProvider.oidcDiscovery.baseUrl.errors.discoveryFailed(),
        }]);
      }
      throw e;
    }
  };

  const loadProvider = editMode.value ? async () => {
    const provider = await getProviderQuery({ providerId: props.id! });
    formValues.value = {
      ...formValues.value,
      id: provider.id,
      version: provider.version,
      name: provider.name,
      clientId: provider.clientId,
      clientSecret: '',
      authorizationUrl: provider.authorizationUrl,
      tokenUrl: provider.tokenUrl,
      userInfoUrl: provider.userInfoUrl,
      userIdAttribute: provider.userIdAttribute,
      scopes: provider.scopes.join(' '),
    };
  } : undefined;

  // any whitespace separates the scopes, so that values pasted from a provider documentation
  // do not end up as a single malformed scope
  const parsedScopes = () => formValues.value.scopes
    .split(/\s+/)
    .filter((scope) => scope.length > 0);

  const saveProvider = async () => {
    try {
      if (editMode.value) {
        await editProviderMutation({
          id: formValues.value.id!,
          version: formValues.value.version!,
          name: formValues.value.name,
          clientId: formValues.value.clientId,
          clientSecret: formValues.value.clientSecret ? formValues.value.clientSecret : null,
          authorizationUrl: formValues.value.authorizationUrl,
          tokenUrl: formValues.value.tokenUrl,
          userInfoUrl: formValues.value.userInfoUrl,
          userIdAttribute: formValues.value.userIdAttribute,
          scopes: parsedScopes(),
        });
        await navigateToProvidersOverview();
      } else {
        const createdProvider = await createProviderMutation({
          name: formValues.value.name,
          clientId: formValues.value.clientId,
          clientSecret: formValues.value.clientSecret,
          authorizationUrl: formValues.value.authorizationUrl,
          tokenUrl: formValues.value.tokenUrl,
          userInfoUrl: formValues.value.userInfoUrl,
          userIdAttribute: formValues.value.userIdAttribute,
          scopes: parsedScopes(),
        });
        await navigateToEditProvider(createdProvider.id);
      }
      showSuccessNotification($t.value.editOAuthProvider.successNotification(formValues.value.name));
    } catch (e: unknown) {
      const errorCode = handleGqlApiBusinessError<CreateOAuthProviderErrorCodes | EditOAuthProviderErrorCodes>(e);
      if (errorCode === CreateOAuthProviderErrorCodes.ProviderAlreadyExists
        || errorCode === EditOAuthProviderErrorCodes.ProviderAlreadyExists) {
        throw new ClientSideValidationError([{
          field: 'name',
          message: $t.value.editOAuthProvider.form.name.errors.providerAlreadyExists(formValues.value.name),
        }]);
      }
      if (errorCode === CreateOAuthProviderErrorCodes.InvalidScope
        || errorCode === EditOAuthProviderErrorCodes.InvalidScope) {
        throw new ClientSideValidationError([{
          field: 'scopes',
          message: $t.value.editOAuthProvider.form.scopes.errors.invalidScope(),
        }]);
      }
      if (errorCode === EditOAuthProviderErrorCodes.UserIdAttributeLocked) {
        throw new ClientSideValidationError([{
          field: 'userIdAttribute',
          message: $t.value.editOAuthProvider.form.userIdAttribute.errors.locked(),
        }]);
      }
      throw e;
    }
  };

  const pageHeader = computed(() => (editMode.value
    ? $t.value.editOAuthProvider.pageHeader.edit()
    : $t.value.editOAuthProvider.pageHeader.create()));

  const copyCallbackUrl = () => {
    copy(callbackUrl.value);
    showSuccessNotification($t.value.editOAuthProvider.form.callbackUrl.copied());
  };
</script>
