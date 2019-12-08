<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      ref="taxForm"
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
          @click="save"
        >
          Save
        </ElButton>
      </template>
    </SaForm>
  </div>
</template>

<script>
  import { api } from '@/services/api';
  import SaForm from '@/components/SaForm';
  import withWorkspaces from '@/components/mixins/with-workspaces';

  export default {
    name: 'EditGeneralTax',

    components: {
      SaForm,
    },

    mixins: [withWorkspaces],

    data() {
      return {
        tax: {
          name: null,
        },
        taxValidationRules: {
          title: {
            required: true,
            message: 'Please provide a title',
          },
          rateInBps: {
            required: true,
            message: 'Please provide the rate',
          },
        },
      };
    },

    computed: {
      pageHeader() {
        return this.$route.params.id ? 'Edit General Tax' : 'Create New General Tax';
      },
    },

    async created() {
      if (this.$route.params.id) {
        const taxResponse = await api
          .get(`/workspaces/${this.currentWorkspace.id}/general-taxes/${this.$route.params.id}`);
        this.tax = { ...taxResponse.data };
      }
    },

    methods: {
      navigateToTaxesOverview() {
        this.$router.push({ name: 'general-taxes-overview' });
      },

      async save() {
        try {
          await this.$refs.taxForm.validate();
        } catch (e) {
          return;
        }

        const taxToPush = {
          title: this.tax.title,
          description: this.tax.description,
          rateInBps: this.tax.rateInBps,
        };

        if (this.tax.id) {
          await api.put(`/workspaces/${this.currentWorkspace.id}/general-taxes/${this.tax.id}`, taxToPush);
        } else {
          await api.post(`/workspaces/${this.currentWorkspace.id}/general-taxes`, taxToPush);
        }
        await this.$router.push({ name: 'general-taxes-overview' });
      },
    },
  };
</script>
