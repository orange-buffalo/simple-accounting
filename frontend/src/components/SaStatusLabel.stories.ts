// noinspection JSUnusedGlobalSymbols

import SaStatusLabel from '@/components/SaStatusLabel.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/SaStatusLabel',
};

export const All = defineStory(() => ({
  components: { SaStatusLabel },
  setup: () => ({
     variants: {
        success: 'Success',
       failure: 'Failure',
       pending: 'Pending',
       regular: 'Regular',
     },
  }),
  template: `
    <h3>Default</h3>
    <span v-for="variant in Object.keys(variants)" :key="variant">
        <SaStatusLabel :status="variant">{{variants[variant]}}</SaStatusLabel>&nbsp;
    </span>
    
    <h3>No icon</h3>
    <span v-for="variant in Object.keys(variants)" :key="variant">
        <SaStatusLabel hide-icon :status="variant">{{variants[variant]}}</SaStatusLabel>&nbsp;
    </span>
    
    <h3>Custom icon</h3>
    <span v-for="variant in Object.keys(variants)" :key="variant">
        <SaStatusLabel custom-icon="draft" :status="variant">{{variants[variant]}}</SaStatusLabel>&nbsp;
    </span>
    
    <h3>Simplified</h3>
    <span v-for="variant in Object.keys(variants)" :key="variant">
        <SaStatusLabel simplified :status="variant">{{variants[variant]}}</SaStatusLabel>&nbsp;
    </span>
    
    <h3>Simplified without icon</h3>
    <span v-for="variant in Object.keys(variants)" :key="variant">
        <SaStatusLabel simplified hide-icon :status="variant">{{variants[variant]}}</SaStatusLabel>&nbsp;
    </span>
  `,
}), {
  screenshotPreparation: waitForText('Success'),
});
