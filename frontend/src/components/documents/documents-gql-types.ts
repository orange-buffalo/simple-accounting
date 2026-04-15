import { ref } from 'vue';
import { graphql } from '@/services/api/gql';
import { type FragmentType, useFragment } from '@/services/api/gql/fragment-masking';

export const DocumentDataFragment = graphql(`
  fragment DocumentData on Document {
    id
    name
    sizeInBytes
    storageId
  }
`);

export type DocumentDataFragmentType = FragmentType<typeof DocumentDataFragment>;

export function getDocumentIds(attachments: ReadonlyArray<DocumentDataFragmentType>): number[] {
  return attachments.map(a => useFragment(DocumentDataFragment, a).id);
}

export function useDocumentAttachments() {
  const resolvedDocuments = ref<DocumentDataFragmentType[]>([]);

  function setDocuments(attachments: ReadonlyArray<DocumentDataFragmentType>): number[] {
    resolvedDocuments.value = [...attachments];
    return getDocumentIds(attachments);
  }

  return {
    resolvedDocuments,
    setDocuments,
  };
}
