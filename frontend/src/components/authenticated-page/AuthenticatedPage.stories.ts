import AuthenticatedPage from '@/components/authenticated-page/AuthenticatedPage.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components/AuthenticatedPage',
};

// noinspection JSUnusedGlobalSymbols
export const Default = defineStory(() => ({
  components: { AuthenticatedPage },
  template: `
    <component is="style">
    .el-container, .el-container .el-container { height: 900px; }
    </component>
    <AuthenticatedPage />
  `,
}), {
  workspace: {
    name: 'Workspace',
  },
  fullScreen: true,
  screenshotPreparation: waitForText('Workspace'),
});
