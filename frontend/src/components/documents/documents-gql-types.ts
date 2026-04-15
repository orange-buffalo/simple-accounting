import { graphql } from '@/services/api/gql';
import type { FragmentType } from '@/services/api/gql/fragment-masking';

export const DocumentDataFragment = graphql(`
  fragment DocumentData on Document {
    id
    name
    sizeInBytes
    storageId
  }
`);

export type DocumentDataFragmentType = FragmentType<typeof DocumentDataFragment>;
