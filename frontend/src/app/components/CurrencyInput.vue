<template>
  <el-select v-model="inputValue"
             filterable
             :placeholder="placeholder">
    <el-option
        v-for="currency in currencies"
        :key="currency.code"
        :label="currency.code"
        :value="currency.code">
    </el-option>
  </el-select>
</template>

<script>
  import api from '@/services/api'
  import {mapState, mapGetters, mapActions} from 'vuex'

  export default {
    name: 'CurrencyInput',

    props: {
      value: String,
      placeholder: String
    },

    data: function () {
      return {
        inputValue: this.value
      }
    },

    created: function () {
      this.loadCurrencies()
      if (!this.inputValue) {
        this.inputValue = this.defaultCurrency
      }
    },

    methods: {
      ...mapActions('app', {
        loadCurrencies: 'loadCurrencies'
      })
    },

    computed: {
      ...mapState({
        currencies: state => state.app.currencies,
        defaultCurrency: state => state.workspaces.currentWorkspace.defaultCurrency
      })
    },

    watch: {
      inputValue(val) {
        this.$emit('input', val);
      }
    }
  }
</script>