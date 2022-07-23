import AuthenticatedPage from '@/components/authenticated-page/AuthenticatedPage.vue';
import { useWorkspaces } from '@/services/workspaces';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components/AuthenticatedPage',
};

// noinspection JSUnusedGlobalSymbols
export const Default = () => ({
  beforeMount: () => {
    const { setCurrentWorkspace } = useWorkspaces();
    setCurrentWorkspace({
      id: 42,
      defaultCurrency: 'AUD',
      editable: true,
      multiCurrencyEnabled: true,
      name: 'Workspace',
      taxEnabled: true,
      version: 1,
    });
  },
  components: { AuthenticatedPage },
  template: `
    <component is="style">
    .el-container, .el-container .el-container { height: auto; }
    </component>
    <AuthenticatedPage />
  `,
});
