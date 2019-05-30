<template>
  <div>
    <div class="sa-page-header">
      <h1>{{pageHeader}}</h1>
    </div>

    <div class="sa-form">
      <el-form :model="workspaceForm"
               ref="workspaceForm"
               :rules="workspaceValidationRules">

        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>General Information</h2>
            <el-form-item label="Workspace Name" prop="name">
              <el-input v-model="workspaceForm.name"></el-input>
            </el-form-item>

            <el-form-item label="Default Currency" prop="defaultCurrency">
              <currency-input v-model="workspaceForm.defaultCurrency"
                              :disabled="isEditing"/>
            </el-form-item>
          </div>
        </div>

        <hr/>

        <div class="sa-buttons-bar">
          <el-button @click="navigateToWorkspacesOverview">Cancel</el-button>
          <el-button type="primary" @click="saveWorkspace">Save</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
  import api from '@/services/api'
  import {assign, isNil} from 'lodash'
  import withWorkspaces from '@/app/components/mixins/with-workspaces'
  import CurrencyInput from '@/app/components/CurrencyInput'

  export default {
    name: 'TheWorkspaceEditor',

    mixins: [withWorkspaces],

    components: {
      CurrencyInput
    },

    data: function () {
      return {
        workspaceForm: {},
        workspaceValidationRules: {
          name: [
            {required: true, message: 'Please provide the name'},
            {max: 255, message: 'Name is too long'}
          ],
          defaultCurrency: {required: true, message: 'Please select a default currency'}
        }
      }
    },

    created: async function () {
      if (this.isEditing) {
        let workspace = this.workspaces.find(it => it.id === this.$route.params.id);
        this.workspaceForm = assign({}, workspace)
      } else {
        this.workspaceForm = {}
      }
    },

    computed: {
      isEditing: function () {
        return !isNil(this.$route.params.id)
      },

      pageHeader: function () {
        return this.isEditing ? 'Edit Workspace' : 'Create New Workspace'
      }
    },

    methods: {
      navigateToWorkspacesOverview: function () {
        this.$router.push({name: 'workspaces-overview'})
      },

      saveWorkspace: async function () {
        try {
          await this.$refs.workspaceForm.validate();
        } catch (e) {
          return
        }

        if (this.workspaceForm.id) {
          await api.put(`/user/workspaces/${this.workspaceForm.id}`, {
            name: this.workspaceForm.name
          })
        } else {
          await api.post(`/user/workspaces`, {
            name: this.workspaceForm.name,
            defaultCurrency: this.workspaceForm.defaultCurrency
          })
        }

        // todo #90: when categories are removed from workspace, just use the reply to update current workspace
        this.$store.dispatch('workspaces/loadWorkspaces')

        this.navigateToWorkspacesOverview()
      }
    }
  }
</script>