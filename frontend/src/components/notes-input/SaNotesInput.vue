<template>
  <div class="sa-notes-input">
    <ElInput
      :model-value="modelValue"
      @update:modelValue="emit('update:modelValue', $event)"
      :placeholder="placeholder"
      type="textarea"
      rows="5"
      class="sa-notes-input__input"
    />

    <SaMarkdownOutput
      v-if="modelValue"
      :source="modelValue"
      preview
      class="sa-notes-input__preview"
    />
  </div>
</template>

<script setup lang="ts">
import SaMarkdownOutput from '@/components/SaMarkdownOutput.vue';

defineProps<{
  modelValue?: string;
  placeholder?: string;
}>();

const emit = defineEmits<(e: 'update:modelValue', value?: string) => void>();
</script>

<style lang="scss">
  .sa-notes-input {
    &__input {
      width: 100% !important;
    }

    &__preview {
      margin-top: 10px;
      line-height: 1em;
      padding: 5px 15px;
    }
  }
</style>
