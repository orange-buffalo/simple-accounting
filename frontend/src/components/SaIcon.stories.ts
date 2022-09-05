// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import SaIcon from '@/components/SaIcon.vue';
import { iconNames } from '@/icons';
import { defineStory } from '@/__storybook__/sa-storybook';
import { disableIconsSvgAnimations } from '@/__storybook__/screenshots';

export default {
  title: 'Components/SaIcon',
};

export const Default = defineStory(() => ({
  components: { SaIcon },
  methods: {
    onClick() {
      action('on-click')();
    },
  },
  data() {
    return {
      supportedIcons: iconNames(),
      configs: [{
        text: 'Default',
        style: {
          'margin-right': '10px',
          'margin-top': '10px',
        },
      },
      {
        text: 'Default size, custom color',
        style: {
          'margin-right': '10px',
          'margin-top': '10px',
          color: '#3fa7ad',
        },
      },
      {
        text: 'Size 30px',
        size: 30,
        style: {
          'margin-right': '15px',
          'margin-top': '15px',
        },
      },
      {
        text: 'Size 50px',
        size: 50,
        style: {
          'margin-right': '25px',
          'margin-top': '25px',
        },
      }],
    };
  },
  template: `
    <div>
    <template v-for="config in configs" :key="config.text">
      <h3 style="margin-bottom: 0">{{ config.text }}</h3>
      <SaIcon :style="config.style"
              :size="config.size"
              v-for="supportedIcon in supportedIcons"
              :key="supportedIcon"
              :icon="supportedIcon"
              :title="supportedIcon"
              @click="onClick"
      />
    </template>
    </div>`,
}), {
  screenshotPreparation: disableIconsSvgAnimations(),
});
