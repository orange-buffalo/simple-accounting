<template>
  <SaPage :header="pageHeader">

    <SaForm
      v-model="formValues"
      :on-submit="saveStandaloneDocument"
      :on-load="loadStandaloneDocument"
      :on-cancel="navigateToDocumentsOverview"
    >
      <div class="row">
        <div class="col col-xs-12 col-lg-6">
          <h2>{{ $t.createStandaloneDocument.generalInformation.header() }}</h2>

          <SaFormInput
            prop="title"
            :label="$t.createStandaloneDocument.generalInformation.title.label()"
            :placeholder="$t.createStandaloneDocument.generalInformation.title.placeholder()"
          />
        </div>

        <div
          v-if="!isEditing"
          class="col col-xs-12 col-lg-6"
        >
          <h2>{{ $t.createStandaloneDocument.document.header() }}</h2>

          <SaFormDocumentsUpload
            prop="documents"
            :documents="[]"
            single
          />
        </div>
      </div>
    </SaForm>
  </SaPage>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import SaPage from '@/components/SaPage.vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaFormDocumentsUpload from '@/components/form/SaFormDocumentsUpload.vue';
  import { $t } from '@/services/i18n';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery, useMutation } from '@/services/api/use-gql-api.ts';
  import {
    AsFormValues,
    ClientSideValidationError,
    toRequestArgs,
    updateFormValues,
  } from '@/components/form/sa-form-api.ts';
  import {
    CreateStandaloneDocumentMutationVariables,
    EditStandaloneDocumentMutationVariables,
  } from '@/services/api/gql/graphql.ts';

  type StandaloneDocumentFormValues = AsFormValues<[
    CreateStandaloneDocumentMutationVariables,
    EditStandaloneDocumentMutationVariables,
  ]> & {
    documents?: string[] | null,
  };

  const props = defineProps<{
    id?: string,
  }>();

  const { currentWorkspaceId } = useCurrentWorkspace();
  const { navigateByViewName } = useNavigation();
  const navigateToDocumentsOverview = async () => {
    await navigateByViewName('documents-overview');
  };

  const getStandaloneDocumentForEditQuery = useLazyQuery(graphql(`
    query getStandaloneDocumentForEdit($workspaceId: String!, $standaloneDocumentId: String!) {
      workspace(id: $workspaceId) {
        standaloneDocument(id: $standaloneDocumentId) {
          id
          version
          title
          documentId
        }
      }
    }
  `), 'workspace');

  const createStandaloneDocumentMutation = useMutation(graphql(`
    mutation createStandaloneDocument(
      $workspaceId: String!,
      $title: String!,
      $documentId: String!
    ) {
      createStandaloneDocument(
        workspaceId: $workspaceId,
        title: $title,
        documentId: $documentId
      ) {
        id
      }
    }
  `), 'createStandaloneDocument');

  const editStandaloneDocumentMutation = useMutation(graphql(`
    mutation editStandaloneDocument(
      $workspaceId: String!,
      $id: String!,
      $version: Int!,
      $title: String!,
      $documentId: String!
    ) {
      editStandaloneDocument(
        workspaceId: $workspaceId,
        id: $id,
        version: $version,
        title: $title,
        documentId: $documentId
      ) {
        id
      }
    }
  `), 'editStandaloneDocument');

  const formValues = ref<StandaloneDocumentFormValues>({
    workspaceId: currentWorkspaceId,
    id: props.id,
    documents: [],
  });

  const isEditing = computed(() => props.id !== undefined);
  const pageHeader = computed(() => (isEditing.value
    ? $t.value.editStandaloneDocument.pageHeader()
    : $t.value.createStandaloneDocument.pageHeader()));

  const loadStandaloneDocument = props.id !== undefined ? async () => {
    const workspace = await getStandaloneDocumentForEditQuery({
      workspaceId: currentWorkspaceId,
      standaloneDocumentId: props.id!,
    });
    updateFormValues(formValues, workspace.standaloneDocument);
  } : undefined;

  const saveStandaloneDocument = async () => {
    if (isEditing.value) {
      await editStandaloneDocumentMutation(toRequestArgs(formValues));
      await navigateToDocumentsOverview();
      return;
    }

    const documentId = formValues.value.documents?.[0];
    if (!documentId) {
      throw new ClientSideValidationError([{
        field: 'documents',
        message: $t.value.createStandaloneDocument.document.errors.required(),
      }]);
    }

    formValues.value.documentId = documentId;
    await createStandaloneDocumentMutation(toRequestArgs(formValues));
    await navigateToDocumentsOverview();
  };
</script>
