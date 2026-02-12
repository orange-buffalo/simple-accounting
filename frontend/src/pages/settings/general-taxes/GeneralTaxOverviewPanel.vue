<template>
  <SaOverviewItem :title="`${tax.title} (${tax.rateInBps / 100}%)`">
    <template
      v-if="tax.description"
      #details
    >
      <!--todo #80: linebreaks-->
      <span class="sa-item-additional-info">{{ tax.description }}</span>
    </template>

    <template #last-column>
      <SaActionLink
        icon="pencil-solid"
        @click="navigateToTaxEdit"
      >
        Edit
      </SaActionLink>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import SaActionLink from '@/components/SaActionLink.vue';
  import useNavigation from '@/services/use-navigation';
  import type { GeneralTaxDto } from '@/services/api';

  const props = defineProps<{
    tax: GeneralTaxDto,
  }>();

  const { navigateToView } = useNavigation();
  const navigateToTaxEdit = () => navigateToView({
    name: 'edit-general-tax',
    params: { id: props.tax.id },
  });
</script>
