<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaLegacyForm
      ref="formRef"
      :model="tax"
      :rules="taxValidationRules"
    >
      <template #default>
        <h2>General Information</h2>

        <ElFormItem
          label="Title"
          prop="title"
        >
          <ElInput
            v-model="tax.title"
            placeholder="Provide a title of the tax"
          />
        </ElFormItem>

        <ElFormItem
          label="Description"
          prop="description"
        >
          <ElInput
            v-model="tax.description"
            placeholder="Short description of a tax"
          />
        </ElFormItem>

        <!--todo #79: input in bps-->
        <ElFormItem
          label="Rate"
          prop="rateInBps"
        >
          <ElInput
            v-model="tax.rateInBps"
            placeholder="Provide a rate for this tax"
          />
        </ElFormItem>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToTaxesOverview">
          Cancel
        </ElButton>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          Save
        </ElButton>
      </template>
    </SaLegacyForm>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import SaLegacyForm from '@/components/form/SaLegacyForm.vue';
import { useForm } from '@/components/form/use-form';
import type { EditGeneralTaxDto } from '@/services/api';
import { generalTaxesApi } from '@/services/api';
import useNavigation from '@/services/use-navigation';
import type { PartialBy } from '@/services/utils';
import { useCurrentWorkspace } from '@/services/workspaces';

const props = defineProps<{
  id?: number;
}>();

const { navigateByViewName } = useNavigation();
const navigateToTaxesOverview = () => navigateByViewName('general-taxes-overview');

type TaxFormValues = PartialBy<EditGeneralTaxDto, 'title' | 'rateInBps'>;
const tax = ref<TaxFormValues>({});

const { currentWorkspaceId } = useCurrentWorkspace();
const loadTax = async () => {
  if (props.id !== undefined) {
    tax.value = await generalTaxesApi.getTax({
      taxId: props.id,
      workspaceId: currentWorkspaceId,
    });
  }
};
const saveTax = async () => {
  if (props.id === undefined) {
    await generalTaxesApi.createTax({
      workspaceId: currentWorkspaceId,
      editGeneralTaxDto: tax.value as EditGeneralTaxDto,
    });
  } else {
    await generalTaxesApi.updateTax({
      workspaceId: currentWorkspaceId,
      editGeneralTaxDto: tax.value as EditGeneralTaxDto,
      taxId: props.id,
    });
  }
  await navigateToTaxesOverview();
};

const taxValidationRules = {
  title: {
    required: true,
    message: 'Please provide a title',
  },
  rateInBps: {
    required: true,
    message: 'Please provide the rate',
  },
};

const { formRef, submitForm } = useForm(loadTax, saveTax);

const pageHeader = props.id ? 'Edit General Tax' : 'Create New General Tax';
</script>
