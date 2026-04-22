<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm v-model="formValues" :on-submit="saveTax" :on-load="loadTax" :on-cancel="navigateToTaxesOverview">
      <SaFormInput
        prop="title"
        :label="$t.editGeneralTax.form.title.label()"
        :placeholder="$t.editGeneralTax.form.title.placeholder()"
      />
      <SaFormInput
        prop="description"
        :label="$t.editGeneralTax.form.description.label()"
        :placeholder="$t.editGeneralTax.form.description.placeholder()"
      />
      <!--todo #79: input in bps-->
      <SaFormNumberInput
        prop="rateInBps"
        :label="$t.editGeneralTax.form.rate.label()"
        :placeholder="$t.editGeneralTax.form.rate.placeholder()"
      />
    </SaForm>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaFormNumberInput from '@/components/form/SaFormNumberInput.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { useMutation, useLazyQuery } from '@/services/api/use-gql-api.ts';
  import { $t } from '@/services/i18n';

  const props = defineProps<{
    id?: number,
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToTaxesOverview = async () => navigateByViewName('general-taxes-overview');

  type TaxFormValues = {
    title: string,
    description: string | null,
    rateInBps: number | null,
  };

  const formValues = ref<TaxFormValues>({
    title: '',
    description: null,
    rateInBps: null,
  });

  const { currentWorkspaceId } = useCurrentWorkspace();

  const getGeneralTaxQuery = useLazyQuery(graphql(`
    query getGeneralTaxForEdit($workspaceId: Long!, $taxId: Long!) {
      workspace(id: $workspaceId) {
        generalTax(id: $taxId) {
          id
          title
          description
          rateInBps
        }
      }
    }
  `), 'workspace');

  const loadTax = props.id !== undefined ? async () => {
    const workspace = await getGeneralTaxQuery({
      workspaceId: currentWorkspaceId,
      taxId: props.id!,
    });
    const loaded = workspace?.generalTax;
    if (loaded) {
      formValues.value = {
        title: loaded.title,
        description: loaded.description ?? null,
        rateInBps: loaded.rateInBps,
      };
    }
  } : undefined;

  const createGeneralTaxMutation = useMutation(graphql(`
    mutation createGeneralTaxMutation(
      $workspaceId: Long!,
      $title: String!,
      $description: String,
      $rateInBps: Int!
    ) {
      createGeneralTax(
        workspaceId: $workspaceId,
        title: $title,
        description: $description,
        rateInBps: $rateInBps
      ) {
        id
      }
    }
  `), 'createGeneralTax');

  const editGeneralTaxMutation = useMutation(graphql(`
    mutation editGeneralTaxMutation(
      $workspaceId: Long!,
      $id: Long!,
      $title: String!,
      $description: String,
      $rateInBps: Int!
    ) {
      editGeneralTax(
        workspaceId: $workspaceId,
        id: $id,
        title: $title,
        description: $description,
        rateInBps: $rateInBps
      ) {
        id
      }
    }
  `), 'editGeneralTax');

  const saveTax = async () => {
    if (props.id === undefined) {
      await createGeneralTaxMutation({
        workspaceId: currentWorkspaceId,
        title: formValues.value.title,
        description: formValues.value.description || null,
        rateInBps: formValues.value.rateInBps!,
      });
    } else {
      await editGeneralTaxMutation({
        workspaceId: currentWorkspaceId,
        id: props.id,
        title: formValues.value.title,
        description: formValues.value.description || null,
        rateInBps: formValues.value.rateInBps!,
      });
    }
    await navigateToTaxesOverview();
  };

  const pageHeader = computed(() => props.id !== undefined
    ? $t.value.editGeneralTax.pageHeader.edit()
    : $t.value.editGeneralTax.pageHeader.create());
</script>
