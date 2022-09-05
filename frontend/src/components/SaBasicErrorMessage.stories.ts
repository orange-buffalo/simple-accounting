// noinspection JSUnusedGlobalSymbols

import SaBasicErrorMessage from '@/components/SaBasicErrorMessage.vue';

export default {
  title: 'Components/SaBasicErrorMessage',
};

export const Default = () => ({
  components: { SaBasicErrorMessage },
  template: `
    <h4>Default Message</h4>
    <SaBasicErrorMessage />
    <h4>Custom Message</h4>
    <SaBasicErrorMessage message="Custom error happened" />
  `,
});
