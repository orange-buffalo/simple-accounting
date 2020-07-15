<template>
  <SaInputLoader :loading="!customersLoaded">
    <ElSelect
      v-bind="props"
      v-on="listeners"
    >
      <ElOption
        v-for="customer in customers"
        :key="customer.id"
        :label="customer.name"
        :value="customer.id"
      />
    </ElSelect>
  </SaInputLoader>
</template>

<script>
  import SaInputLoader from '@/components/SaInputLoader';
  import useCustomers from '@/components/customer/useCustomers';

  export default {
    components: {
      SaInputLoader,
    },

    props: {
      value: {
        type: Number,
        default: null,
      },
      placeholder: {
        type: String,
        default: null,
      },
    },

    setup(props, { listeners }) {
      const { customers, customersLoaded } = useCustomers();

      return {
        customers,
        customersLoaded,
        props,
        listeners,
      };
    },
  };
</script>
