<template>
  <ElContainer>
    <ElMain>
      <h1>Workspace</h1>
      <ElForm
        ref="formRef"
        :model="form"
        :rules="formValidationRules"
      >
        <ElFormItem
          label="Name"
          prop="name"
        >
          <ElInput v-model="form.name" />
        </ElFormItem>
        <ElFormItem
          label="Tax Enabled"
          prop="taxEnabled"
        >
          <ElCheckbox v-model="form.taxEnabled" />
        </ElFormItem>
        <ElFormItem
          label="Multi-currency Enabled"
          prop="multiCurrencyEnabled"
        >
          <ElCheckbox v-model="form.multiCurrencyEnabled" />
        </ElFormItem>
        <ElFormItem
          label="Default Currency"
          prop="defaultCurrency"
        >
          <ElInput v-model="form.defaultCurrency" />
        </ElFormItem>
        <ElFormItem>
          <ElButton
            type="primary"
            @click="submitForm"
          >
            Save
          </ElButton>
        </ElFormItem>
      </ElForm>
    </ElMain>
  </ElContainer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { useForm } from '@/components/form/use-form';
  import { workspacesApi } from '@/services/api';
  import useNavigation from '@/services/use-navigation';

  const formValidationRules = {
    name: [
      {
        required: true,
        message: 'Please input name',
        trigger: 'blur',
      },
    ],
    defaultCurrency: [
      {
        required: true,
        message: 'Please input currency',
        trigger: 'blur',
      },
    ],
  };

  const form = ref({
    name: '',
    taxEnabled: false,
    multiCurrencyEnabled: false,
    defaultCurrency: 'AUD',
  });

  const save = async () => {
    await workspacesApi.createWorkspace({
      createWorkspaceDto: form.value,
    });
    await useNavigation()
      .navigateByPath('/');
  };

  const {
    formRef,
    submitForm,
  } = useForm(async () => {
    // no op
  }, save);
</script>
