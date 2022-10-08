// noinspection JSUnusedGlobalSymbols

import SaMoneyOutput from '@/components/SaMoneyOutput.vue';

export default {
  title: 'Components/Basic/SaMoneyOutput',
};

export const Default = () => ({
  components: { SaMoneyOutput },
  template: `
    <div>
    With value:
    <SaMoneyOutput :amount-in-cents="12345" currency="AUD" />
    <br />
    No value:
    <SaMoneyOutput currency="AUD" />
    </div>
  `,
});
