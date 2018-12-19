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
                     @click="navigateToWorkspaceSetup">
            <svgicon name="pencil"/>
            Edit
          </el-button>
        </div>
      </div>

    </el-popover>

  </div>
</template>

<script>
  import {withWorkspaces} from '@/app/components/mixins/with-workspaces'
  import '@/components/icons/gear'
  import '@/components/icons/plus-thin'
  import TheSideMenuLink from '@/app/components/TheSideMenuLink'

  export default {
    name: 'TheWorkspaceSelector',

    mixins: [withWorkspaces],

    components: {
      TheSideMenuLink
    },

    methods: {
      navigateToWorkspaceSetup: function () {
        this.$router.push({name: 'workspace-setup'})
      },

      switchWorkspace: function (ws) {
        // todo do not commit directly, use wrapper action
        this.$store.commit('workspaces/setCurrentWorkspace', ws)
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