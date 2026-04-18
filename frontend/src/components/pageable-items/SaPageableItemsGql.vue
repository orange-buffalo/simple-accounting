<template>
  <div class="sa-pageable-items">
    <ElPagination
      v-if="paginatorVisible"
      v-model:current-page="pageNumber"
      :page-size="pageSize"
      layout="prev, slot, next"
      :total="totalElements"
    >
      <span class="sa-pageable-items__page-indicator">
        {{ $t.saPageableItems.pageIndicator(pageNumber, totalPages) }}
      </span>
    </ElPagination>

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
      layout="prev, slot, next"
      :total="totalElements"
    >
      <span class="sa-pageable-items__page-indicator">
        {{ $t.saPageableItems.pageIndicator(pageNumber, totalPages) }}
      </span>
    </ElPagination>
  </div>
</template>

<script
  lang="ts"
  setup
  generic="TResponse, TPath extends string, TVariables extends PageVars = PageVars"
>
  import {
    computed, onBeforeUnmount, ref, shallowRef, watch,
  } from 'vue';
  import { throttle } from 'lodash';
  import type { TypedDocumentNode } from '@graphql-typed-document-node/core';
  import SaIcon from '@/components/SaIcon.vue';
  import { $t } from '@/services/i18n';
  import { useLazyQuery } from '@/services/api/use-gql-api';
  import { useFragment } from '@/services/api/gql/fragment-masking';
  import {
    PaginationPageInfoFragment,
    type GqlConnection,
    type GqlPaginationVariables as PageVars,
    type NodeOf,
    type DeepAccess,
  } from '@/components/pageable-items/pageable-items-gql-types';

  type ExtraArgs<T extends PageVars> = Omit<T, keyof PageVars>;
  type RootKey<P extends string> = P extends `${infer K}.${string}` ? K : P;
  type TNode = NodeOf<DeepAccess<TResponse, TPath>>;

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
    pageQuery: TypedDocumentNode<TResponse, TVariables>,
    path: TPath,
    pageQueryArguments?: ExtraArgs<TVariables>,
  }>();

  defineSlots<{
    default(props: { item: TNode }): unknown;
  }>();

  const pageNumber = ref(1);
  const totalElements = ref(0);
  const pageSize = ref(10);
  const data = shallowRef<TNode[]>([]);

  const {
    loading,
    stopLoading,
    startLoading,
  } = useLoading();

  const totalPages = computed(() => Math.ceil(totalElements.value / pageSize.value));

  const pathParts = props.path.split('.');
  const rootKey = pathParts[0] as RootKey<TPath> & keyof TResponse;
  const subPath = pathParts.slice(1);

  const executeQuery = useLazyQuery(
    props.pageQuery,
    rootKey,
  );

  // Stores the endCursor from each visited page for backward navigation.
  // endCursors[0] = endCursor of page 1, endCursors[1] = endCursor of page 2, etc.
  let endCursors: string[] = [];

  const reloadData = throttle(async () => {
    startLoading();

    const after = pageNumber.value === 1
      ? null
      : endCursors[pageNumber.value - 2] ?? null;

    try {
      const queryResult = await executeQuery({
        ...props.pageQueryArguments,
        first: pageSize.value,
        after,
      } as TVariables);

      let connectionValue: unknown = queryResult;
      for (const key of subPath) {
        connectionValue = (connectionValue as Record<string, unknown>)[key];
      }
      const connection = connectionValue as GqlConnection<TNode>;

      data.value = connection.edges.map((edge) => edge.node);
      totalElements.value = connection.totalCount;

      const pageInfo = useFragment(PaginationPageInfoFragment, connection.pageInfo);
      if (pageInfo.endCursor) {
        endCursors[pageNumber.value - 1] = pageInfo.endCursor;
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

  watch(() => props.pageQueryArguments, () => {
    endCursors = [];
    pageNumber.value = 1;
    reloadData();
  }, {
    deep: true,
  });

  watch(pageNumber, () => {
    reloadData();
  }, {
    immediate: true,
  });

  onBeforeUnmount(() => {
    reloadData.cancel();
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
