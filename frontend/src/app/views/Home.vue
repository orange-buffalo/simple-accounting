<template>
  <div class="home-page">
    <div class="sa-page-header">
      <h1>Dashboard</h1>

      <div class="sa-header-options">
          <span>...</span>
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
        </div>

        <div class="home-page__row__hero__details">
          <div class="home-page__row__hero__details__item"
               v-for="item in expensesItems">
            <span>{{categoryById(item.categoryId).name}}</span>
            <money-output :currency="defaultCurrency"
                          :amount="item.totalAmount"/>
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
  import MoneyOutput from '@/app/components/MoneyOutput'

  export default {
    name: 'Home',

    mixins: [withWorkspaces, withCategories],

    components: {MoneyOutput},

    data: function () {
      return {
        expenses: {}
      }
    },

    computed: {
      expensesItems: function () {
        return (this.expenses.items || []).sort((a, b) => b.totalAmount - a.totalAmount)
      }
    },

    created: async function () {
      this.expenses = (await api.get(`/user/workspaces/${this.currentWorkspace.id}/statistics/expenses?fromDate=2000-01-01&toDate=3000-01-01`)).data
    }
  }
</script>

<style lang="scss">
  .home-page__row {
    display: flex;
    justify-content: space-around;
  }

  .home-page__row__hero {
    height: 100%;
    padding: 20px;
    border: 1px solid #ebeef5;
    background-color: #fff;
    border-radius: 4px;
    width: 30%;
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
