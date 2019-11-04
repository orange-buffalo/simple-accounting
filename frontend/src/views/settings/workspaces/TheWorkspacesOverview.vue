<template>
  <div>
    <div class="sa-page-header">
      <h1>Workspaces</h1>

      <div class="sa-header-options">
        <span />

        <ElButton
          round
          @click="navigateToCreateWorkspace"
        >
          <Svgicon name="plus-thin" />
          Create new workspace
        </ElButton>
      </div>
    </div>

    <h2>Current Workspace</h2>
    <TheWorkspacesOverviewItemPanel :workspace="currentWorkspace" />

    <template v-if="hasOtherOwnWorkspaces">
      <h2>My Other Workspaces</h2>
      <TheWorkspacesOverviewItemPanel
        v-for="workspace in ownOtherWorkspaces"
        :key="workspace.id"
        :workspace="workspace"
      />
    </template>

    <template v-if="hasSharedWorkspaces">
      <h2>Workspaces Shared With Me</h2>
      <TheWorkspacesOverviewItemPanel
        v-for="workspace in sharedWorkspaces"
        :key="workspace.id"
        :workspace="workspace"
      />
    </template>
  </div>
</template>

<script>
  import '@/components/icons/plus-thin';
  import { withWorkspaces } from '@/components/mixins/with-workspaces';
  import TheWorkspacesOverviewItemPanel from './TheWorkspacesOverviewItemPanel';
  import { api } from '@/services/api';

  export default {
    name: 'TheWorkspacesOverview',

    components: {
      TheWorkspacesOverviewItemPanel,
    },

    mixins: [withWorkspaces],

    data() {
      return {
        sharedWorkspaces: [],
      };
    },

    computed: {
      ownOtherWorkspaces() {
        return this.workspaces.filter(it => it.id !== this.currentWorkspace.id);
      },

      hasOtherOwnWorkspaces() {
        return this.ownOtherWorkspaces.length;
      },

      hasSharedWorkspaces() {
        return this.sharedWorkspaces.length;
      },
    },

    async created() {
      const sharedWorkspacesResponse = await api.get('/shared-workspaces');
      this.sharedWorkspaces = sharedWorkspacesResponse.data;
    },

    methods: {
      navigateToCreateWorkspace() {
        this.$router.push({ name: 'create-new-workspace' });
      },
    },
  };
</script>
