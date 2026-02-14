<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaLegacyForm
      ref="formRef"
      :model="customer"
      :rules="customerValidationRules"
    >
      <template #default>
        <h2>General Information</h2>

        <ElFormItem
          label="Name"
          prop="name"
        >
          <ElInput
            v-model="customer.name"
            placeholder="Provide a name of the customer"
          />
        </ElFormItem>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToCustomersOverview">
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
import type { EditCustomerDto } from '@/services/api';
import { customersApi } from '@/services/api';
import useNavigation from '@/services/use-navigation';
import type { PartialBy } from '@/services/utils';
import { useCurrentWorkspace } from '@/services/workspaces';

const props = defineProps<{
  id?: number;
}>();

const customerValidationRules = {
  name: {
    required: true,
    message: 'Please provide a name',
  },
};

const { navigateByViewName } = useNavigation();
const navigateToCustomersOverview = async () => navigateByViewName('customers-overview');

type CustomerFormValues = PartialBy<EditCustomerDto, 'name'>;
const customer = ref<CustomerFormValues>({});

const { currentWorkspaceId } = useCurrentWorkspace();

const loadCustomer = async () => {
  if (props.id !== undefined) {
    customer.value = await customersApi.getCustomer({
      customerId: props.id,
      workspaceId: currentWorkspaceId,
    });
  }
};

const saveCustomer = async () => {
  if (props.id) {
    await customersApi.updateCustomer({
      customerId: props.id,
      editCustomerDto: customer.value as EditCustomerDto,
      workspaceId: currentWorkspaceId,
    });
  } else {
    await customersApi.createCustomer({
      editCustomerDto: customer.value as EditCustomerDto,
      workspaceId: currentWorkspaceId,
    });
  }
  await navigateToCustomersOverview();
};

const { submitForm, formRef } = useForm(loadCustomer, saveCustomer);

const pageHeader = props.id ? 'Edit Customer' : 'Create New Customer';
</script>
