<template>
  <SaOverviewItem :title="document.name">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute
        :tooltip="$t.documentsOverviewPanel.timeUploaded.label()"
        icon="calendar"
      >
        {{ $t.common.dateTime.medium(document.timeUploaded) }}
      </SaOverviewItemPrimaryAttribute>

      <SaOverviewItemPrimaryAttribute
        :tooltip="$t.documentsOverviewPanel.storage.label()"
        icon="upload"
      >
        {{ storageLabel }}
      </SaOverviewItemPrimaryAttribute>
    </template>

    <template #middle-column>
      <div
        v-if="document.usedBy.length > 0"
        class="documents-overview-panel__usages"
      >
        <a
          v-for="usage in document.usedBy"
          :key="`${usage.type}-${usage.relatedEntityId}`"
          class="documents-overview-panel__usage-link"
          href="#"
          @click.prevent="navigateToUsage(usage)"
        >
          {{ usage.displayName }}
        </a>
      </div>
      <SaStatusLabel
        v-else
        status="pending"
      >
        {{ $t.documentsOverviewPanel.unused() }}
      </SaStatusLabel>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import SaOverviewItemPrimaryAttribute from '@/components/overview-item/SaOverviewItemPrimaryAttribute.vue';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n';
  import { DocumentUsageType } from '@/services/api/gql/graphql';

  interface DocumentUsage {
    type: DocumentUsageType,
    relatedEntityId: number,
    displayName: string,
  }

  interface DocumentItem {
    id: number,
    name: string,
    timeUploaded: string,
    storageId: string,
    usedBy: DocumentUsage[],
  }

  const props = defineProps<{
    document: DocumentItem,
  }>();

  const { navigateToView } = useNavigation();

  const storageLabel = computed(() => {
    switch (props.document.storageId) {
    case 'google-drive':
      return $t.value.documentsOverviewPanel.storage.googleDrive();
    default:
      return $t.value.documentsOverviewPanel.storage.internalSystem();
    }
  });

  const usageTypeToRouteMap: Record<DocumentUsageType, string> = {
    [DocumentUsageType.Expense]: 'edit-expense',
    [DocumentUsageType.Income]: 'edit-income',
    [DocumentUsageType.Invoice]: 'edit-invoice',
    [DocumentUsageType.IncomeTaxPayment]: 'edit-income-tax-payment',
  };

  const navigateToUsage = (usage: DocumentUsage) => {
    const routeName = usageTypeToRouteMap[usage.type];
    navigateToView({
      name: routeName,
      params: { id: usage.relatedEntityId },
    });
  };
</script>

<style lang="scss">
  .documents-overview-panel {
    &__usages {
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    &__usage-link {
      display: block;
      text-decoration: none;
      color: inherit;

      &:hover {
        text-decoration: underline;
      }
    }
  }
</style>
