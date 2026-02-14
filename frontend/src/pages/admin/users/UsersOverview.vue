<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.usersOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.usersOverview.filters.announcement() }}</span>
        </div>

        <div>
          <ElInput
            v-model="freeSearchText"
            :placeholder="$t.usersOverview.filters.input.placeholder()"
            clearable
          >
            <template #prefix>
              <i class="el-icon-search el-input__icon" />
            </template>
          </ElInput>
        </div>

        <ElButton
          round
          @click="navigateToCreateUserView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t.usersOverview.create() }}
        </ElButton>
      </div>
    </div>

    <SaPageableItems
      #default="{ item: user }"
      :page-provider="usersProvider"
      :reload-on="[freeSearchText]"
    >
      <UsersOverviewPanel :user="user as PlatformUserDto" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
import SaIcon from '@/components/SaIcon.vue';
import UsersOverviewPanel from '@/pages/admin/users/UsersOverviewPanel.vue';
import type { ApiPageRequest, PlatformUserDto } from '@/services/api';
import { usersApi } from '@/services/api/api-client.ts';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';

const freeSearchText = ref<string | undefined>();

const usersProvider = async (request: ApiPageRequest, config: RequestInit) =>
  usersApi.getUsers(
    {
      ...request,
      freeSearchTextEq: freeSearchText.value,
    },
    config,
  );

const { navigateByViewName } = useNavigation();
const navigateToCreateUserView = () => navigateByViewName('create-new-user');
</script>
