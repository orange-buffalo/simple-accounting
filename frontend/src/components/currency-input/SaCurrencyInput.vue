<template>
  <ElSelect
    v-model="inputValue"
    filterable
    :disabled="disabled"
    :loading="loading"
  >
    <ElOptionGroup
      v-for="group in currenciesGroups"
      :key="group.title"
      :label="group.title"
    >
      <ElOption
        v-for="currency in group.currencies"
        :key="currency.code"
        :label="$t.saCurrencyInput.currencyLabel(currency)"
        :value="currency.code"
      >
        <div class="sa-currency-input">
          <span class="sa-currency-input__currency-code">{{ currency.code }}</span>
          <span class="sa-currency-input__currency-name">{{ currency.displayName }}</span>
        </div>
      </ElOption>
    </ElOptionGroup>
  </ElSelect>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue';
import { statisticsApi } from '@/services/api';
import type { CurrencyInfo } from '@/services/i18n';
import { $t, getCurrenciesInfo } from '@/services/i18n';
import { useCurrentWorkspace } from '@/services/workspaces';

const props = defineProps<{
  modelValue?: string;
  disabled?: boolean;
}>();

const emit = defineEmits<(e: 'update:modelValue', value: string) => void>();

const currenciesShortlist = ref<CurrencyInfo[]>([]);
const currencies = getCurrenciesInfo();
const loading = ref(true);

async function loadShortlist() {
  const { currentWorkspaceId } = useCurrentWorkspace();
  const shortlistedCurrencyCodes = await statisticsApi.getCurrenciesShortlist({
    workspaceId: currentWorkspaceId,
  });
  currenciesShortlist.value = shortlistedCurrencyCodes.map((currencyCode) => currencies[currencyCode]);
  loading.value = false;
}

const currenciesGroups = computed(() => {
  const groups: Array<{
    title: string;
    currencies: CurrencyInfo[];
  }> = [];
  if (currenciesShortlist.value.length > 0) {
    groups.push({
      title: $t.value.saCurrencyInput.groups.recent(),
      currencies: currenciesShortlist.value,
    });
  }
  groups.push({
    title: $t.value.saCurrencyInput.groups.all(),
    currencies: Object.values(currencies),
  });
  return groups;
});

const { defaultCurrency } = useCurrentWorkspace();
const inputValue = ref(props.modelValue || defaultCurrency);
watch(
  () => props.modelValue,
  (newValue) => {
    inputValue.value = newValue || defaultCurrency;
  },
);
watch(inputValue, (newValue) => {
  emit('update:modelValue', newValue);
});

onMounted(() => loadShortlist());
</script>

<style lang="scss">
  .sa-currency-input {
    &__currency-code {
      font-size: 110%;
      display: inline-block;
      min-width: 3em;
    }

    &__currency-name {
      display: inline-block;
      margin-left: 5px;
      color: #6e716f;
      font-size: 90%;
    }
  }
</style>
