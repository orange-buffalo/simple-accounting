<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ headerText }}</h1>

      <div class="sa-header-options">
        <slot name="header-options-left">
          <span />
        </slot>

        <ElButton
          round
          @click="$emit('create')"
        >
          <SaIcon icon="plus-thin" />
          {{ createButtonText }}
        </ElButton>
      </div>
    </div>

    <SaPageableItemsGql
      ref="pageableItems"
      :page-query="pageQuery"
      :path="path"
      :page-query-arguments="pageQueryArguments"
      #default="{ item }"
    >
      <slot :item="item" />
    </SaPageableItemsGql>
  </div>
</template>

<script
  lang="ts"
  setup
  generic="TNode, TPath extends string, TVariables extends GqlPaginationVariables = GqlPaginationVariables"
>
  import { ref } from 'vue';
  import type { TypedDocumentNode } from '@graphql-typed-document-node/core';
  import SaIcon from '@/components/SaIcon.vue';
  import SaPageableItemsGql from '@/components/pageable-items/SaPageableItemsGql.vue';
  import type { GqlConnection, GqlPaginationVariables } from '@/components/pageable-items/pageable-items-gql-types';

  type ExtraArgs<T extends GqlPaginationVariables> = Omit<T, keyof GqlPaginationVariables>;

  defineProps<{
    headerText: string,
    createButtonText: string,
    pageQuery: TypedDocumentNode<Record<TPath, GqlConnection<TNode>>, TVariables>,
    path: TPath,
    pageQueryArguments?: ExtraArgs<TVariables>,
  }>();

  defineEmits<{
    (e: 'create'): void,
  }>();

  defineSlots<{
    default(props: { item: TNode }): unknown,
    'header-options-left'(props: Record<string, never>): unknown,
  }>();

  const pageableItems = ref<{ reload: () => void }>();

  defineExpose({
    reload: () => pageableItems.value?.reload(),
  });
</script>
