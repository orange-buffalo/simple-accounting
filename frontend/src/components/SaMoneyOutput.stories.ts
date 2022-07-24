import SaMoneyOutput from '@/components/SaMoneyOutput.vue';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components/SaMoneyOutput',
};

// noinspection JSUnusedGlobalSymbols
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
