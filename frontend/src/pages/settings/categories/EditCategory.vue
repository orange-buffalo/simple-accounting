<template>
  <div>
    <div class="sa-page-header">
      <h1>Edit Category</h1>
    </div>

    <SaForm v-model="formValues" :on-submit="saveCategory" :on-load="loadCategory" :on-cancel="navigateToCategoriesOverview">
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
  import { useMutation, useLazyQuery } from '@/services/api/use-gql-api.ts';

  const props = defineProps<{
    id: number,
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToCategoriesOverview = async () => navigateByViewName('settings-categories');

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

  const { currentWorkspaceId } = useCurrentWorkspace();

  const getCategoryQuery = useLazyQuery(graphql(`
    query getCategoryForEdit($workspaceId: Long!, $categoryId: Long!) {
      workspace(id: $workspaceId) {
        category(id: $categoryId) {
          id
          name
          description
          income
          expense
        }
      }
    }
  `), 'workspace');

  const loadCategory = async () => {
    const workspace = await getCategoryQuery({
      workspaceId: currentWorkspaceId,
      categoryId: props.id,
    });
    const loaded = workspace?.category;
    if (loaded) {
      formValues.value = {
        name: loaded.name,
        description: loaded.description ?? null,
        income: loaded.income,
        expense: loaded.expense,
      };
    }
  };

  const editCategoryMutation = useMutation(graphql(`
    mutation editCategoryMutation(
      $workspaceId: Long!,
      $id: Long!,
      $name: String!,
      $description: String,
      $income: Boolean!,
      $expense: Boolean!
    ) {
      editCategory(
        workspaceId: $workspaceId,
        id: $id,
        name: $name,
        description: $description,
        income: $income,
        expense: $expense
      ) {
        id
      }
    }
  `), 'editCategory');

  const saveCategory = async () => {
    await editCategoryMutation({
      workspaceId: currentWorkspaceId,
      id: props.id,
      name: formValues.value.name,
      description: formValues.value.description || null,
      income: formValues.value.income,
      expense: formValues.value.expense,
    });
    await navigateToCategoriesOverview();
  };
</script>
