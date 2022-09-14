// noinspection JSUnusedGlobalSymbols

import SaMoneyInput from '@/components/SaMoneyInput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/SaMoneyInput',
};

export const Default = defineStory(() => ({
  components: { SaMoneyInput },
  data: () => ({
    audValue: 783,
    yenValue: undefined,
    kwdValue: 0,
  }),
  template: `
    <h4>With initial value</h4>
    <SaMoneyInput v-model="audValue" currency="AUD" />
    <br />{{audValue}}
    <br />
    <h4>No initial value</h4>
    <SaMoneyInput v-model="yenValue" currency="JPY" />
    <br />{{yenValue}}
    <h4>Currency 3 digits</h4>
    <SaMoneyInput v-model="kwdValue" currency="KWD" />
    <br /><span id="kwdValue">{{ kwdValue }}</span>
  `,
}), {
  screenshotPreparation: waitForText('0', '#kwdValue'),
});
