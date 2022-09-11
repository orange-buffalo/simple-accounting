// noinspection JSUnusedGlobalSymbols

import { ElButton } from 'element-plus';
import { ref } from 'vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { useConfirmation } from '@/components/confirmation/use-confirmation';
import { allOf, clickOnElement, waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/Confirmation',
};

export const Default = defineStory(() => ({
  components: {
    ElButton,
  },
  setup() {
    const data = ref('Initial');
    const executeAction = useConfirmation(
      'Are you sure?',
      {
        title: 'Confirmation',
        type: 'warning',
      },
      async () => {
        data.value = 'Confirmed';
      },
    );
    return {
      executeAction,
      data,
    };
  },
  template: `
    <ElButton @click="executeAction">Execute</ElButton>
    <br />
    <br />
    {{data}}
  `,
}), {
  screenshotPreparation: allOf(
    clickOnElement('.el-button'),
    waitForText('Are you sure?'),
  ),
});
