<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <SaNotesInput
      :model-value="inputValue ?? undefined"
      :placeholder="placeholder"
      @update:model-value="inputValue = $event ?? null"
    />
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { SaFormComponentProps } from '@/components/form/sa-form-api';
  import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
  import SaNotesInput from '@/components/notes-input/SaNotesInput.vue';

  const inputValue = ref<string | null>(null);

  const props = defineProps<SaFormComponentProps & {
    placeholder?: string,
  }>();
</script>
