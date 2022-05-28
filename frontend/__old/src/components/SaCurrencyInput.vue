<template>
  <ElSelect
    v-model="inputValue"
    filterable
    :placeholder="placeholder"
    :disabled="disabled"
  >
    <ElOptionGroup
      v-for="group in currenciesGroups"
      :key="group.title"
      :label="group.title"
    >
      <ElOption
        v-for="currency in group.currencies"
        :key="currency.code"
        :label="$t('saCurrencyInput.currencyLabel', currency)"
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

<script lang="ts">
  import {
    computed, defineComponent, Ref, ref, watch,
  } from '@vue/composition-api';
  import i18n from '@/services/i18n';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { apiClient } from '@/services/api';

  // todo: types once i18n migrated to ts
  async function loadShortlist(currenciesShortlist: Ref<Array<any>>, currencies: any) {
    const { currentWorkspaceId } = useCurrentWorkspace();
    const response = await apiClient.getCurrenciesShortlist({
      workspaceId: currentWorkspaceId,
    });
    // eslint-disable-next-line no-param-reassign
    currenciesShortlist.value = response.data
      .map((currencyCode) => currencies[currencyCode]);
  }

  export default defineComponent({
    props: {
      value: {
        type: String,
        default: null,
      },
      placeholder: {
        type: String,
        default: null,
      },
      disabled: {
        type: Boolean,
        default: false,
      },
    },

    setup(props, { emit }) {
      // todo: types when i18n is migrated to ts
      const currenciesShortlist = ref([]);
      const currencies = (i18n as any).getCurrenciesInfo();

      const currenciesGroups = computed(() => {
        const groups = [];
        if (currenciesShortlist.value.length > 0) {
          groups.push({
            title: i18n.t('saCurrencyInput.groups.recent'),
            currencies: currenciesShortlist.value,
          });
        }
        groups.push({
          title: i18n.t('saCurrencyInput.groups.all'),
          currencies: Object.values(currencies),
        });
        return groups;
      });

      const { defaultCurrency } = useCurrentWorkspace();
      const inputValue = ref(props.value || defaultCurrency);
      watch(() => props.value, (newValue) => {
        inputValue.value = newValue;
      });
      watch(inputValue, (newValue) => {
        emit('input', newValue);
      });

      loadShortlist(currenciesShortlist, currencies);

      return {
        inputValue,
        currenciesShortlist,
        currencies,
        currenciesGroups,
      };
    },
  });
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
