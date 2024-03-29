// noinspection JSUnusedGlobalSymbols

import Login from '@/pages/login/Login.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Pages/Login',
};

export const Default = defineStory(() => ({
  components: { Login },
  template: '<Login/>',
}), {
  fullScreen: true,
  screenshotPreparation: waitForText('Remember me for 30 days'),
});
