<template>
  <SaInputLoader
    class="sa-entity-select"
    :loading="selectedValueLoadingState.loading"
    :error="selectedValueLoadingState.error"
  >
    <ElSelect
      v-model="selectedValue"
      :placeholder="placeholder"
      :loading="loading"
      remote
      filterable
      :remote-method="executeSearch"
      :loading-text="$t.saEntitySelect.loading.text()"
      :no-data-text="$t.saEntitySelect.noData.text()"
      :no-match-text="$t.saEntitySelect.noData.text()"
      remote-show-suffix
      :teleported="false"
      :clearable="clearable"
    >
      <ElOption
        v-for="availableValue in availableValues"
        :key="availableValue.key"
        :label="availableValue.label"
        :value="availableValue.key"
        :disabled="availableValue.isInfo"
      >
        <div v-if="!availableValue.isInfo">
          <slot :entity="ensureDefined(availableValue.entity)">
            <div class="sa-entity-select__simple-option">{{ availableValue.label }}</div>
          </slot>
        </div>

        <div
          v-else-if="availableValue.error"
          class="sa-entity-select__list-footer"
        >
          <SaBasicErrorMessage />
        </div>

        <div
          v-else
          class="sa-entity-select__list-footer sa-entity-select__list-footer--dimmed"
        >
          {{ availableValue.label }}
        </div>
      </ElOption>
    </ElSelect>
  </SaInputLoader>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue';
import SaBasicErrorMessage from '@/components/SaBasicErrorMessage.vue';
import SaInputLoader from '@/components/SaInputLoader.vue';
import type { ApiPage, ApiPageRequest, HasOptionalId, RequestConfigReturn } from '@/services/api';
import { useRequestConfig } from '@/services/api';
import { $t } from '@/services/i18n';
import { ensureDefined } from '@/services/utils';

const itemsToDisplay = 10;

const props = defineProps<{
  labelProvider: (option: HasOptionalId) => string;
  modelValue?: number;
  placeholder?: string;
  optionsProvider: (
    pageRequest: ApiPageRequest,
    query: string | undefined,
    requestInit: RequestInit,
  ) => Promise<ApiPage<HasOptionalId>>;
  optionProvider: (id: number, requestInit: RequestInit) => Promise<HasOptionalId>;
  clearable?: boolean;
}>();

const emit = defineEmits<(e: 'update:modelValue', value?: number) => void>();

let requestConfigData: RequestConfigReturn | undefined;

const loading = ref(false);

interface ListItem {
  label?: string;
  isInfo?: boolean;
  error?: boolean;
  key: number | string;
  entity?: HasOptionalId;
}

const availableValues = ref<Array<ListItem>>([]);

const executeSearch = async (query?: string) => {
  if (requestConfigData) {
    requestConfigData.cancelRequest();
  }
  loading.value = true;
  requestConfigData = useRequestConfig({});
  try {
    const providerData = await props.optionsProvider(
      {
        pageSize: itemsToDisplay,
      },
      query,
      requestConfigData.requestConfig,
    );

    availableValues.value = providerData.data.map((entity) => ({
      entity,
      key: ensureDefined(entity.id),
      label: props.labelProvider(entity),
    }));
    if (providerData.totalElements > itemsToDisplay) {
      availableValues.value.push({
        key: 'overflow',
        isInfo: true,
        label: $t.value.saEntitySelect.moreElements.text(providerData.totalElements - itemsToDisplay),
      });
    }
  } catch (_) {
    // TODO #458 cancellation
    // if (!api.isCancel(thrown)) {
    //   availableValues.value = [];
    //   searchRequestState.value.error = true;
    // }
    availableValues.value = [
      {
        key: 'loadingError',
        error: true,
        isInfo: true,
      },
    ];
  } finally {
    requestConfigData = undefined;
    loading.value = false;
  }
};

const selectedValue = ref<number | string | undefined>();
const selectedValueLoadingState = ref({
  loading: false,
  error: false,
});

// optimization to avoid API lookups if we loaded the value previously
const knownSelectedValues: Array<HasOptionalId> = [];

async function loadSelectedValue(entityId: number) {
  selectedValueLoadingState.value.error = false;
  const knownValue = knownSelectedValues.find((it) => it.id === entityId);
  if (knownValue) {
    selectedValue.value = props.labelProvider(knownValue);
  } else {
    selectedValueLoadingState.value.loading = true;
    try {
      const initialEntity = await props.optionProvider(entityId, {});
      selectedValue.value = props.labelProvider(initialEntity);
    } catch (_) {
      selectedValueLoadingState.value.error = true;
    } finally {
      selectedValueLoadingState.value.loading = false;
    }
  }
}

watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue === undefined) {
      selectedValue.value = undefined;
    } else {
      loadSelectedValue(newValue);
    }
  },
  { immediate: true },
);

watch(
  () => selectedValue.value,
  (entityIdOrLabel) => {
    if (entityIdOrLabel === undefined) {
      emit('update:modelValue', undefined);
    } else if (typeof entityIdOrLabel === 'number') {
      const selectedOption = availableValues.value.find((it) => it.key === entityIdOrLabel);
      if (selectedOption) {
        knownSelectedValues.push(ensureDefined(selectedOption.entity));
        emit('update:modelValue', selectedOption.entity?.id);
      }
    }
  },
);
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .sa-entity-select {
    .el-select {
      width: 100%;
    }

    .el-select-dropdown__item {
      height: auto;
      min-height: 34px;
      line-height: inherit;
    }

    &__simple-option {
      height: 34px;
      line-height: 34px;
    }

    &__list-footer {
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 90%;
      height: 34px;

      &--dimmed {
        color: $secondary-grey-darker-i;
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
