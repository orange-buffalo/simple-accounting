<template>
  <div class="sa-pageable-items">
    <ElPagination
      v-if="paginatorVisible"
      :current-page.sync="pageNumber"
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
        class="col col-xs-12"
      >
        <slot :item="dataItem" />
      </div>
    </div>

    <div
      v-if="totalElements === 0 && !loading"
      class="sa-pageable-items__empty-results"
    >
      <SaIcon
        icon="empty-box"
        class="sa-pageable-items__empty-results__icon"
      />
      <span>No results here</span>
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
      :current-page.sync="pageNumber"
      :page-size="pageSize"
      layout="prev, pager, next"
      :total="totalElements"
    />
  </div>
</template>

<script lang="ts">
  import { computed, defineComponent, PropType } from '@vue/composition-api';
  import { PageableItems } from '@/components/data/pageableItems';
  import { HasOptionalId } from '@/services/api';
  import SaIcon from '@/components/SaIcon';

  export default defineComponent({
    components: { SaIcon },

    props: {
      items: {
        type: Object as PropType<PageableItems<HasOptionalId>>,
        required: true,
      },
    },

    setup(props) {
      const paginatorVisible = computed(() => props.items.totalElements.value > 0 && !props.items.loading.value);

      return {
        ...props.items,
        paginatorVisible,
      };
    },
  });
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";
  @import "~@/styles/mixins.scss";

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
