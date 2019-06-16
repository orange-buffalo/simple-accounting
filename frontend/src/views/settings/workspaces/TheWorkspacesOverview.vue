<template>
  <div>
    <div class="sa-page-header">
      <h1>Workspaces</h1>

      <div class="sa-header-options">
        <span></span>

        <el-button round
                   @click="navigateToCreateWorkspace">
          <svgicon name="plus-thin"/>
          Create new workspace
        </el-button>
      </div>
    </div>

    <h2>Current Workspace</h2>
    <the-workspaces-overview-item-panel :workspace="currentWorkspace"/>

    <h2>My Other Workspaces</h2>
    <the-workspaces-overview-item-panel
        v-for="workspace in ownOtherWorkspaces"
        :key="workspace.id"
        :workspace="workspace"/>
  </div>
</template>

<script>
  import '@/components/icons/plus-thin'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'
  import TheWorkspacesOverviewItemPanel from './TheWorkspacesOverviewItemPanel'

  export default {
    name: 'TheWorkspacesOverview',

    mixins: [withWorkspaces],

    components: {
      TheWorkspacesOverviewItemPanel
    },

    data: function () {
      return {}
    },

    computed: {
      ownOtherWorkspaces: function () {
        return this.workspaces.filter(it => it.id !== this.currentWorkspace.id);
      }
    },

    methods: {
      navigateToCreateWorkspace: function () {
        this.$router.push({name: 'create-new-workspace'})
      }
    }
  }
</script>