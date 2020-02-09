<template>
  <SaOutputLoader :loading="!generalTaxesLoaded">
    {{ generalTaxTitle }}
  </SaOutputLoader>
</template>

<script>
  import { computed } from '@vue/composition-api';
  import useGeneralTaxes from '@/components/general-tax/useGeneralTaxes';
  import SaOutputLoader from '@/components/SaOutputLoader';

  export default {
    components: {
      SaOutputLoader,
    },

    props: {
      generalTaxId: {
        type: Number,
        default: null,
      },
    },

    setup({ generalTaxId }) {
      const { generalTaxById, generalTaxesLoaded } = useGeneralTaxes();

      const generalTaxTitle = computed(() => generalTaxById.value(generalTaxId).title);

      return {
        generalTaxesLoaded,
        generalTaxTitle,
      };
    },
  };
</script>
