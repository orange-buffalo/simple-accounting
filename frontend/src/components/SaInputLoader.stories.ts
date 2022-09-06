// noinspection JSUnusedGlobalSymbols

import { ElInput } from 'element-plus';
import SaInputLoader from '@/components/SaInputLoader.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, waitForElementToBeVisible, waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/SaInputLoader',
};

export const Default = defineStory(() => ({
  components: { SaInputLoader, ElInput },
  created() {
    this.loading = false;
  },
  template: `
    <h4>Loading</h4>
    <SaInputLoader loading style="width: 300px">
    <ElInput />
    </SaInputLoader>

    <h4>Error</h4>
    <SaInputLoader :loading="false" error style="width: 300px">
    <ElInput />
    </SaInputLoader>

    <h4>Loaded</h4>
    <SaInputLoader :loading="false" style="width: 300px">
    <ElInput />
    </SaInputLoader>
  `,
}), {
  screenshotPreparation: allOf(
    waitForText('An error happened'),
    waitForElementToBeVisible('.el-input'),
  ),
});
