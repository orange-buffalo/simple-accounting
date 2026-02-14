<template>
  <div
    class="side-menu__workspace-name"
  >
    {{ currentWorkspace.name }}
  </div>

  <SaSideMenuLink
    to="/"
    :title="$t.navigationMenu.dashboard()"
    icon="dashboard"
  />
  <SaSideMenuLink
    to="/expenses"
    :title="$t.navigationMenu.expenses()"
    icon="expense"
  />
  <SaSideMenuLink
    to="/incomes"
    :title="$t.navigationMenu.incomes()"
    icon="income"
  />
  <SaSideMenuLink
    to="/invoices"
    :title="$t.navigationMenu.invoices()"
    icon="invoices-overview"
  />
  <SaSideMenuLink
    to="/income-tax-payments"
    :title="$t.navigationMenu.taxPayments()"
    icon="income-tax-payments-overview"
  />
  <SaSideMenuLink
    to="/reporting"
    :title="$t.navigationMenu.reporting()"
    icon="reporting"
  />

  <template v-if="isCurrentUserRegular()">
    <span class="side-menu__category">{{ $t.navigationMenu.settings.header() }}</span>

    <SaSideMenuLink
      v-if="currentWorkspace.editable"
      to="/settings/customers"
      :title="$t.navigationMenu.settings.customers()"
      icon="customers-overview"
    />

    <SaSideMenuLink
      v-if="currentWorkspace.editable"
      to="/settings/categories"
      :title="$t.navigationMenu.settings.categories()"
      icon="category"
    />

    <SaSideMenuLink
      v-if="currentWorkspace.editable"
      to="/settings/general-taxes"
      :title="$t.navigationMenu.settings.generalTaxes()"
      icon="taxes-overview"
    />

    <SaSideMenuLink
      to="/settings/workspaces"
      :title="$t.navigationMenu.settings.workspaces()"
      icon="workspaces"
    />
  </template>
</template>

<script lang="ts" setup>
/// <reference types="vite-svg-loader" />
import { onUnmounted, ref } from 'vue';
import SaSideMenuLink from '@/components/side-menu/SaSideMenuLink.vue';
import type { WorkspaceDto } from '@/services/api';
import { useAuth } from '@/services/api';
import { WORKSPACE_CHANGED_EVENT } from '@/services/events';
import { $t } from '@/services/i18n';
import { useCurrentWorkspace } from '@/services/workspaces';

const { isCurrentUserRegular } = useAuth();

const currentWorkspace = ref(useCurrentWorkspace().currentWorkspace);
const onWorkspaceChange = (newWorkspace: WorkspaceDto) => {
  currentWorkspace.value = newWorkspace;
};
WORKSPACE_CHANGED_EVENT.subscribe(onWorkspaceChange);
onUnmounted(() => WORKSPACE_CHANGED_EVENT.unsubscribe(onWorkspaceChange));
</script>
