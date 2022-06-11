import { action } from '@storybook/addon-actions';
import SaIcon from '@/components/SaIcon.vue';
import { iconNames } from '@/icons';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components/SaIcon',
};

// noinspection JSUnusedGlobalSymbols
export const Default = () => ({
  components: { SaIcon },
  methods: {
    onClick() {
      action('on-click')();
    },
  },
  template: '<SaIcon icon="hourglass" @click="onClick" />',
});

// noinspection JSUnusedGlobalSymbols
export const AllIcons = () => ({
  components: { SaIcon },
  data() {
    return {
      supportedIcons: iconNames(),
    };
  },
  template: `<div>
      <SaIcon style="width: 30px; margin-right: 10px; margin-top: 10px;"
              v-for="supportedIcon in supportedIcons"
              :key="supportedIcon"
              :icon="supportedIcon" />
      </div>`,
});
