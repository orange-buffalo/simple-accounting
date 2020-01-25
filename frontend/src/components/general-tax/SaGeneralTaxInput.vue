<template>
  <SaInputLoader :loading="!generalTaxesLoaded">
    <!--suppress RequiredAttributes -->
    <ElSelect
      v-bind="props"
      v-on="listeners"
    >
      <ElOption
        v-for="tax in generalTaxes"
        :key="tax.id"
        :label="tax.title"
        :value="tax.id"
      />
    </ElSelect>
  </SaInputLoader>
</template>

<script>
  import SaInputLoader from '@/components/SaInputLoader';
  import useGeneralTaxes from '@/components/general-tax/useGeneralTaxes';

  export default {
    components: {
      SaInputLoader,
    },

    props: {
      value: {
        // eslint-disable-next-line no-bitwise,vue/require-prop-type-constructor
        type: Number | String,
        default: null,
      },
      placeholder: {
        type: String,
        default: null,
      },
      clearable: {
        type: Boolean,
        default: false,
      },
    },

    setup(props, { listeners }) {
      const { generalTaxes, generalTaxesLoaded } = useGeneralTaxes();

      return {
        generalTaxes,
        generalTaxesLoaded,
        props,
        listeners,
      };
    },
  };
</script>
