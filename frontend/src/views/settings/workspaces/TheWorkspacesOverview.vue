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

    <template v-if="hasOtherOwnWorkspaces">
      <h2>My Other Workspaces</h2>
      <the-workspaces-overview-item-panel
          v-for="workspace in ownOtherWorkspaces"
          :key="workspace.id"
          :workspace="workspace"/>
    </template>

    <template v-if="hasSharedWorkspaces">
      <h2>Workspaces Shared With Me</h2>
      <the-workspaces-overview-item-panel
          v-for="workspace in sharedWorkspaces"
          :key="workspace.id"
          :workspace="workspace"/>
    </template>
  </div>
</template>

<script>
  import '@/components/icons/plus-thin'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'
  import TheWorkspacesOverviewItemPanel from './TheWorkspacesOverviewItemPanel'
  import {api} from '@/services/api'

  export default {
    name: 'TheWorkspacesOverview',

    mixins: [withWorkspaces],

    components: {
      TheWorkspacesOverviewItemPanel
    },

    data: function () {
      return {
        sharedWorkspaces: []
      }
    },

    created: async function () {
      let sharedWorkspacesResponse = await api.get('/shared-workspaces');
      this.sharedWorkspaces = sharedWorkspacesResponse.data
    },

    computed: {
      ownOtherWorkspaces: function () {
        return this.workspaces.filter(it => it.id !== this.currentWorkspace.id);
      },

      hasOtherOwnWorkspaces: function () {
        return this.ownOtherWorkspaces.length
      },

      hasSharedWorkspaces: function () {
        return this.sharedWorkspaces.length
      }
    },

    methods: {
      navigateToCreateWorkspace: function () {
        this.$router.push({name: 'create-new-workspace'})
      }
    }
  }
</script>