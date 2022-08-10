<template>
  <div class="sa-action-link">
    <SaIcon
      :icon="icon"
      class="sa-action-link__icon"
    />
<!--    TODO type=text is deprecated, need to replace with text attribute and style-->
    <ElButton
      type="text"
      @click="click"
    >
      <slot />
    </ElButton>
  </div>
</template>

<script lang="ts" setup>
  import SaIcon from '@/components/SaIcon.vue';

  defineProps<{
    icon: string
  }>();

  const emits = defineEmits<{(e: 'click'): void}>();

  const click = () => {
    emits('click');
  };
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .sa-action-link {
    display: inline-flex;
    align-items: center;
    color: $components-color;

    .sa-action-link__icon {
      margin-right: 3px;
      height: 13px;
      width: 13px;
    }
  }
</style>
