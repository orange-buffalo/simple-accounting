<template>
  <SaPageWithoutSideMenu>
    <SaForm
      v-model="form"
      :on-submit="save"
      :submit-button-label="$t.accountSetup.submitButton()"
    >
      <div class="account-setup-page__welcome-message">
        <p>{{ $t.accountSetup.welcomeMessage() }}</p>
      </div>

      <SaFormInput
        :label="$t.accountSetup.workspaceNameLabel()"
        :placeholder="$t.accountSetup.workspaceNamePlaceholder()"
        prop="name"
      />
      <SaFormInput
        :label="$t.accountSetup.defaultCurrencyLabel()"
        :placeholder="$t.accountSetup.defaultCurrencyPlaceholder()"
        prop="defaultCurrency"
      />
    </SaForm>
  </SaPageWithoutSideMenu>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { $t } from '@/services/i18n';
  import useNavigation from '@/services/use-navigation';
  import SaPageWithoutSideMenu from '@/components/page-without-side-menu/SaPageWithoutSideMenu.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaForm from '@/components/form/SaForm.vue';
  import { useWorkspaces } from '@/services/workspaces.ts';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api';

  interface CreateWorkspaceForm {
    name: string;
    defaultCurrency: string;
  }

  const form = ref<CreateWorkspaceForm>({
    name: '',
    defaultCurrency: 'AUD',
  });

  const createWorkspaceMutation = graphql(`
    mutation createWorkspaceAccountSetup($name: String!, $defaultCurrency: String!) {
      createWorkspace(name: $name, defaultCurrency: $defaultCurrency) {
        id
        name
        defaultCurrency
      }
    }
  `);

  const executeCreate = useMutation(createWorkspaceMutation, 'createWorkspace');

  const navigation = useNavigation();
  const save = async () => {
    await executeCreate({ name: form.value.name, defaultCurrency: form.value.defaultCurrency });
    await useWorkspaces().loadWorkspaces();
    await navigation.navigateByPath('/');
  };
</script>
