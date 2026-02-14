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
        v-for="dataItem in data"
        :key="dataItem.id"
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

<script lang="ts" setup>
import { throttle } from 'lodash';
import { computed, ref, watch } from 'vue';
import SaIcon from '@/components/SaIcon.vue';
import type { ApiPage, ApiPageRequest, HasOptionalId } from '@/services/api';
import { $t } from '@/services/i18n';

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
  reloadOn?: Array<unknown>;
  pageProvider: (request: ApiPageRequest, config: RequestInit) => Promise<ApiPage<HasOptionalId>>;
}>();

const pageNumber = ref(1);
const totalElements = ref(0);
const pageSize = ref(10);
const data = ref<HasOptionalId[]>([]);

const { loading, stopLoading, startLoading } = useLoading();

let abortController: AbortController | null;

const reloadData = throttle(
  async () => {
    startLoading();

    if (abortController) {
      abortController.abort();
    }
    abortController = new AbortController();

    const request: ApiPageRequest = {
      pageNumber: pageNumber.value,
      pageSize: pageSize.value,
    };

    try {
      const response = await props.pageProvider(request, {
        signal: abortController?.signal,
      });
      totalElements.value = response.totalElements;
      data.value = response.data;

      stopLoading();
    } catch (e) {
      stopLoading();
      throw e;
    }
  },
  300,
  {
    trailing: true,
    leading: false,
  },
);

watch(
  () => [pageNumber, ...(props.reloadOn || [])],
  () => {
    reloadData();
  },
  {
    immediate: true,
    deep: true,
  },
);

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
