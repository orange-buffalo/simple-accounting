<template>
  <SaEntitySelect
    #default="{ entity: invoice }"
    v-bind="props"
    entity-path="invoices"
    :placeholder="$t.saInvoiceSelect.placeholder()"
    :label-provider="invoiceLabelProvider"
    class="sa-invoice-select"
    v-on="listeners"
  >
    <div class="sa-invoice-select__option">
      <div class="sa-invoice-select__option__title">
        {{ invoice.title }}
      </div>
      <div class="sa-invoice-select__option__date">
        <SaIcon icon="calendar" />
        {{ $t.common.date.medium(invoice.dateIssued) }}
      </div>
      <MoneyOutput
        :amount="invoice.amount"
        :currency="invoice.currency"
        class="sa-invoice-select__option__amount"
      />
    </div>
  </SaEntitySelect>
</template>

<script>
  import SaEntitySelect from '@/components/SaEntitySelect';
  import SaIcon from '@/components/SaIcon';
  import MoneyOutput from '@/components/MoneyOutput';

  function invoiceLabelProvider(invoice) {
    return invoice.title;
  }

  export default {
    components: {
      MoneyOutput,
      SaIcon,
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

    &__option {
      display: flex;
      justify-content: space-between;
      flex-wrap: wrap;
      padding-bottom: 10px;

      &__title {
        width: 100%;
      }

      &__date, &__amount {
        font-size: 90%;
        line-height: 1em;
        color: $secondary-color;
      }

      &__date {
        .sa-icon {
          width: 14px;
          height: 14px;
        }
      }
    }
  }
</style>
