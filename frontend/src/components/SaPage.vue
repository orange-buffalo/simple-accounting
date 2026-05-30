<template>
  <div class="sa-page">
    <div
      v-if="header || $slots.header || $slots['header-options']"
      class="sa-page__header"
    >
      <slot name="header">
        <h1 v-if="header">{{ header }}</h1>
      </slot>

      <div
        v-if="$slots['header-options']"
        class="sa-page__header-options"
      >
        <slot name="header-options" />
      </div>
    </div>

    <slot />
  </div>
</template>

<script lang="ts" setup>
  defineProps<{
    header?: string,
  }>();
</script>

<style lang="scss" scoped>
  @use "@/styles/vars.scss" as *;

  .sa-page {
    &__header {
      h1 {
        margin-top: 0;
        margin-bottom: 10px;
        font-size: 160%;
      }

      &:after {
        content: "";
        display: block;
        width: 100%;
        height: 1px;
        margin-top: 10px;
        margin-bottom: 30px;
        background: $secondary-grey;
      }
    }

    &__header-options {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
</style>
