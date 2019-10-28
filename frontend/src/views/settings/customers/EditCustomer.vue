<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <div class="sa-form">
      <el-form
        ref="customerForm"
        :model="customer"
        label-position="right"
        label-width="200px"
        :rules="customerValidationRules"
      >
        <h2>General Information</h2>

        <el-form-item
          label="Name"
          prop="name"
        >
          <el-input
            v-model="customer.name"
            placeholder="Provide a name of the customer"
          />
        </el-form-item>

        <hr>

        <div class="sa-buttons-bar">
          <el-button @click="navigateToCustomersOverview">
            Cancel
          </el-button>
          <el-button
            type="primary"
            @click="save"
          >
            Save
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import { assign } from 'lodash';
import api from '@/services/api';

export default {
  name: 'EditCustomer',

  components: {
  },

  data() {
    return {
      customer: {
        name: null,
      },
      customerValidationRules: {
        name: { required: true, message: 'Please select a name' },
      },
    };
  },

  async created() {
    if (this.$route.params.id) {
      const incomeResponse = await api.get(`/workspaces/${this.workspace.id}/customers/${this.$route.params.id}`);
      this.customer = assign({}, this.customer, incomeResponse.data);
    }
  },

  computed: {
    ...mapState('workspaces', {
      workspace: 'currentWorkspace',
    }),

    pageHeader() {
      return this.$route.params.id ? 'Edit Customer' : 'Create New Customer';
    },
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
        await api.put(`/workspaces/${this.workspace.id}/customers/${this.customer.id}`, customerToPush);
      } else {
        await api.post(`/workspaces/${this.workspace.id}/customers`, customerToPush);
      }
      this.$router.push({ name: 'customers-overview' });
    },
  },
};
</script>
