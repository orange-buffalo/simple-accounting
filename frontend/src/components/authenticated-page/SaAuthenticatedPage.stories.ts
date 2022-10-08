// noinspection JSUnusedGlobalSymbols

import SaAuthenticatedPage from '@/components/authenticated-page/SaAuthenticatedPage.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/PageTemplates/SaAuthenticatedPage',
};

export const Default = defineStory(() => ({
  components: { SaAuthenticatedPage },
  template: `
    <component is="style">
    .el-container, .el-container .el-container { height: 900px; }
    </component>
    <SaAuthenticatedPage />
  `,
}), {
  workspace: {
    name: 'Workspace',
  },
  fullScreen: true,
  screenshotPreparation: waitForText('Workspace'),
});
