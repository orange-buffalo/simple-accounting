<template>
  <div class="data-items">
    <ElPagination
      v-if="paginatorVisible"
      :current-page.sync="currentPage"
      :page-size="pageSize"
      layout="prev, pager, next"
      :total="totalElements"
      @current-change="onCurrentPageChange"
    />

    <div
      v-if="!loading"
      class="row"
    >
      <div
        v-for="dataItem in data"
        :key="dataItem.id"
        class="col col-xs-12"
      >
        <slot :item="dataItem" />
      </div>
    </div>

    <div
      v-if="totalElements === 0 && !loading"
      class="data-items__empty-results"
    >
      <SaIcon
        icon="empty-box"
        class="data-items__empty-results__icon"
      />
      <span>No results here</span>
    </div>

    <div
      v-if="loading"
      class="col"
    >
      <div class="col col-xs-12 data-items__loader-item" />
      <div class="col col-xs-12 data-items__loader-item" />
      <div class="col col-xs-12 data-items__loader-item" />
      <div class="col col-xs-12 data-items__loader-item" />
      <div class="col col-xs-12 data-items__loader-item" />
    </div>

    <ElPagination
      v-if="paginatorVisible"
      :current-page.sync="currentPage"
      :page-size="pageSize"
      layout="prev, pager, next"
      :total="totalElements"
      @current-change="onCurrentPageChange"
    />
  </div>
</template>

<script>
  import throttle from 'lodash/throttle';
  import { api } from '@/services/api-legacy';
  import SaIcon from '@/components/SaIcon';

  export default {
    name: 'DataItems',

    components: { SaIcon },

    props: {
      apiPath: {
        type: String,
        required: true,
      },

      filters: {
        type: Object,
        default: null,
      },

      paginator: {
        type: Boolean,
        default: true,
      },
    },

    data() {
      return {
        totalElements: 0,
        data: [],
        pageSize: 10,
        currentPage: 1,
        loading: false,
      };
    },

    computed: {
      paginatorVisible() {
        return this.paginator && this.totalElements > 0 && !this.loading;
      },
    },

    watch: {
      apiPath() {
        this.reloadData();
      },

      filters: {
        handler() {
          this.onFilterChange();
        },
        deep: true,
      },
    },

    created() {
      this.onFilterChange = throttle(this.reloadData, 500, {
        trailing: true,
        leading: false,
      });
      this.loadingRequestsCount = 0;
      this.updateLoading = throttle(
        () => {
          this.loading = this.loadingRequestsCount > 0;
        },
        400,
        {
          leading: false,
          trailing: true,
        },
      );

      this.reloadData();
    },

    methods: {
      startLoading() {
        this.loadingRequestsCount += 1;
        this.updateLoading();
      },

      stopLoading() {
        this.loadingRequestsCount = Math.max(0, this.loadingRequestsCount - 1);
        this.updateLoading();
      },

      onCurrentPageChange() {
        this.reloadData();
      },

      async reloadData() {
        this.startLoading();

        if (this.cancelToken) {
          this.cancelToken.cancel();
        }

        this.cancelToken = api.createCancelToken();

        const pageRequest = api.pageRequest(this.apiPath)
          .limit(this.paginator ? this.pageSize : 500)
          .page(this.paginator ? this.currentPage : 1)
          .config({
            cancelToken: this.cancelToken.token,
          });

        if (this.filters) {
          this.filters.applyToRequest(pageRequest);
        }

        try {
          const page = await pageRequest.getPage();

          this.data = page.data;
          this.totalElements = page.totalElements;
          this.cancelToken = null;

          this.stopLoading();
        } catch (e) {
          this.stopLoading();
          if (!api.isCancel(e)) {
            // todo #72: proper error handling
            throw e;
          }
        }
      },
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";
  @import "~@/styles/mixins.scss";

  .data-items {
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
        width: 48px;
        height: 48px;
        margin: 10px;
      }
    }

    &__loader-item {
      height: 90px;
      box-sizing: content-box;
      margin-bottom: 20px;
      border-radius: 5px;
      border: $secondary-grey solid 1px;
      animation: data-items-animation 1.4s linear infinite;
      background: $white;
    }
  }

  @keyframes data-items-animation {
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
