<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      ref="customerForm"
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
    name: 'EditCustomer',

    components: {
      SaForm,
    },

    mixins: [withWorkspaces],

    data() {
      return {
        customer: {
          name: null,
        },
        customerValidationRules: {
          name: {
            required: true,
            message: 'Please select a name',
          },
        },
      };
    },

    computed: {
      pageHeader() {
        return this.$route.params.id ? 'Edit Customer' : 'Create New Customer';
      },
    },

    async created() {
      if (this.$route.params.id) {
        const incomeResponse = await api
          .get(`/workspaces/${this.currentWorkspace.id}/customers/${this.$route.params.id}`);
        this.customer = { ...incomeResponse.data };
      }
    },

    methods: {
      navigateToCustomersOverview() {
        this.$router.push({ name: 'customers-overview' });
      },

      async save() {
        try {
          await this.$refs.customerForm.validate();
        } catch (e) {
          return;
        }

        const customerToPush = {
          name: this.customer.name,
        };

        if (this.customer.id) {
          await api.put(`/workspaces/${this.currentWorkspace.id}/customers/${this.customer.id}`, customerToPush);
        } else {
          await api.post(`/workspaces/${this.currentWorkspace.id}/customers`, customerToPush);
        }
        await this.$router.push({ name: 'customers-overview' });
      },
    },
  };
</script>
