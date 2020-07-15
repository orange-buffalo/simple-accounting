<template>
  <SaEntitySelect
    v-bind="props"
    entity-path="invoices"
    :placeholder="$t('saInvoiceSelect.placeholder')"
    :label-provider="invoiceLabelProvider"
    class="sa-invoice-select"
    v-on="listeners"
  />
</template>

<script>
  import SaEntitySelect from '@/components/SaEntitySelect';

  function invoiceLabelProvider(invoice) {
    return invoice.title;
  }

  export default {
    components: {
      SaEntitySelect,
    },

    props: {
      value: {
        type: Number,
        default: null,
      },
    },

    setup(props, { listeners }) {
      return {
        props,
        listeners,
        invoiceLabelProvider,
      };
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";

  .sa-invoice-select {
    min-width: 400px;
  }
</style>
