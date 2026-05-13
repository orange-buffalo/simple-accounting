<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.createStandaloneDocument.pageHeader() }}</h1>
    </div>

    <SaForm
      v-model="formValues"
      :on-submit="saveStandaloneDocument"
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

        <div class="col col-xs-12 col-lg-6">
          <h2>{{ $t.createStandaloneDocument.document.header() }}</h2>

          <SaFormDocumentsUpload
            prop="documents"
            :documents="[]"
            single
          />
        </div>
      </div>
    </SaForm>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaFormDocumentsUpload from '@/components/form/SaFormDocumentsUpload.vue';
  import { $t } from '@/services/i18n';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api.ts';
  import { AsFormValues, ClientSideValidationError } from '@/components/form/sa-form-api.ts';
  import { CreateStandaloneDocumentMutationVariables } from '@/services/api/gql/graphql.ts';

  type CreateStandaloneDocumentFormValues = AsFormValues<[CreateStandaloneDocumentMutationVariables]> & {
    documents?: string[] | null,
  };

  const { currentWorkspaceId } = useCurrentWorkspace();
  const { navigateByViewName } = useNavigation();
  const navigateToDocumentsOverview = async () => {
    await navigateByViewName('documents-overview');
  };

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

  const formValues = ref<CreateStandaloneDocumentFormValues>({
    workspaceId: currentWorkspaceId,
    documents: [],
  });

  const saveStandaloneDocument = async () => {
    const documentId = formValues.value.documents?.[0];
    if (!documentId) {
      throw new ClientSideValidationError([{
        field: 'documents',
        message: $t.value.createStandaloneDocument.document.errors.required(),
      }]);
    }

    await createStandaloneDocumentMutation({
      workspaceId: currentWorkspaceId,
      title: formValues.value.title ?? '',
      documentId,
    });
    await navigateToDocumentsOverview();
  };
</script>
