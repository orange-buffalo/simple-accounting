<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <div class="sa-form">
      <el-form
        ref="taxForm"
        :model="tax"
        label-position="right"
        label-width="200px"
        :rules="taxValidationRules"
      >
        <h2>General Information</h2>

        <el-form-item
          label="Title"
          prop="title"
        >
          <el-input
            v-model="tax.title"
            placeholder="Provide a title of the tax"
          />
        </el-form-item>

        <el-form-item
          label="Description"
          prop="description"
        >
          <el-input
            v-model="tax.description"
            placeholder="Short description of a tax"
          />
        </el-form-item>

        <!--todo #79: input in bps-->
        <el-form-item
          label="Rate"
          prop="rateInBps"
        >
          <el-input
            v-model="tax.rateInBps"
            placeholder="Provide a rate for this tax"
          />
        </el-form-item>

        <hr>

        <div class="sa-buttons-bar">
          <el-button @click="navigateToTaxesOverview">
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
  name: 'EditGeneralTax',

  components: {
  },

  data() {
    return {
      tax: {
        name: null,
      },
      taxValidationRules: {
        title: { required: true, message: 'Please provide a title' },
        rateInBps: { required: true, message: 'Please provide the rate' },
      },
    };
  },

  computed: {
    ...mapState('workspaces', {
      workspace: 'currentWorkspace',
    }),

    pageHeader() {
      return this.$route.params.id ? 'Edit General Tax' : 'Create New General Tax';
    },
  },

  async created() {
    if (this.$route.params.id) {
      const taxResponse = await api.get(`/workspaces/${this.workspace.id}/general-taxes/${this.$route.params.id}`);
      this.tax = assign({}, this.tax, taxResponse.data);
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
        await api.put(`/workspaces/${this.workspace.id}/general-taxes/${this.tax.id}`, taxToPush);
      } else {
        await api.post(`/workspaces/${this.workspace.id}/general-taxes`, taxToPush);
      }
      await this.$router.push({ name: 'general-taxes-overview' });
    },
  },
};
</script>
