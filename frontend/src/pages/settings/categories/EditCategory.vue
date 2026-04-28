<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm v-model="formValues" :on-submit="saveCategory" :on-load="loadCategory" :on-cancel="navigateToCategoriesOverview">
      <SaFormInput prop="name" :label="$t.editCategory.form.name.label()" />
      <SaFormInput prop="description" :label="$t.editCategory.form.description.label()" type="textarea" />
      <SaFormCheckbox prop="income" :label="$t.editCategory.form.income.label()" />
      <SaFormCheckbox prop="expense" :label="$t.editCategory.form.expense.label()" />
    </SaForm>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaFormCheckbox from '@/components/form/SaFormCheckbox.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { graphql } from '@/services/api/gql';
  import { useMutation, useLazyQuery } from '@/services/api/use-gql-api.ts';
  import { $t } from '@/services/i18n';
  import {
    CreateCategoryMutationVariables,
    EditCategoryMutationVariables,
  } from '@/services/api/gql/graphql.ts';
  import { AsFormValues, toRequestArgs, updateFormValues } from '@/components/form/sa-form-api.ts';

  const props = defineProps<{
    id?: number,
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToCategoriesOverview = async () => navigateByViewName('settings-categories');
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

  const createCategory = useMutation(graphql(`
    mutation createCategory(
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

  const editCategory = useMutation(graphql(`
    mutation editCategory(
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

  type CategoryFormValues = AsFormValues<[CreateCategoryMutationVariables, EditCategoryMutationVariables]>;

  const formValues = ref<CategoryFormValues>({
    workspaceId: currentWorkspaceId,
    id: props.id,
    income: false,
    expense: false,
  });

  const loadCategory = props.id !== undefined ? async () => {
    const workspace = await getCategoryQuery({
      workspaceId: currentWorkspaceId,
      categoryId: props.id!,
    });
    updateFormValues(formValues, workspace.category);
  } : undefined;

  const saveCategory = async () => {
    if (props.id === undefined) {
      await createCategory(toRequestArgs(formValues));
    } else {
      await editCategory(toRequestArgs(formValues));
    }
    await navigateToCategoriesOverview();
  };

  const pageHeader = computed(() => props.id !== undefined
    ? $t.value.editCategory.pageHeader.edit()
    : $t.value.editCategory.pageHeader.create());
</script>
