<template>
  <div
    class="sa-status-label"
    :class="statusClass"
  >
    <SaIcon
      v-if="!hideIcon"
      :icon="statusIcon"
      class="sa-status-label__icon"
    />
    <slot />
  </div>
</template>

<script lang="ts">
  import SaIcon from '@/components/SaIcon';
  import { defineComponent, computed, PropType } from '@vue/composition-api';

  type StatusLabelStatus = 'success' | 'pending' | 'regular' | 'failure';

  export default defineComponent({
    components: {
      SaIcon,
    },

    props: {
      status: {
        type: String as PropType<StatusLabelStatus>,
        required: true,
      },
      simplified: {
        type: Boolean,
        default: false,
      },
      customIcon: {
        type: String,
        default: null,
      },
      hideIcon: {
        type: Boolean,
        default: false,
      },
    },

    setup(props) {
      const statusIcon = computed((): string => {
        if (props.customIcon) {
          return props.customIcon;
        }
        switch (props.status) {
        case 'success':
          return 'success';
        case 'pending':
          return 'hourglass';
        case 'failure':
          return 'error';
        case 'regular':
          return 'gear';
        default:
          throw new Error(`${props.status} is not supported yet`);
        }
      });

      const statusClass = computed(() => ({
        'sa-status-label_success': !props.simplified && props.status === 'success',
        'sa-status-label_pending': !props.simplified && props.status === 'pending',
        'sa-status-label_regular': !props.simplified && props.status === 'regular',
        'sa-status-label_failure': !props.simplified && props.status === 'failure',
        'sa-status-label_success-simplified': props.simplified && props.status === 'success',
        'sa-status-label_pending-simplified': props.simplified && props.status === 'pending',
        'sa-status-label_regular-simplified': props.simplified && props.status === 'regular',
        'sa-status-label_failure-simplified': props.simplified && props.status === 'failure',
      }));

      return {
        statusIcon,
        statusClass,
      };
    },
  });
</script>

<style lang="scss">
  /*todo #73: common component refers to app styles - redesign dependencies  */
  @import "~@/styles/vars.scss";

  .sa-status-label {
    display: inline-flex;
    align-items: center;
    padding: 4px 10px;
    border-radius: 3px;

    .sa-status-label__icon {
      margin-right: 4px;
      height: 16px;
      width: 14px;
    }

    &_pending {
      background: $warning-color-bg;
      color: $warning-color;
    }

    &_pending-simplified {
      color: $warning-color;
      padding: 0;
    }

    &_success {
      background: $success-color-bg;
      color: $success-color;
    }

    &_success-simplified {
      color: $success-color;
      padding: 0;
    }

    &_regular {
      background: $primary-grey;
      color: $secondary-text-color;
    }

    &_regular-simplified {
      color: $secondary-text-color;
      padding: 0;
    }

    &_failure {
      background: $danger-color-bg;
      color: $danger-color;
    }

    &_failure-simplified {
      color: $danger-color;
      padding: 0;
    }
  }
</style>
