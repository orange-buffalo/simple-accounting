<template>
  <div class="data-items">
    <ElPagination
      v-if="paginator && totalElements > 0"
      :current-page.sync="currentPage"
      :page-size="pageSize"
      layout="prev, pager, next"
      :total="totalElements"
      @current-change="onCurrentPageChange"
    />

    <ElRow
      :gutter="10"
      v-bind="$props"
      :class="loading ? 'loading' : ''"
    >
      <ElCol
        v-for="dataItem in data"
        :key="dataItem.id"
        v-bind="$props"
      >
        <slot :item="dataItem" />
      </ElCol>
    </ElRow>

    <div
      v-if="totalElements === 0 && !loading"
      class="empty-results"
    >
      <SaIcon icon="empty-box" />
      <span>No results here</span>
    </div>

    <div
      v-if="totalElements === 0 && loading"
      class="loading-bar"
    >
      <i class="el-icon-loading" />
      <span>Loading..</span>
    </div>

    <ElPagination
      v-if="paginator && totalElements > 0"
      :current-page.sync="currentPage"
      :page-size="pageSize"
      layout="prev, pager, next"
      :total="totalElements"
      @current-change="onCurrentPageChange"
    />
  </div>
</template>

<script>
  import { api } from '@/services/api';
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

    watch: {
      apiPath() {
        this.reloadData();
      },

      filters: {
        handler() {
          this.reloadData();
        },
        deep: true,
      },
    },

    created() {
      this.reloadData();
    },

    methods: {
      onCurrentPageChange() {
        this.reloadData();
      },

      async reloadData() {
        this.loading = true;

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
          this.loading = false;
        } catch (e) {
          if (!api.isCancel(e)) {
            this.loading = false;
            // todo #72: proper error handling
            throw e;
          }
        }
      },
    },
  };
</script>

<style lang="scss">
  /*todo #73: common component refers to app styles - redesign dependencies  */
  @import "@/styles/vars.scss";

  /*todo #74: BEM notation*/
  .data-items {
    .el-row {
      transition: opacity 0.3s;

      &.loading {
        opacity: 0.5;
      }
    }

    .el-col {
      margin-bottom: 20px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .el-pagination {
      text-align: right;

      .btn-prev, .btn-next, .el-pager li {
        background-color: transparent;
      }
    }

    .empty-results {
      display: flex;
      flex-flow: column;
      align-items: center;
      color: $primary-color-lighter-iii;

      .svg-icon {
        width: 48px;
        height: 48px;
        margin: 10px;
      }
    }

    .loading-bar {
      display: flex;
      flex-flow: column;
      align-items: center;
      color: $primary-color-lighter-iii;

      i {
        font-size: 300%;
        margin: 10px;
      }
    }
  }
</style>
