<template>
  <div class="the-workspace-selector">
    {{currentWorkspace.name}}

    <el-popover placement="bottom"
                width="300"
                trigger="click">
      <span slot="reference">
        <svgicon class="the-workspace-selector__trigger" name="gear"/>
      </span>

      <div class="the-workspace-selector__add-new">
        <el-button type="text"
                   @click="navigateToWorkspaceSetup">
          <svgicon name="plus-thin"/>
          New Workspace
        </el-button>
      </div>

      <div v-for="workspace in workspaces">
        <div class="the-workspace-selector__workspace-name">
          {{workspace.name}}
        </div>
        <div class="the-workspace-selector__workspace-controls">
          <!--todo select icon-->
          <el-button type="text"
                     v-if="currentWorkspace.id !== workspace.id"
                     @click="switchWorkspace(workspace)">
            <svgicon name="plus-thin"/>
            Switch
          </el-button>

          <el-button type="text"
                     @click="openEditDialog(workspace)">
            <svgicon name="pencil"/>
            Edit
          </el-button>
        </div>
      </div>

    </el-popover>

    <el-dialog title="Edit Workspace"
               :visible.sync="workspaceEditDialogVisible">
      <el-form :model="workspaceForm"
               ref="workspaceForm"
               :rules="workspaceValidationRules">
        <el-form-item label="Workspace Name" prop="name">
          <el-input v-model="workspaceForm.name"></el-input>
        </el-form-item>

        <el-form-item label="Default Currency" prop="defaultCurrency">
          <el-input v-model="workspaceForm.defaultCurrency"
                    :readonly="true"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
    <el-button @click="workspaceEditDialogVisible = false">Cancel</el-button>
    <el-button type="primary" @click="saveWorkspace">Save</el-button>
  </span>
    </el-dialog>

  </div>
</template>

<script>
  import {withWorkspaces} from '@/app/components/mixins/with-workspaces'
  import '@/components/icons/gear'
  import '@/components/icons/plus-thin'
  import TheSideMenuLink from '@/app/components/TheSideMenuLink'
  import {assign} from 'lodash'
  import api from '@/services/api'

  export default {
    name: 'TheWorkspaceSelector',

    mixins: [withWorkspaces],

    components: {
      TheSideMenuLink
    },

    data: function () {
      return {
        workspaceEditDialogVisible: false,
        workspaceForm: {},
        workspaceValidationRules: {
          name: [
            {required: true, message: 'Please provide the name'},
            {max: 255, message: 'Name is too long'}
          ]
        }
      }
    },

    methods: {
      navigateToWorkspaceSetup: function () {
        this.$router.push({name: 'workspace-setup'})
      },

      switchWorkspace: function (ws) {
        // todo do not commit directly, use wrapper action
        this.$store.commit('workspaces/setCurrentWorkspace', ws)
      },

      openEditDialog: function (workspace) {
        this.workspaceEditDialogVisible = true
        this.workspaceForm = assign({}, workspace)
      },

      saveWorkspace: async function () {
        try {
          await this.$refs.workspaceForm.validate();
        } catch (e) {
          return
        }

        await api.put(`/user/workspaces/${this.workspaceForm.id}`, {
          name: this.workspaceForm.name
        })

        // todo when categories are removed from workspace, just use the reply to update current workspace
        this.$store.dispatch('workspaces/loadWorkspaces')

        this.workspaceEditDialogVisible = false
      }
    }
  }
</script>


<style lang="scss">
  .the-workspace-selector {
    text-align: center;
    margin-bottom: 20px;
  }

  .the-workspace-selector__trigger {
    cursor: pointer;
  }

  .the-workspace-selector__add-new {
    display: flex;
    align-items: center;
    justify-content: flex-end;

    .el-button--text {
      span {
        display: inline-flex;
        align-items: center;

        .svg-icon {
          margin-right: 5px;
        }
      }
    }
  }

  .the-workspace-selector__workspace-name {
    font-weight: bold;
    margin-top: 10px;
  }

  .the-workspace-selector__workspace-controls {
    text-align: right;

    .el-button--text {
      padding: 3px 0;

      span {
        display: inline-flex;
        align-items: center;

        .svg-icon {
          margin-right: 5px;
          height: 14px;
          width: 14px;
        }
      }
    }
  }
</style>