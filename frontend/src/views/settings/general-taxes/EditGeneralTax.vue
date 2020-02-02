<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      ref="form"
      :loading="loading"
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
    </SaForm>
  </div>
</template>

<script>
  import { reactive } from '@vue/composition-api';
  import { api } from '@/services/api';
  import SaForm from '@/components/SaForm';
  import useNavigation from '@/components/navigation/useNavigation';
  import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';
  import { useForm, useLoading } from '@/components/utils/utils';

  function navigateToTaxesOverview() {
    const { navigateByViewName } = useNavigation();
    navigateByViewName('general-taxes-overview');
  }

  function useTaxApi(tax) {
    const { currentWorkspaceApiUrl } = useCurrentWorkspace();
    const { loading, withLoading, withLoadingProducer } = useLoading();

    const saveTax = withLoadingProducer(async () => {
      const taxToPush = {
        title: tax.title,
        description: tax.description,
        rateInBps: tax.rateInBps,
      };

      if (tax.id) {
        await api.put(currentWorkspaceApiUrl(`general-taxes/${tax.id}`), taxToPush);
      } else {
        await api.post(currentWorkspaceApiUrl('general-taxes'), taxToPush);
      }
      await navigateToTaxesOverview();
    });

    if (tax.id) {
      withLoading(() => api.getAndSafeAssign(currentWorkspaceApiUrl(`general-taxes/${tax.id}`), tax));
    }

    return {
      saveTax,
      loading,
    };
  }

  function useTaxForm(saveTax) {
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

    return {
      ...useForm(saveTax),
      taxValidationRules,
    };
  }

  export default {
    components: {
      SaForm,
    },

    props: {
      id: {
        type: Number,
        default: null,
      },
    },

    setup({ id }) {
      const tax = reactive({ id });

      const { loading, saveTax } = useTaxApi(tax);

      const { form, taxValidationRules, submitForm } = useTaxForm(saveTax);

      const pageHeader = id ? 'Edit General Tax' : 'Create New General Tax';

      return {
        loading,
        submitForm,
        tax,
        taxValidationRules,
        form,
        pageHeader,
        navigateToTaxesOverview,
      };
    },
  };
</script>
