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
import type { ApiPageRequest, HasOptionalId, InvoiceDto } from '@/services/api';
import { invoicesApi } from '@/services/api';
import { $t } from '@/services/i18n';
import { useCurrentWorkspace } from '@/services/workspaces';

defineProps<{
  modelValue?: number;
}>();

const emit = defineEmits<(e: 'update:modelValue', value?: number) => void>();

// case required as Vue does not support generic slots/props
const invoice = (entity: HasOptionalId) => entity as InvoiceDto;
const invoiceLabelProvider = (entity: HasOptionalId) => invoice(entity).title;

const { currentWorkspaceId } = useCurrentWorkspace();

const optionsProvider = async (pageRequest: ApiPageRequest, query: string | undefined, requestInit: RequestInit) =>
  invoicesApi.getInvoices(
    {
      workspaceId: currentWorkspaceId,
      freeSearchTextEq: query,
      ...pageRequest,
    },
    requestInit,
  );

const optionProvider = async (id: number, requestInit: RequestInit) =>
  invoicesApi.getInvoice(
    {
      workspaceId: currentWorkspaceId,
      invoiceId: id,
    },
    requestInit,
  );
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
