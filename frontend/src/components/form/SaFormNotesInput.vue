<template>
  <SaFormItemInternal v-bind="props" v-model="inputValue">
    <!--
      SaNotesInput uses string | undefined, while SaFormItemInternal uses string | null.
      The conversion bridges this type gap without any functional difference.
    -->
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
