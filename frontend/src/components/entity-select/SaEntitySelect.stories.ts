// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import { ref } from 'vue';
import type { ApiPage, ApiPageRequest, HasOptionalId } from '@/services/api';
import { delay } from '@/__storybook__/stories-utils';
import SaEntitySelect from '@/components/entity-select/SaEntitySelect.vue';
import { defineStory } from '@/__storybook__/sa-storybook';

export default {
  title: 'Components/Basic/SaEntitySelect',
};

interface StoriesEntity extends HasOptionalId {
  name: string,
}

const entities: Array<StoriesEntity> = [];
for (let i = 0; i < 1000; i += 1) {
  entities.push({
    id: i,
    name: `Entity ${i}`,
  });
}

export const Default = defineStory(() => ({
  components: { SaEntitySelect },
  setup: () => ({
    search: async (
      pageRequest: ApiPageRequest,
      query: string | undefined,
      requestInit: RequestInit,
    ) => {
      action('search')(pageRequest, query, requestInit);
      const filteredEntities = entities.filter((it) => it.name.toLowerCase()
        .indexOf((query || '').toLowerCase()) >= 0);
      return {
        totalElements: filteredEntities.length,
        pageSize: pageRequest.pageSize,
        pageNumber: 1,
        data: filteredEntities.slice(0, pageRequest.pageSize),
      } as ApiPage<StoriesEntity>;
    },
    neverEndingSearch: async () => {
      await delay(999999);
    },
    failingSearch: async () => {
      throw new Error('Cannot load data');
    },
    emptySearch: async () => ({
      totalElements: 0,
      data: [],
      pageSize: 10,
      pageNumber: 1,
    } as ApiPage<HasOptionalId>),
    labelProvider: (entity: StoriesEntity) => entity.name,
    neverEndingEntityProvider: async () => {
      await delay(999999);
    },
    failingEntityProvider: async () => {
      throw new Error('Request failed');
    },
    regularEntityProvider: async (id: number) => entities.find((it) => it.id === id),
    entity1: ref(1),
    entity2: ref(2),
    entity3: ref(3),
    entity4: ref(4),
    entity5: ref(5),
    entity6: ref(6),
    entity7: ref<number | undefined>(),
  }),
  template: `
    <h3>Loading initial value</h3>
    <SaEntitySelect
      style="width: 400px"
      :label-provider="labelProvider"
      :option-provider="neverEndingEntityProvider"
      :options-provider="search"
      v-model="entity1"
    />
    <br /> Selected value: {{entity1}}

    <h3>Failing loading initial value</h3>
    <SaEntitySelect
      style="width: 400px"
      :label-provider="labelProvider"
      :option-provider="failingEntityProvider"
      :options-provider="search"
      v-model="entity2"
    />
    <br /> Selected value: {{entity2}}

    <h3>Loaded initial value</h3>
    <SaEntitySelect
      id="screenshotTarget"
      style="width: 400px"
      :label-provider="labelProvider"
      :option-provider="regularEntityProvider"
      :options-provider="search"
      v-model="entity3"
    />
    <br /> Selected value: {{entity3}}

    <h3>Loading search</h3>
    <SaEntitySelect
      style="width: 400px"
      :label-provider="labelProvider"
      :option-provider="regularEntityProvider"
      :options-provider="neverEndingSearch"
      v-model="entity4"
    />
    <br /> Selected value: {{entity4}}

    <h3>Failing search</h3>
    <SaEntitySelect
      style="width: 400px"
      :label-provider="labelProvider"
      :option-provider="regularEntityProvider"
      :options-provider="failingSearch"
      v-model="entity5"
    />
    <br /> Selected value: {{entity5}}

    <h3>Empty search</h3>
    <SaEntitySelect
      style="width: 400px"
      :label-provider="labelProvider"
      :option-provider="regularEntityProvider"
      :options-provider="emptySearch"
      v-model="entity6"
    />
    <br /> Selected value: {{entity6}}

    <h3>With placeholder</h3>
    <SaEntitySelect
      style="width: 400px"
      placeholder="Select value"
      :label-provider="labelProvider"
      :option-provider="regularEntityProvider"
      :options-provider="search"
      v-model="entity7"
    />
    <br /> Selected value: {{entity7}}
  `,
}), {
  useRealTime: true,
});
