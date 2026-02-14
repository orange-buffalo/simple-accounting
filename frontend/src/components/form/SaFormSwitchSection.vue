<template>
  <SaFormItemInternal v-bind="props" :label="undefined" v-model="switchValue">
    <div class="sa-form-switch-section">
      <ElSwitch
        v-model="switchValue"
        @change="onChange"
      />
      <h4>{{ props.label }}</h4>
    </div>
  </SaFormItemInternal>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import SaFormItemInternal from '@/components/form/SaFormItemInternal.vue';
import type { SaFormComponentProps } from '@/components/form/sa-form-api';
import { useSaFormComponentsApi } from '@/components/form/sa-form-components-api';

const props = defineProps<
  SaFormComponentProps & {
    submitOnChange?: boolean;
  }
>();

const switchValue = ref<boolean | null>();

const formApi = useSaFormComponentsApi();

const onChange = async () => {
  if (props.submitOnChange) {
    await formApi.submitForm();
  }
};
</script>

<style lang="scss">
  .sa-form-switch-section {
    display: flex;
    align-items: center;

    h4 {
      display: inline;
      margin: 0 0 0 10px;
    }
  }
</style>
