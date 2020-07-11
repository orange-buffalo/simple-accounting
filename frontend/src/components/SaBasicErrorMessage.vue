<template>
  <SaStatusLabel
    simplified
    status="failure"
    class="sa-basic-error-message"
  >
    {{ errorMessage }}
  </SaStatusLabel>
</template>

<script>
  import { computed } from '@vue/composition-api';
  import i18n from '@/services/i18n';
  import SaStatusLabel from '@/components/SaStatusLabel';

  export default {
    components: { SaStatusLabel },

    props: {
      message: {
        type: String,
        default: null,
      },
    },

    setup(props) {
      const errorMessage = computed(() => props.message || i18n.t('saBasicErrorMessage.defaultMessage'));

      return {
        errorMessage,
      };
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";
  @import "~@/styles/mixins.scss";

  .sa-basic-error-message {
    white-space: nowrap;
    .sa-status-label__icon {
      min-width: 18px;
      min-height: 18px;
      margin-right: 5px;
    }
  }
</style>
