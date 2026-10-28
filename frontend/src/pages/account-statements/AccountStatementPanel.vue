<template>
  <SaOverviewItem :title="statement.partyName || statement.externalId" class="account-statement">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute icon="calendar" :tooltip="$t.accountStatements.date()">
        {{ $t.common.dateTime.medium(statement.occurredAt) }}
      </SaOverviewItemPrimaryAttribute>
      <SaOverviewItemPrimaryAttribute icon="integrations" :tooltip="$t.accountStatements.provider()">
        {{ provider }} · {{ statement.accountCurrency }}
      </SaOverviewItemPrimaryAttribute>
    </template>
    <template #middle-column>{{ type }}</template>
    <template #last-column>
      <SaOverviewItemAmountPanel :amount="statement.amountInCents" :currency="statement.currency" />
    </template>
    <template #details>
      <SaOverviewItemDetailsSection :title="$t.accountStatements.details()">
        <div class="row">
          <SaOverviewItemDetailsSectionAttribute
            v-for="attribute in attributes"
            :key="attribute.label"
            :label="attribute.label"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ attribute.value }}
          </SaOverviewItemDetailsSectionAttribute>
        </div>
      </SaOverviewItemDetailsSection>
      <SaOverviewItemDetailsSection v-if="statement.additionalData" :title="$t.accountStatements.additionalData()">
        <div class="row">
          <div class="col col-xs-12 account-statement__additional-data">{{ statement.additionalData }}</div>
        </div>
      </SaOverviewItemDetailsSection>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import SaOverviewItemPrimaryAttribute from '@/components/overview-item/SaOverviewItemPrimaryAttribute.vue';
  import SaOverviewItemAmountPanel from '@/components/overview-item/SaOverviewItemAmountPanel.vue';
  import SaOverviewItemDetailsSection from '@/components/overview-item/SaOverviewItemDetailsSection.vue';
  import SaOverviewItemDetailsSectionAttribute
    from '@/components/overview-item/SaOverviewItemDetailsSectionAttribute.vue';
  import type { AccountStatementsPageQuery } from '@/services/api/gql/graphql';
  import { $t } from '@/services/i18n';

  type Statement = NonNullable<AccountStatementsPageQuery['workspace']>['accountStatements']['edges'][number]['node'];
  const props = defineProps<{ statement: Statement }>();
  const provider = computed(() => ({ WISE: $t.value.integrations.wise.name() }[props.statement.provider]));
  const type = computed(() => $t.value.accountStatements.types[props.statement.type]());
  const attributes = computed(() => {
    const s = props.statement;
    const t = $t.value.accountStatements;
    const money = (amount: number | null | undefined, currency: string | null | undefined) => (
      amount != null && currency ? $t.value.common.amount.withCurrency(amount, currency) : null
    );
    return [
      { label: t.party(), value: s.partyName },
      { label: t.amount(), value: money(s.amountInCents, s.currency) },
      { label: t.type(), value: type.value },
      { label: t.date(), value: $t.value.common.dateTime.medium(s.occurredAt) },
      { label: t.provider(), value: provider.value },
      { label: t.accountId(), value: s.accountId },
      { label: t.accountCurrency(), value: s.accountCurrency },
      { label: t.externalId(), value: s.externalId },
      { label: t.fees(), value: money(s.feesInCents, s.feesCurrency) },
      { label: t.runningBalance(), value: money(s.runningBalanceInCents, s.runningBalanceCurrency) },
      { label: t.originalAmount(), value: money(s.originalAmountInCents, s.originalCurrency) },
      { label: t.sourceAmount(), value: money(s.sourceAmountInCents, s.sourceCurrency) },
      { label: t.targetAmount(), value: money(s.targetAmountInCents, s.targetCurrency) },
      { label: t.exchangeRate(), value: s.exchangeRate == null ? null : String(s.exchangeRate) },
      { label: t.importedAt(), value: $t.value.common.dateTime.medium(s.createdAt) },
      { label: t.integrationId(), value: s.integrationId },
      { label: t.recordId(), value: s.id },
    ].filter((attribute) => attribute.value != null);
  });
</script>

<style lang="scss" scoped>
  .account-statement {
    :deep(.overview-item__details) { max-height: none; overflow: visible; }
    :deep(.overview-item-details-section-attribute) { overflow-wrap: anywhere; }
    &__additional-data { white-space: pre-wrap; overflow-wrap: anywhere; padding-bottom: 20px; }
  }
</style>
