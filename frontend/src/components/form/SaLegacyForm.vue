<template>
  <div class="sa-form">
    <ElForm
      ref="elForm"
      v-loading="loading"
      label-position="top"
      :model="props.model"
      :rules="rules"
    >
      <slot />

      <hr>

      <div class="sa-form__buttons-bar-legacy">
        <slot name="buttons-bar" />
      </div>
    </ElForm>
  </div>
</template>

<script lang="ts" setup>
import { ElForm, type FormInstance, type FormRules } from 'element-plus';
import { ref } from 'vue';
import type { FormValues } from '@/components/form/sa-form-components-api.ts';

type SaLegacyFormProps = {
  model: FormValues;
  rules?: FormRules;
  initiallyLoading?: boolean;
};

const props = withDefaults(defineProps<SaLegacyFormProps>(), {
  initiallyLoading: true,
});

const elForm = ref<FormInstance | undefined>(undefined);
const loading = ref(props.initiallyLoading);

const validate = async (): Promise<boolean> => {
  if (elForm.value) {
    return elForm.value.validate();
  }
  return false;
};
const startLoading = () => {
  loading.value = true;
};
const stopLoading = () => {
  loading.value = false;
};
defineExpose({
  validate,
  startLoading,
  stopLoading,
});
</script>

<style lang="scss">
  /*todo #73: common component refers to app styles - redesign dependencies  */
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .sa-form {
    padding: 20px;
    border: 1px solid $secondary-grey;
    background-color: $white;
    border-radius: 2px;
    overflow: hidden;

    .el-form {
      width: 100%;
    }

    .el-form-item {
      margin-bottom: 15px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .sa-documents_upload {
      margin-top: 15px;
    }

    .el-select {
      @include input-width;
    }

    .el-input {
      @include input-width;
    }

    .el-form-item__label {
      padding: 0;
    }

    .el-input {
      .el-input-number & {
        width: 100%;
      }
    }

    h2 {
      margin-bottom: 10px;
    }

    hr {
      border: 1px solid $primary-grey;
      margin-top: 10px;
      margin-bottom: 10px;
    }

    &__buttons-bar-legacy {
      margin-top: 20px;
      display: flex;
      justify-content: space-between;
    }
  }
</style>
