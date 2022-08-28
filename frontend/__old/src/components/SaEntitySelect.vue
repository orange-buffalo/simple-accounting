<template>
  <SaInputLoader
    class="sa-entity-select"
    :loading="selectedValueLoadingState.loading"
    :error="selectedValueLoadingState.error"
  >
    <VSelect
      v-model="selectedValue"
      :placeholder="placeholder"
      :options="availableValues"
      :filterable="false"
      :get-option-label="(entity) => labelProvider(entity)"
      @search="executeSearch"
    >
      <template #selected-option="entity">
        {{ labelProvider(entity) }}
      </template>

      <template #option="entity">
        <slot :entity="entity">
          {{ labelProvider(entity) }}
        </slot>
      </template>

      <template #list-footer>
        <div
          v-if="searchRequestState.error"
          class="sa-entity-select__list-footer"
        >
          <SaBasicErrorMessage />
        </div>

        <div
          v-else-if="searchRequestState.loading"
          class="sa-entity-select__list-footer"
        >
          <SaStatusLabel
            status="regular"
            simplified
            custom-icon="loading"
          >
            {{ $t.saEntitySelect.loading.text() }}
          </SaStatusLabel>
        </div>

        <div
          v-else-if="availableValues.length === 0"
          class="sa-entity-select__list-footer"
        >
          {{ $t.saEntitySelect.noData.text() }}
        </div>

        <div
          v-else-if="totalSearchResultsCount > availableValues.length"
          class="sa-entity-select__list-footer sa-entity-select__list-footer--dimmed"
        >
          {{ $t('saEntitySelect.moreElements.text', [totalSearchResultsCount - availableValues.length]) }}
        </div>
      </template>

      <template #no-options>
        <span />
      </template>

      <template #open-indicator>
        <i class="el-icon-arrow-down vs__open-indicator" />
      </template>
    </VSelect>
  </SaInputLoader>
</template>

<script>
  import VSelect from 'vue-select';
  import { ref, watch } from '@vue/composition-api';
  import { api } from '@/services/api-legacy';
  import SaInputLoader from '@/components/SaInputLoader';
  import SaBasicErrorMessage from '@/components/SaBasicErrorMessage';
  import SaStatusLabel from '@/components/SaStatusLabel';
  import { useCurrentWorkspace } from '@/services/workspaces';

  const maxItemsToDisplay = 10;
  const itemsToDisplay = 5;

  function useSearchApi(props) {
    let cancelToken = null;

    const searchRequestState = ref({
      loading: false,
      error: false,
    });

    const searchResults = ref({
      data: [],
    });

    async function executeSearch(query, pageSize) {
      if (cancelToken) {
        cancelToken.cancel();
      }

      searchRequestState.value.loading = true;
      searchResults.value = {
        data: [],
      };

      cancelToken = api.createCancelToken();

      const { currentWorkspaceId } = useCurrentWorkspace();

      let request = api
        .pageRequest(`/workspaces/${currentWorkspaceId}/${props.entityPath}`)
        .config({
          cancelToken: cancelToken.token,
          skipGlobalErrorHandler: true,
        });
      if (query != null && query !== '') {
        request = request.eqFilter(props.searchQueryParamName, query);
      }
      request = request.limit(pageSize || itemsToDisplay);

      try {
        searchResults.value = await request.getPage();
        searchRequestState.value.loading = false;
        cancelToken = null;
      } catch (thrown) {
        if (!api.isCancel(thrown)) {
          searchRequestState.value = {
            loading: false,
            error: true,
          };
          cancelToken = null;
        }
      }
    }

    executeSearch();

    return {
      searchRequestState,
      executeSearch,
      searchResults,
    };
  }

  function useSelectedValue(props, emit) {
    const selectedValue = ref(null);
    const selectedValueLoadingState = ref({
      loading: false,
      error: false,
    });

    // optimization to avoid API lookups if we loaded the value previously
    const knownSelectedValues = [];

    async function loadSelectedValue(entityId) {
      selectedValueLoadingState.value.error = false;
      const knownValue = knownSelectedValues.find((it) => it.id === entityId);
      if (knownValue) {
        selectedValue.value = knownValue;
      } else {
        selectedValueLoadingState.value.loading = true;
        const { currentWorkspaceId } = useCurrentWorkspace();
        try {
          const response = await api.get(`/workspaces/${currentWorkspaceId}/${props.entityPath}/${entityId}`, {
            skipGlobalErrorHandler: true,
          });
          selectedValue.value = response.data;
        } catch (e) {
          selectedValueLoadingState.value.error = true;
        } finally {
          selectedValueLoadingState.value.loading = false;
        }
      }
    }

    watch(() => props.value, (newValue) => {
      if (newValue == null) {
        selectedValue.value = null;
      } else {
        loadSelectedValue(newValue);
      }
    }, { immediate: true });

    watch(selectedValue, (newValue) => {
      if (newValue === null) {
        emit('input', null);
      } else {
        knownSelectedValues.push(newValue);
        emit('input', newValue.id);
      }
    }, { immediate: false });

    return {
      selectedValue,
      selectedValueLoadingState,
    };
  }

  function useSelectSetup({
    searchResults, searchRequestState,
  }) {
    const availableValues = ref([]);
    const totalSearchResultsCount = ref(0);

    watch(searchResults, () => {
      let data = searchResults.value.data.slice();
      totalSearchResultsCount.value = searchResults.value.totalElements;

      if (totalSearchResultsCount.value > maxItemsToDisplay) {
        data = data.slice(0, itemsToDisplay);
      }

      if (!searchRequestState.value.error) {
        availableValues.value = data;
      }
    }, { immediate: false });

    return {
      availableValues,
      totalSearchResultsCount,
    };
  }

  export default {
    components: {
      SaStatusLabel,
      SaBasicErrorMessage,
      SaInputLoader,
      VSelect,
    },

    props: {
      entityPath: {
        type: String,
        required: true,
      },
      labelProvider: {
        type: Function,
        required: true,
      },
      value: {
        type: Number,
        default: null,
      },
      placeholder: {
        type: String,
        default: null,
      },
      searchQueryParamName: {
        type: String,
        default: 'freeSearchText',
      },
    },

    setup(props, { emit }) {
      const {
        searchResults,
        executeSearch,
        searchRequestState,
      } = useSearchApi(props);

      return {
        ...useSelectSetup({
          searchResults,
          searchRequestState,
        }),
        ...useSelectedValue(props, emit),
        executeSearch,
        searchRequestState,
      };
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";
  @import "~@/styles/mixins.scss";

  .sa-entity-select {

    &__list-footer {
      display: flex;
      height: 34px;
      align-items: center;
      justify-content: center;
      font-size: 90%;
      padding: 15px;

      &--dimmed {
        color: $secondary-grey-darker-i;
        font-weight: bold;
      }

      .sa-status-label {
        max-width: 100%;
        overflow: hidden;

        .sa-status-label__icon {
          min-width: 16px;
          min-height: 16px;
        }
      }
    }
  }
</style>
