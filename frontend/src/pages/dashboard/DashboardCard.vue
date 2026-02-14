<template>
  <div class="sa-dashboard__card">
    <div class="sa-dashboard__card__header">
      <span class="sa-dashboard__card__header__icon">
        <SaIcon :icon="headerIcon" />
      </span>

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
  loaded: boolean;
  headerIcon: string;
}>();
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as * ;
  @use "sass:math";

  $header-icon-container-size: 20%;
  $header-icon-size: 60%;
  $header-icon-position: math.div(100% - $header-icon-size, 2);

  .sa-dashboard__card {
    padding: 20px;
    border: 1px solid $secondary-grey;
    background-color: $white;
    border-radius: 2px;
    width: 27%;
    margin-bottom: 20px;

    &__header {
      text-align: center;

      &__icon {
        display: inline-block;
        width: $header-icon-container-size;
        padding-top: $header-icon-container-size;
        border: 1px solid $secondary-grey;
        border-radius: 50%;
        position: relative;

        .sa-icon {
          position: absolute;
          width: $header-icon-size !important;
          height: $header-icon-size !important;
          top: $header-icon-position;
          left: $header-icon-position;
        }
      }

      &__loader {
        margin-top: 20px;
        color: $secondary-grey-darker-i;
      }

      &__amount {
        display: block;
        margin: 10px 0;
        font-size: 130%;
        font-weight: bold;
      }

      &__finalized {
        display: block;
        font-size: 90%;
        color: $secondary-text-color;
      }

      &__pending {
        display: block;
        color: $warning-color;
        font-size: 90%;
      }
    }

    &__details {
      margin-top: 20px;
      padding-top: 10px;
      border-top: 1px solid $secondary-grey;

      &__item {
        display: flex;
        justify-content: space-between;
        padding: 5px 0 0;
        font-size: 80%;
        color: $secondary-text-color;
      }
    }
  }
</style>
