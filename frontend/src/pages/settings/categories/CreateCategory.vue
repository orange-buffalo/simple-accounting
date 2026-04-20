<template>
  <div>
    <div class="sa-page-header">
      <h1>Create New Category</h1>
    </div>

    <SaForm v-model="formValues" :on-submit="saveCategory" :on-cancel="navigateToCategoriesOverview">
      <SaFormInput prop="name" label="Name" />
      <SaFormInput prop="description" label="Description" type="textarea" />
      <ElFormItem>
        <ElCheckbox v-model="formValues.income">Income</ElCheckbox>
      </ElFormItem>
      <ElFormItem>
        <ElCheckbox v-model="formValues.expense">Expense</ElCheckbox>
      </ElFormItem>
    </SaForm>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api.ts';

  type CategoryFormValues = {
    name: string,
    description: string | null,
    income: boolean,
    expense: boolean,
  };

  const formValues = ref<CategoryFormValues>({
    name: '',
    description: null,
    income: false,
    expense: false,
  });

  const { navigateByViewName } = useNavigation();
  const navigateToCategoriesOverview = async () => navigateByViewName('settings-categories');

  const { currentWorkspaceId } = useCurrentWorkspace();

  const createCategoryMutation = useMutation(graphql(`
    mutation createCategoryMutation(
      $workspaceId: Long!,
      $name: String!,
      $description: String,
      $income: Boolean!,
      $expense: Boolean!
    ) {
      createCategory(
        workspaceId: $workspaceId,
        name: $name,
        description: $description,
        income: $income,
        expense: $expense
      ) {
        id
      }
    }
  `), 'createCategory');

  const saveCategory = async () => {
    await createCategoryMutation({
      workspaceId: currentWorkspaceId,
      name: formValues.value.name,
      description: formValues.value.description || null,
      income: formValues.value.income,
      expense: formValues.value.expense,
    });
    await navigateToCategoriesOverview();
  };
</script>
