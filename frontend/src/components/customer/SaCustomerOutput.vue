<template>
  <SaOutputLoader :loading="!customersLoaded">
    {{ customerName }}
  </SaOutputLoader>
</template>

<script lang="ts">
  import { computed, defineComponent } from '@vue/composition-api';
  import useCustomers from '@/components/customer/useCustomers';
  import SaOutputLoader from '@/components/SaOutputLoader';

  export default defineComponent({
    components: {
      SaOutputLoader,
    },

    props: {
      customerId: {
        type: Number,
        default: null,
      },
    },

    setup({ customerId }) {
      const {
        customersLoaded,
        customerById,
      } = useCustomers();

      const customerName = computed(() => customerById.value(customerId).name);

      return {
        customersLoaded,
        customerName,
      };
    },
  });
</script>
