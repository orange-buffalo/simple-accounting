<template>
  <SaEntitySelect
    v-slot="{ entity }"
    :options-provider="optionsProvider"
    :option-provider="optionProvider"
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    entity-path="invoices"
    :placeholder="$t.saInvoiceSelect.placeholder()"
    :label-provider="invoiceLabelProvider"
    class="sa-invoice-select"
    clearable
  >
    <div class="sa-invoice-select__option">
      <div class="sa-invoice-select__option__title">
        {{ invoice(entity).title }}
      </div>
      <div class="sa-invoice-select__option__date">
        <SaIcon icon="calendar" />
        {{ $t.common.date.medium(invoice(entity).dateIssued) }}
      </div>
      <SaMoneyOutput
        :amount-in-cents="invoice(entity).amount"
        :currency="invoice(entity).currency"
        class="sa-invoice-select__option__amount"
      />
    </div>
  </SaEntitySelect>
</template>

<script lang="ts" setup>
  import SaEntitySelect from '@/components/entity-select/SaEntitySelect.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import { $t } from '@/services/i18n';
  import type { ApiConnectionRequest, HasOptionalId } from '@/services/api';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';

  defineProps<{
    modelValue?: string,
  }>();

  const emit = defineEmits<{(e: 'update:modelValue', value?: string): void }>();

  type InvoiceItem = HasOptionalId & {
    title: string,
    dateIssued: string,
    amount: number,
    currency: string,
  };

  const invoice = (entity: HasOptionalId) => entity as InvoiceItem;
  const invoiceLabelProvider = (entity: HasOptionalId) => invoice(entity).title;

  const { currentWorkspaceId } = useCurrentWorkspace();

  const getInvoicesQuery = useLazyQuery(graphql(`
    query getInvoicesForSelect($workspaceId: String!, $first: Int!, $freeSearchText: String) {
      workspace(id: $workspaceId) {
        invoices(first: $first, freeSearchText: $freeSearchText) {
          edges {
            node {
              id
              title
              dateIssued
              amount
              currency
            }
          }
          totalCount
        }
      }
    }
  `), 'workspace');

  const getInvoiceQuery = useLazyQuery(graphql(`
    query getInvoiceForSelect($workspaceId: String!, $invoiceId: String!) {
      workspace(id: $workspaceId) {
        invoice(id: $invoiceId) {
          id
          title
          dateIssued
          amount
          currency
        }
      }
    }
  `), 'workspace');

  const optionsProvider = async (
    connectionRequest: ApiConnectionRequest,
    query: string | undefined,
    requestInit: RequestInit,
  ) => {
    const workspace = await getInvoicesQuery({
      workspaceId: currentWorkspaceId,
      first: connectionRequest.first ?? 10,
      freeSearchText: query ?? null,
    }, {
      requestConfig: requestInit,
    });
    const edges = workspace?.invoices.edges ?? [];
    return {
      edges: edges.map(({ node }) => ({
        node: { ...node } as InvoiceItem,
      })),
      totalCount: workspace?.invoices.totalCount ?? 0,
    };
  };

  const optionProvider = async (
    id: string,
    requestInit: RequestInit,
  ) => {
    const workspace = await getInvoiceQuery({
      workspaceId: currentWorkspaceId,
      invoiceId: id,
    }, {
      requestConfig: requestInit,
    });
    const inv = workspace?.invoice;
    if (!inv) throw new Error(`Invoice ${id} not found`);
    return {
      id: inv.id,
      title: inv.title,
      dateIssued: inv.dateIssued,
      amount: inv.amount,
      currency: inv.currency,
    } as InvoiceItem;
  };
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .sa-invoice-select {
    min-width: 400px;

    &__option {
      display: flex;
      justify-content: space-between;
      flex-wrap: wrap;
      padding-bottom: 10px;

      &__title {
        width: 100%;
        padding-top: 5px;
      }

      &__date, &__amount {
        font-size: 90%;
        line-height: 1em;
        color: $secondary-color;
        display: flex;
        align-items: center;
      }

      &__date {
        .sa-icon {
          width: 14px;
          height: 14px;
          margin-right: 5px;
        }
      }
    }
  }
</style>
