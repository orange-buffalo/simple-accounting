<template>
  <div class="sa-pageable-items">
    <ElPagination
      v-if="paginatorVisible"
      v-model:current-page="pageNumber"
      :page-size="pageSize"
      layout="prev, pager, next"
      :total="totalElements"
    />

    <div
      v-if="!loading"
      class="row"
    >
      <div
        v-for="(dataItem, index) in data"
        :key="index"
        class="col col-xs-12 sa-pageable-items__item"
      >
        <slot :item="dataItem">{{ dataItem }}</slot>
      </div>
    </div>

    <div
      v-if="totalElements === 0 && !loading"
      class="sa-pageable-items__empty-results"
    >
      <!--suppress HtmlUnknownTag -->
      <SaIcon
        icon="empty-box"
        :size="48"
        class="sa-pageable-items__empty-results__icon"
      />
      <span>{{ $t.saPageableItems.emptyResults() }}</span>
    </div>

    <div
      v-if="loading"
      class="col"
    >
      <div class="col col-xs-12 sa-pageable-items__loader-item" />
      <div class="col col-xs-12 sa-pageable-items__loader-item" />
      <div class="col col-xs-12 sa-pageable-items__loader-item" />
      <div class="col col-xs-12 sa-pageable-items__loader-item" />
      <div class="col col-xs-12 sa-pageable-items__loader-item" />
    </div>

    <ElPagination
      v-if="paginatorVisible"
      v-model:current-page="pageNumber"
      :page-size="pageSize"
      layout="prev, pager, next"
      :total="totalElements"
    />
  </div>
</template>

<script
  lang="ts"
  setup
  generic="TNode, TPath extends string"
>
  import {
    computed, ref, shallowRef, watch,
  } from 'vue';
  import { throttle } from 'lodash';
  import type { TypedDocumentNode } from '@graphql-typed-document-node/core';
  import SaIcon from '@/components/SaIcon.vue';
  import { $t } from '@/services/i18n';
  import { useLazyQuery } from '@/services/api/use-gql-api';

  type GqlConnectionOf<N> = {
    edges: Array<{ node: N; cursor: string }>;
    pageInfo: {
      endCursor?: string | null;
      hasNextPage: boolean;
      hasPreviousPage: boolean;
      startCursor?: string | null;
    };
    totalCount: number;
  };

  type PaginationVariables = {
    first: number;
    after?: string | null;
  };

  function useLoading() {
    let loadingRequestsCount = 0;
    const loading = ref(true);
    const updateLoading = throttle(
      () => {
        loading.value = loadingRequestsCount > 0;
      },
      200,
      {
        leading: false,
        trailing: true,
      },
    );

    const startLoading = () => {
      loadingRequestsCount += 1;
      updateLoading();
    };

    const stopLoading = () => {
      loadingRequestsCount = Math.max(0, loadingRequestsCount - 1);
      updateLoading();
    };

    return {
      loading,
      startLoading,
      stopLoading,
    };
  }

  const props = defineProps<{
    pageQuery: TypedDocumentNode<Record<TPath, GqlConnectionOf<TNode>>, PaginationVariables>,
    path: TPath,
    reloadOn?: Array<unknown>,
  }>();

  defineSlots<{
    default(props: { item: TNode }): unknown;
  }>();

  const pageNumber = ref(1);
  const totalElements = ref(0);
  const pageSize = ref(10);
  const data = shallowRef<TNode[]>([]);

  const cursorsByPage = new Map<number, string>();

  const {
    loading,
    stopLoading,
    startLoading,
  } = useLoading();

  const executeQuery = useLazyQuery(
    props.pageQuery,
    props.path as string & keyof Record<TPath, GqlConnectionOf<TNode>>,
  );

  const reloadData = throttle(async () => {
    startLoading();

    const after = pageNumber.value === 1
      ? null
      : cursorsByPage.get(pageNumber.value - 1) ?? null;

    try {
      const connection = await executeQuery({
        first: pageSize.value,
        after,
      } as PaginationVariables);

      data.value = connection.edges.map((edge) => edge.node);
      totalElements.value = connection.totalCount;

      if (connection.pageInfo.endCursor) {
        cursorsByPage.set(pageNumber.value, connection.pageInfo.endCursor);
      }

      stopLoading();
    } catch (e) {
      stopLoading();
      throw e;
    }
  }, 300, {
    trailing: true,
    leading: false,
  });

  watch(() => [pageNumber, ...(props.reloadOn || [])], () => {
    reloadData();
  }, {
    immediate: true,
    deep: true,
  });

  const paginatorVisible = computed(() => totalElements.value > 0 && !loading.value);

  defineExpose({
    reload: reloadData,
  });
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .sa-pageable-items {
    .el-pagination {
      text-align: right;

      .btn-prev, .btn-next, .el-pager li {
        background-color: transparent;
      }
    }

    &__empty-results {
      display: flex;
      flex-flow: column;
      align-items: center;
      color: $primary-color-lighter-iii;

      &__icon {
        margin: 10px;
      }
    }

    &__loader-item {
      height: 120px;
      margin-bottom: 20px;
      border-radius: 5px;
      border: $secondary-grey solid 1px;
      animation: sa-pageable-items-animation 1.4s linear infinite;
      background: $white;
    }
  }

  @keyframes sa-pageable-items-animation {
    0% {
      opacity: 0.5
    }
    50% {
      opacity: 0.8;
    }
    100% {
      opacity: 0.5;
    }
  }
</style>
