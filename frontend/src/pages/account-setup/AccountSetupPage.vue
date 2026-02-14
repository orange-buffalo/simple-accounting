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
import SaForm from '@/components/form/SaForm.vue';
import SaFormInput from '@/components/form/SaFormInput.vue';
import SaPageWithoutSideMenu from '@/components/page-without-side-menu/SaPageWithoutSideMenu.vue';
import { CreateWorkspaceDto, workspacesApi } from '@/services/api';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';
import { useWorkspaces } from '@/services/workspaces.ts';

const form = ref<CreateWorkspaceDto>({
  name: '',
  defaultCurrency: 'AUD',
});

const navigation = useNavigation();
const save = async () => {
  await workspacesApi.createWorkspace({
    createWorkspaceDto: form.value,
  });
  await useWorkspaces().loadWorkspaces();
  await navigation.navigateByPath('/');
};
</script>
