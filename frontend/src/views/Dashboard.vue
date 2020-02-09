<template>
  <div class="home-page">
    <div class="sa-page-header">
      <h1>{{ $t('dashboard.header') }}</h1>

      <div class="sa-header-options">
        <span>&nbsp;</span>

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

          <div class="home-page__row__hero__header__loader"
               v-else>
            <i class="el-icon-loading" />
          </div>
        </div>

        <div
          v-if="expensesLoaded"
          class="home-page__row__hero__details"
        >
          <div
            v-for="item in expensesItems"
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

          <div class="home-page__row__hero__header__loader"
               v-else>
            <i class="el-icon-loading" />
          </div>
        </div>

        <div
          v-if="incomesLoaded"
          class="home-page__row__hero__details"
        >
          <div
            v-for="item in incomesItems"
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

          <div class="home-page__row__hero__header__loader"
               v-else>
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

<script>
  import { api } from '@/services/api';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import MoneyOutput from '@/components/MoneyOutput';
  import { lockr } from '@/services/app-services';
  import SaIcon from '@/components/SaIcon';
  import SaCustomerOutput from '@/components/customer/SaCustomerOutput';
  import SaCategoryOutput from '@/components/category/SaCategoryOutput';

  const SELECTED_DATE_RANGE_KEY = 'dashboard.selected-date-range';

  export default {
    name: 'Dashboard',

    components: {
      SaCategoryOutput,
      SaCustomerOutput,
      SaIcon,
      MoneyOutput,
    },

    mixins: [withWorkspaces],

    data() {
      return {
        expenses: {},
        incomes: {},
        incomeTaxPayments: {},
        selectedDateRange: [],
        pendingInvoices: [],
      };
    },

    computed: {
      incomesLoaded() {
        return this.incomes.items != null;
      },

      expensesLoaded() {
        return this.incomes.items != null;
      },

      taxPaymentsLoaded() {
        return this.incomeTaxPayments.totalTaxPayments != null;
      },

      profitLoaded() {
        return this.incomesLoaded && this.expensesLoaded && this.taxPaymentsLoaded;
      },

      expensesItems() {
        return (this.expenses.items || []).sort((a, b) => b.totalAmount - a.totalAmount);
      },

      incomesItems() {
        return (this.incomes.items || []).sort((a, b) => b.totalAmount - a.totalAmount);
      },

      incomeTaxableAmount() {
        return this.incomes.totalAmount - this.expenses.totalAmount;
      },

      totalProfit() {
        return this.incomeTaxableAmount + this.currencyExchangeDifference - this.incomeTaxPayments.totalTaxPayments;
      },

      invoiceStatus() {
        return (invoice) => {
          if (invoice.status === 'OVERDUE') {
            return 'Overdue';
          }
          return 'Pending';
        };
      },

      currencyExchangeDifference() {
        return this.incomes.currencyExchangeDifference + this.expenses.currencyExchangeDifference;
      },
    },

    watch: {
      selectedDateRange() {
        lockr.set(SELECTED_DATE_RANGE_KEY, this.selectedDateRange);
        this.reload();
      },
    },

    created() {
      const selectedDateRange = lockr.get(SELECTED_DATE_RANGE_KEY);
      if (selectedDateRange == null) {
        const now = new Date();
        this.selectedDateRange = [
          new Date(now.getFullYear(), 0, 1),
          now,
        ];
      } else {
        this.selectedDateRange = selectedDateRange.map(it => new Date(it));
      }

      this.reload();
    },

    methods: {
      reload() {
        this.expenses = {};
        this.incomes = {};
        this.incomeTaxPayments = {};
        this.pendingInvoices = [];

        api.get(`/workspaces/${this.currentWorkspace.id}/statistics/expenses`
          + `?fromDate=${api.dateToString(this.selectedDateRange[0])}`
          + `&toDate=${api.dateToString(this.selectedDateRange[1])}`)
          .then(response => this.expenses = response.data);

        api.get(`/workspaces/${this.currentWorkspace.id}/statistics/incomes`
          + `?fromDate=${api.dateToString(this.selectedDateRange[0])}`
          + `&toDate=${api.dateToString(this.selectedDateRange[1])}`)
          .then(response => this.incomes = response.data);

        api.get(`/workspaces/${this.currentWorkspace.id}/statistics/income-tax-payments`
          + `?fromDate=${api.dateToString(this.selectedDateRange[0])}`
          + `&toDate=${api.dateToString(this.selectedDateRange[1])}`)
          .then(response => this.incomeTaxPayments = response.data);

        api.pageRequest(`/workspaces/${this.currentWorkspace.id}/invoices`)
          .eager()
          .eqFilter('status', ['SENT', 'OVERDUE'])
          .getPageData()
          .then(invoices => this.pendingInvoices = invoices);
      },
    },
  };
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

  .home-page__row__hero__header__icon {
    display: inline-block;
    width: 15%;
    border: 1px solid $secondary-grey;
    border-radius: 50%;
    padding: 15px;

    .svg-icon {
      width: 100%;
      height: auto;
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
