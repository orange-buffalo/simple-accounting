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
  import type { GeneralTaxesPageQuery } from '@/services/api/gql/graphql';

  type GeneralTaxNode = GeneralTaxesPageQuery['workspace']['generalTaxes']['edges'][0]['node'];

  const props = defineProps<{
    tax: GeneralTaxNode,
  }>();

  const { navigateToView } = useNavigation();
  const navigateToTaxEdit = () => navigateToView({
    name: 'edit-general-tax',
    params: { id: props.tax.id },
  });
</script>
