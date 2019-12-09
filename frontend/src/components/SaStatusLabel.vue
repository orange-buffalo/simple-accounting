<template>
  <div
    class="sa-status-label"
    :class="statusClass"
  >
    <SaIcon
      :icon="statusIcon"
      class="sa-status-label__icon"
    />
    <slot />
  </div>
</template>

<script>
  import SaIcon from '@/components/SaIcon';

  export default {
    name: 'SaStatusLabel',

    components: {
      SaIcon,
    },

    props: {
      status: {
        type: String,
        required: true,
        validator(value) {
          return [
            'success',
            'pending',
            'regular',
            'failure',
          ].indexOf(value) !== -1;
        },
      },
      simplified: {
        type: Boolean,
        default: false,
      },
      customIcon: {
        type: String,
      },
    },

    computed: {
      statusIcon() {
        if (this.customIcon) {
          return this.customIcon;
        }
        switch (this.status) {
        case 'success':
          return 'success';
        case 'pending':
          return 'hourglass';
        case 'failure':
          return 'error';
        case 'regular':
          return 'gear';
        }
      },

      statusClass() {
        return {
          'sa-status-label_success': !this.simplified && this.status === 'success',
          'sa-status-label_pending': !this.simplified && this.status === 'pending',
          'sa-status-label_regular': !this.simplified && this.status === 'regular',
          'sa-status-label_failure': !this.simplified && this.status === 'failure',
          'sa-status-label_success-simplified': this.simplified && this.status === 'success',
          'sa-status-label_pending-simplified': this.simplified && this.status === 'pending',
          'sa-status-label_regular-simplified': this.simplified && this.status === 'regular',
          'sa-status-label_failure-simplified': this.simplified && this.status === 'failure',
        };
      },
    },
  };
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
      margin-top: 2px;
      height: 14px;
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
