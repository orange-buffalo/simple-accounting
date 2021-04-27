<template>
  <div class="home-page">
    <div class="sa-page-header">
      <h1>{{ $t('dashboard.header') }}</h1>

      <div class="sa-header-options">
        <span>&nbsp;</span>

        <!--suppress HtmlDeprecatedAttribute -->
        <ElDatePicker
          v-model="selectedDateRange"
          type="daterange"
          align="right"
          unlink-panels
          range-separator="To"
          start-placeholder="Start date"
          end-placeholder="End date"
        />
      </div>
    </div>

    <div class="home-page__row">
      <div class="home-page__row__hero">
        <div class="home-page__row__hero__header">
          <span class="home-page__row__hero__header__icon">
            <SaIcon icon="expense" />
          </span>

          <template v-if="expensesLoaded">
            <MoneyOutput
              class="home-page__row__hero__header__amount"
              :currency="defaultCurrency"
              :amount="expenses.totalAmount"
            />

            <span class="home-page__row__hero__header__finalized">Total of {{ expenses.finalizedCount }} expenses</span>

            <span
              v-if="expenses.pendingCount"
              class="home-page__row__hero__header__pending"
            >Pending {{ expenses.pendingCount }} more</span>
            <span
              v-if="!expenses.pendingCount"
              class="home-page__row__hero__header__pending"
            >&nbsp;</span>
          </template>

          <div
            v-else
            class="home-page__row__hero__header__loader"
          >
            <i class="el-icon-loading" />
          </div>
        </div>

        <div
          v-if="expensesLoaded"
          class="home-page__row__hero__details"
        >
          <div
            v-for="item in expenses.items"
            :key="item.categoryId || 'fake'"
            class="home-page__row__hero__details__item"
          >
            <span><SaCategoryOutput :category-id="item.categoryId" /></span>
            <MoneyOutput
              :currency="defaultCurrency"
              :amount="item.totalAmount"
            />
          </div>
        </div>
      </div>

      <div class="home-page__row__hero">
        <div class="home-page__row__hero__header">
          <span class="home-page__row__hero__header__icon">
            <SaIcon icon="income" />
          </span>

          <template v-if="incomesLoaded">
            <MoneyOutput
              class="home-page__row__hero__header__amount"
              :currency="defaultCurrency"
              :amount="incomes.totalAmount"
            />

            <span class="home-page__row__hero__header__finalized">Total of {{ incomes.finalizedCount }} incomes</span>

            <span
              v-if="incomes.pendingCount"
              class="home-page__row__hero__header__pending"
            >Pending {{ incomes.pendingCount }} more</span>
            <span
              v-if="!incomes.pendingCount"
              class="home-page__row__hero__header__pending"
            >&nbsp;</span>
          </template>

          <div
            v-else
            class="home-page__row__hero__header__loader"
          >
            <i class="el-icon-loading" />
          </div>
        </div>

        <div
          v-if="incomesLoaded"
          class="home-page__row__hero__details"
        >
          <div
            v-for="item in incomes.items"
            :key="item.categoryId || 'fake'"
            class="home-page__row__hero__details__item"
          >
            <span><SaCategoryOutput :category-id="item.categoryId" /></span>
            <MoneyOutput
              :currency="defaultCurrency"
              :amount="item.totalAmount"
            />
          </div>
        </div>
      </div>

      <div class="home-page__row__hero">
        <div class="home-page__row__hero__header">
          <span class="home-page__row__hero__header__icon">
            <SaIcon icon="profit" />
          </span>

          <template v-if="profitLoaded">
            <MoneyOutput
              class="home-page__row__hero__header__amount"
              :currency="defaultCurrency"
              :amount="Math.max(incomeTaxableAmount, 0)"
            />

            <span class="home-page__row__hero__header__finalized">Taxable Amount</span>
            <span class="home-page__row__hero__header__pending">&nbsp;</span>
          </template>

          <div
            v-else
            class="home-page__row__hero__header__loader"
          >
            <i class="el-icon-loading" />
          </div>
        </div>

        <div
          v-if="profitLoaded"
          class="home-page__row__hero__details"
        >
          <div
            v-if="currencyExchangeDifference"
            class="home-page__row__hero__details__item"
          >
            <span>Currency exchange rate difference</span>
            <MoneyOutput
              :currency="defaultCurrency"
              :amount="currencyExchangeDifference"
            />
          </div>

          <div class="home-page__row__hero__details__item">
            <span>Income Tax Payments</span>
            <MoneyOutput
              :currency="defaultCurrency"
              :amount="incomeTaxPayments.totalTaxPayments"
            />
          </div>

          <div class="home-page__row__hero__details__item">
            <span>Estimated Tax</span>
            <span>coming soon..</span>
          </div>

          <div class="home-page__row__hero__details__item">
            <span>Profit</span>
            <MoneyOutput
              :currency="defaultCurrency"
              :amount="totalProfit"
            />
          </div>
        </div>
      </div>

      <div
        v-for="invoice in pendingInvoices"
        :key="invoice.id"
        class="home-page__row__hero"
      >
        <div class="home-page__row__hero__header">
          <span class="home-page__row__hero__header__icon">
            <SaIcon icon="invoices-overview" />
          </span>

          <MoneyOutput
            class="home-page__row__hero__header__amount"
            :currency="invoice.currency"
            :amount="invoice.amount"
          />

          <span class="home-page__row__hero__header__finalized">{{ invoice.title }}</span>
          <span class="home-page__row__hero__header__finalized">{{ invoiceStatus(invoice) }}</span>
          <span class="home-page__row__hero__header__pending">&nbsp;</span>
        </div>

        <div class="home-page__row__hero__details">
          <div class="home-page__row__hero__details__item">
            <span>To</span>
            <span><SaCustomerOutput :customer-id="invoice.customer" /></span>
          </div>

          <div class="home-page__row__hero__details__item">
            <span>Issue Date</span>
            <span>{{ $t('common.date.medium', [invoice.dateIssued]) }}</span>
          </div>

          <div class="home-page__row__hero__details__item">
            <span>Date Sent</span>
            <span>{{ $t('common.date.medium', [invoice.dateSent]) }}</span>
          </div>

          <div class="home-page__row__hero__details__item">
            <span>Due Date</span>
            <span>{{ $t('common.date.medium', [invoice.dueDate]) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import {
    defineComponent, ref, computed, watch, Ref,
  } from '@vue/composition-api';
  import {
    apiClient, apiDateString, InvoiceDto, consumeAllPages,
  } from '@/services/api';
  import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';
  import { useStorage } from '@/services/storage';
  import MoneyOutput from '@/components/MoneyOutput';
  import SaIcon from '@/components/SaIcon';
  import SaCustomerOutput from '@/components/customer/SaCustomerOutput';
  import SaCategoryOutput from '@/components/category/SaCategoryOutput';
  import { AxiosResponse } from 'axios';

  function useSelectedDateRange() {
    const storage = useStorage<Array<Date>>('dashboard.selected-date-range');

    const selectedDateRange = ref<Array<Date>>(storage.getOrDefault([]));
    if (!selectedDateRange.value.length) {
      const now = new Date();
      selectedDateRange.value = [
        new Date(now.getFullYear(), 0, 1),
        now,
      ];
    } else {
      selectedDateRange.value = selectedDateRange.value.map((it) => new Date(it));
    }

    watch(selectedDateRange, (newDatesRanges) => {
      storage.set(newDatesRanges);
    });

    return { selectedDateRange };
  }

  type StatisticsRequest = {
      fromDate: string,
      toDate: string,
      workspaceId: number,
  }

  function useStatisticsData<T>(
    selectedDateRange: Ref<Array<Date>>,
    apiCall: (request: StatisticsRequest) => Promise<AxiosResponse<T>>,
  ) {
    const statisticsData: Ref<T | null> = ref(null);
    const { currentWorkspaceId } = useCurrentWorkspace();

    watch(selectedDateRange, async ([fromDate, toDate]) => {
      statisticsData.value = null;

      const response = await apiCall({
        fromDate: apiDateString(fromDate),
        toDate: apiDateString(toDate),
        workspaceId: currentWorkspaceId,
      });
      statisticsData.value = response.data;
    }, { immediate: true });

    const statisticsLoaded = computed(() => statisticsData.value !== null);

    return {
      statisticsData,
      statisticsLoaded,
    };
  }

  function useInvoices() {
    const pendingInvoices = ref<Array<InvoiceDto>>([]);

    const { currentWorkspaceId } = useCurrentWorkspace();

    consumeAllPages((pageRequest) => apiClient.getInvoices({
      workspaceId: currentWorkspaceId,
      'status[in]': ['SENT', 'OVERDUE'],
      ...pageRequest,
    }))
      .then((data) => {
        pendingInvoices.value = data;
      });

    const invoiceStatus = (invoice:InvoiceDto) => {
      if (invoice.status === 'OVERDUE') {
        return 'Overdue';
      }
      return 'Pending';
    };

    return { pendingInvoices, invoiceStatus };
  }

  export default defineComponent({
    components: {
      SaCategoryOutput,
      SaCustomerOutput,
      SaIcon,
      MoneyOutput,
    },

    setup() {
      const { selectedDateRange } = useSelectedDateRange();

      const {
        statisticsData: expenses,
        statisticsLoaded: expensesLoaded,
      } = useStatisticsData(selectedDateRange, (request) => apiClient.getExpensesStatistics(request));

      const {
        statisticsData: incomes,
        statisticsLoaded: incomesLoaded,
      } = useStatisticsData(selectedDateRange, (request) => apiClient.getIncomesStatistics(request));

      const {
        statisticsData: incomeTaxPayments,
        statisticsLoaded: taxPaymentsLoaded,
      } = useStatisticsData(selectedDateRange, (request) => apiClient.getTaxPaymentsStatistics(request));

      const profitLoaded = computed(() => incomesLoaded.value && expensesLoaded.value && taxPaymentsLoaded.value);

      const { defaultCurrency } = useCurrentWorkspace();

      watch(expenses, (newExpenses) => {
        if (newExpenses) {
          newExpenses.items.sort((a, b) => b.totalAmount - a.totalAmount);
        }
      });

      watch(incomes, (newIncomes) => {
        if (newIncomes) {
          newIncomes.items.sort((a, b) => b.totalAmount - a.totalAmount);
        }
      });

      const incomeTaxableAmount = computed(() => {
        if (incomes.value && expenses.value) {
          return incomes.value.totalAmount - expenses.value.totalAmount;
        }
        return 0;
      });

      const currencyExchangeDifference = computed(() => {
        if (incomes.value && expenses.value) {
          return incomes.value.currencyExchangeDifference + expenses.value.currencyExchangeDifference;
        }
        return 0;
      });

      const totalProfit = computed(() => (incomeTaxPayments.value
        ? incomeTaxableAmount.value + currencyExchangeDifference.value - incomeTaxPayments.value.totalTaxPayments : 0));

      return {
        selectedDateRange,
        expenses,
        expensesLoaded,
        incomes,
        incomesLoaded,
        incomeTaxPayments,
        taxPaymentsLoaded,
        profitLoaded,
        defaultCurrency,
        incomeTaxableAmount,
        currencyExchangeDifference,
        totalProfit,
        ...useInvoices(),
      };
    },
  });
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";

  .home-page__row {
    display: flex;
    justify-content: space-evenly;
    align-items: stretch;
    flex-wrap: wrap;
  }

  .home-page__row__hero {
    padding: 20px;
    border: 1px solid $secondary-grey;
    background-color: $white;
    border-radius: 2px;
    width: 27%;
    margin-bottom: 20px;
  }

  .home-page__row__hero__header {
    text-align: center;
  }

  $header-icon-container-size: 20%;
  $header-icon-size: 60%;
  $header-icon-position: (100% - $header-icon-size) / 2;

  .home-page__row__hero__header__icon {
    display: inline-block;
    width: $header-icon-container-size;
    padding-top: $header-icon-container-size;
    border: 1px solid $secondary-grey;
    border-radius: 50%;
    position: relative;

    .sa-icon {
      position: absolute;
      width: $header-icon-size;
      height: $header-icon-size;
      top: $header-icon-position;
      left: $header-icon-position;
    }
  }

  .home-page__row__hero__header__amount {
    display: block;
    margin: 10px 0;
    font-size: 130%;
    font-weight: bold;
  }

  .home-page__row__hero__header__finalized {
    display: block;
    font-size: 90%;
    color: $secondary-text-color;
  }

  .home-page__row__hero__header__pending {
    display: block;
    color: $warning-color;
    font-size: 90%;
  }

  .home-page__row__hero__header__loader {
    margin-top: 20px;
    font-size: 200%;
  }

  .home-page__row__hero__details {
    margin-top: 20px;
    padding-top: 10px;
    border-top: 1px solid $secondary-grey;
  }

  .home-page__row__hero__details__item {
    display: flex;
    justify-content: space-between;
    padding: 5px 0 0;
    font-size: 80%;
    color: $secondary-text-color;
  }
</style>
