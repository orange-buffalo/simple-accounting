<template>
  <div class="sa-form-switch-section">
    <ElSwitch
      v-model="switchValue"
      @change="onChange"
    />
    <h4>{{ props.label }}</h4>
  </div>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { useSaFormComponentsApi } from '@/components/form/sa-form-components-api';

  const props = defineProps<{
    label: string,
    modelValue: boolean,
    submitOnChange?: boolean,
  }>();

  const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void,
  }>();

  const switchValue = ref(props.modelValue);

  watch(() => props.modelValue, (newVal) => {
    switchValue.value = newVal;
  });

  const formApi = useSaFormComponentsApi();

  const onChange = async () => {
    emit('update:modelValue', switchValue.value);
    if (props.submitOnChange) {
      await formApi.submitForm();
    }
  };
</script>

<style lang="scss">
  .sa-form-switch-section {
    display: flex;
    align-items: center;
    margin-bottom: 10px;

    h4 {
      display: inline;
      margin: 0 0 0 10px;
    }
  }
</style>
