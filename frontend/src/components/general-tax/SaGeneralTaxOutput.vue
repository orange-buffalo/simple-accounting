<template>
  <SaOutputLoader :loading="!generalTaxesLoaded">
    {{ generalTaxTitle }}
  </SaOutputLoader>
</template>

<script lang="ts">
  import { computed, defineComponent } from '@vue/composition-api';
  import useGeneralTaxes from '@/components/general-tax/useGeneralTaxes';
  import SaOutputLoader from '@/components/SaOutputLoader';

  export default defineComponent({
    components: {
      SaOutputLoader,
    },

    props: {
      generalTaxId: {
        type: Number,
        default: null,
      },
    },

    setup(props) {
      const {
        generalTaxById,
        generalTaxesLoaded,
      } = useGeneralTaxes();

      const generalTaxTitle = computed(() => generalTaxById.value(props.generalTaxId).title);

      return {
        generalTaxesLoaded,
        generalTaxTitle,
      };
    },
  });
</script>
