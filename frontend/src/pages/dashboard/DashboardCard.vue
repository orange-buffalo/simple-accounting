<template>
  <div class="sa-dashboard__card">
    <div class="sa-dashboard__card__header">
      <span class="sa-dashboard__card__header__icon">
        <SaIcon :icon="headerIcon" variant="detailed" />
      </span>
      <span v-if="title" class="sa-dashboard__card__header__title">{{ title }}</span>

      <slot v-if="loaded" name="header" />

      <div
        v-else
        class="sa-dashboard__card__header__loader"
      >
        <SaIcon icon="loading" :size="32" />
      </div>
    </div>

    <div
      v-if="loaded"
      class="sa-dashboard__card__details"
    >
      <slot name="content" />
    </div>
  </div>
</template>

<script lang="ts" setup>
  import SaIcon from '@/components/SaIcon.vue';

  defineProps<{
    loaded: boolean,
    headerIcon: string,
    title?: string,
  }>();
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as * ;
  .sa-dashboard__card {
    border: 1px solid $secondary-grey;
    background-color: $white;
    border-radius: 14px;
    min-width: 0;
    overflow: hidden;
    box-shadow: 0 3px 12px rgba(20, 43, 64, 0.025);

    &__header {
      padding: 24px;
      min-height: 170px;
      box-sizing: border-box;

      &__title {
        display: block;
        font-size: 14px;
        font-weight: bold;
        padding-right: 36px;
      }

      &__icon {
        float: right;
        color: $accent-primary-color;

        .sa-icon {
          width: 26px !important;
          height: 26px !important;
        }
      }

      &__loader {
        margin-top: 20px;
        color: $secondary-grey-darker-i;
      }

      &__amount {
        display: block;
        margin: 16px 0 10px;
        font-size: clamp(20px, 2vw, 28px);
        font-weight: bold;
        overflow-wrap: anywhere;
        font-variant-numeric: tabular-nums;
      }

      &__finalized {
        display: block;
        font-size: 13px;
        color: $secondary-text-color;
      }

      &__pending {
        display: inline-block;
        color: #985008;
        background: $warning-color-bg;
        border-radius: 20px;
        padding: 4px 9px;
        margin-top: 10px;
        font-size: 12px;
        &--empty {
          visibility: hidden;
        }
      }
    }

    &__details {
      margin: 0 24px;
      padding: 16px 0 20px;
      border-top: 1px solid $secondary-grey;

      &__item {
        display: flex;
        justify-content: space-between;
        gap: 16px;
        padding: 7px 0;
        font-size: 13px;
        color: $secondary-text-color;

        > :first-child {
          min-width: 0;
          overflow-wrap: anywhere;
        }

        > :last-child {
          text-align: right;
          color: $primary-text-color;
          font-weight: 600;
          overflow-wrap: anywhere;
          min-width: 0;
        }
      }
    }

    &--profit {
      .sa-dashboard__card__header {
        background: $secondary-color;
        color: $white;
      }

      .sa-dashboard__card__header__icon {
        color: $accent-contrast-color;
      }

      .sa-dashboard__card__details {
        border-top: 0;
      }

      .sa-dashboard__card__details__item:last-child {
        margin-top: 8px;
        padding-top: 16px;
        border-top: 1px solid $secondary-grey;
        font-weight: bold;
      }
    }
  }
</style>
