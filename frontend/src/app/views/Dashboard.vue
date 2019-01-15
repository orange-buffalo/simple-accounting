<template>
  <div class="home-page">
    <div class="sa-page-header">
      <h1>Dashboard</h1>

      <div class="sa-header-options">
        <span>&nbsp;</span>

        <el-date-picker
            v-model="selectedDateRange"
            type="daterange"
            align="right"
            unlink-panels
            range-separator="To"
            start-placeholder="Start date"
            end-placeholder="End date">
        </el-date-picker>
      </div>
    </div>

    <div class="home-page__row">
      <div class="home-page__row__hero">
        <div class="home-page__row__hero__header">
          <span class="home-page__row__hero__header__icon">
            <svgicon name="expense"/>
          </span>

          <money-output class="home-page__row__hero__header__amount"
                        :currency="defaultCurrency"
                        :amount="expenses.totalAmount"/>

          <span class="home-page__row__hero__header__finalized">Total of {{expenses.finalizedCount}} expenses</span>

          <span v-if="expenses.pendingCount"
                class="home-page__row__hero__header__pending">Pending {{expenses.pendingCount}} more</span>
          <span v-if="!expenses.pendingCount"
                class="home-page__row__hero__header__pending">&nbsp;</span>
        </div>

        <div class="home-page__row__hero__details" v-if="expenses.totalAmount">
          <div class="home-page__row__hero__details__item"
               v-for="item in expensesItems">
            <span>{{categoryById(item.categoryId).name}}</span>
            <money-output :currency="defaultCurrency"
                          :amount="item.totalAmount"/>
          </div>
        </div>
      </div>

      <div class="home-page__row__hero">
        <div class="home-page__row__hero__header">
          <span class="home-page__row__hero__header__icon">
            <svgicon name="income"/>
          </span>

          <money-output class="home-page__row__hero__header__amount"
                        :currency="defaultCurrency"
                        :amount="incomes.totalAmount"/>

          <span class="home-page__row__hero__header__finalized">Total of {{incomes.finalizedCount}} incomes</span>

          <span v-if="incomes.pendingCount"
                class="home-page__row__hero__header__pending">Pending {{incomes.pendingCount}} more</span>
          <span v-if="!incomes.pendingCount"
                class="home-page__row__hero__header__pending">&nbsp;</span>
        </div>

        <div class="home-page__row__hero__details" v-if="incomes.totalAmount">
          <div class="home-page__row__hero__details__item"
               v-for="item in incomesItems">
            <span>{{categoryById(item.categoryId).name}}</span>
            <money-output :currency="defaultCurrency"
                          :amount="item.totalAmount"/>
          </div>
        </div>
      </div>

      <div class="home-page__row__hero">
        <div class="home-page__row__hero__header">
          <span class="home-page__row__hero__header__icon">
            <svgicon name="profit"/>
          </span>

          <money-output class="home-page__row__hero__header__amount"
                        :currency="defaultCurrency"
                        :amount="profit"/>

          <span class="home-page__row__hero__header__finalized">Profit</span>
          <span class="home-page__row__hero__header__pending">&nbsp;</span>
        </div>

        <div class="home-page__row__hero__details"
             v-if="profitDetailsVisible">
          <div class="home-page__row__hero__details__item"
               v-if="incomes.currencyExchangeGain">
            <span>Currency exchange gain</span>
            <money-output :currency="defaultCurrency"
                          :amount="incomes.currencyExchangeGain"/>
          </div>

          <div class="home-page__row__hero__details__item"
               v-if="incomes.currencyExchangeGain && profit">
            <span>Total profit</span>
            <money-output :currency="defaultCurrency"
                          :amount="totalProfit"/>
          </div>

          <div class="home-page__row__hero__details__item"
               v-if="taxPayments.totalTaxPayments">
            <span>Tax Payments</span>
            <money-output :currency="defaultCurrency"
                          :amount="taxPayments.totalTaxPayments"/>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import {api} from '@/services/api'
  import {withWorkspaces} from '@/app/components/mixins/with-workspaces'
  import {withCategories} from '@/app/components/mixins/with-categories'
  import '@/components/icons/expense'
  import '@/components/icons/income'
  import '@/components/icons/profit'
  import MoneyOutput from '@/app/components/MoneyOutput'
  import {lockr} from '@/app/services/app-services'
  import {isNil} from 'lodash'

  const SELECTED_DATE_RANGE_KEY = 'dashboard.selected-date-range'

  export default {
    name: 'Dashboard',

    mixins: [withWorkspaces, withCategories],

    components: {MoneyOutput},

    data: function () {
      return {
        expenses: {},
        incomes: {},
        taxPayments: {},
        selectedDateRange: []
      }
    },

    computed: {
      expensesItems: function () {
        return (this.expenses.items || []).sort((a, b) => b.totalAmount - a.totalAmount)
      },

      incomesItems: function () {
        return (this.incomes.items || []).sort((a, b) => b.totalAmount - a.totalAmount)
      },

      profit: function () {
        return (!isNil(this.expenses.totalAmount) && !isNil(this.incomes.totalAmount))
            ? this.incomes.totalAmount - this.expenses.totalAmount
            : null
      },

      profitDetailsVisible: function () {
        return this.incomes.currencyExchangeGain || this.taxPayments.totalTaxPayments
      },

      totalProfit: function () {
        return (this.profit && this.incomes.currencyExchangeGain)
            ? this.profit + this.incomes.currencyExchangeGain
            : null
      }
    },

    created: function () {
      let selectedDateRange = lockr.get(SELECTED_DATE_RANGE_KEY)
      if (isNil(selectedDateRange)) {
        let now = new Date();
        this.selectedDateRange = [
          new Date(now.getFullYear(), 0, 1),
          now
        ]
      } else {
        this.selectedDateRange = selectedDateRange.map(it => new Date(it))
      }

      this.reload()
    },

    methods: {
      reload: function () {
        api.get(`/user/workspaces/${this.currentWorkspace.id}/statistics/expenses` +
            `?fromDate=${api.dateToString(this.selectedDateRange[0])}` +
            `&toDate=${api.dateToString(this.selectedDateRange[1])}`)
            .then(response => this.expenses = response.data)

        api.get(`/user/workspaces/${this.currentWorkspace.id}/statistics/incomes` +
            `?fromDate=${api.dateToString(this.selectedDateRange[0])}` +
            `&toDate=${api.dateToString(this.selectedDateRange[1])}`)
            .then(response => this.incomes = response.data)

        api.get(`/user/workspaces/${this.currentWorkspace.id}/statistics/tax-payments` +
            `?fromDate=${api.dateToString(this.selectedDateRange[0])}` +
            `&toDate=${api.dateToString(this.selectedDateRange[1])}`)
            .then(response => this.taxPayments = response.data)
      }
    },

    watch: {
      currentWorkspace: function () {
        this.reload()
      },

      selectedDateRange: function () {
        lockr.set(SELECTED_DATE_RANGE_KEY, this.selectedDateRange)
        this.reload()
      }
    }
  }
</script>

<style lang="scss">
  .home-page__row {
    display: flex;
    justify-content: space-evenly;
    align-items: stretch;
  }

  .home-page__row__hero {
    padding: 20px;
    border: 1px solid #ebeef5;
    background-color: #fff;
    border-radius: 4px;
    width: 27%;
  }

  .home-page__row__hero__header {
    text-align: center;
  }

  .home-page__row__hero__header__icon {
    display: inline-block;
    width: 15%;
    border: 1px solid #ebeef5;
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
    color: #8e8e8e;
  }

  .home-page__row__hero__header__pending {
    display: block;
    color: orange;
    font-size: 90%;
  }

  .home-page__row__hero__details {
    margin-top: 20px;
    padding-top: 10px;
    border-top: 1px solid #ebeef5;
  }

  .home-page__row__hero__details__item {
    display: flex;
    justify-content: space-between;
    padding: 5px 0;
    font-size: 80%;
    color: #72757c;
  }
</style>
