<template>
  <div class="sa-action-link">
    <SaIcon
      :icon="icon"
      class="sa-action-link__icon"
    />
    <!--    TODO #460: type=text is deprecated, need to replace with text attribute and style-->
    <ElButton
      link
      @click="click"
    >
      <slot />
    </ElButton>
  </div>
</template>

<script lang="ts" setup>
import SaIcon from '@/components/SaIcon.vue';

defineProps<{
  icon: string;
}>();

const emit = defineEmits<(e: 'click') => void>();

const click = () => {
  emit('click');
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

    .el-button {
      padding: 0;
      height: 32px;
    }
  }
</style>
