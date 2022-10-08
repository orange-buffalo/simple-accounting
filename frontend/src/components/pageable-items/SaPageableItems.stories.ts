// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import { defineComponent, ref } from 'vue';
import { ElInput } from 'element-plus';
import { waitForText } from '@/__storybook__/screenshots';
import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
import type { ApiPage, ApiPageRequest, HasOptionalId } from '@/services/api';
import { delay } from '@/__storybook__/stories-utils';
import { defineStory } from '@/__storybook__/sa-storybook';

interface ResourceResponse extends HasOptionalId {
  title: string,
}

export default {
  title: 'Components/Basic/SaPageableItems',
  parameters: {
    useRealTime: true,
    asPage: true,
  },
};

interface ResourceRequest extends ApiPageRequest {
  'filter[eq]'?: string,
}

function resourceEntities(request: ApiPageRequest, totalElements: number): ApiPage<ResourceResponse> {
  const result: ResourceResponse[] = [];
  const pageSize = request.pageSize || 10;
  const pageNumber = request.pageNumber || 1;
  const startIndex = pageSize * (pageNumber - 1);
  const endIndex = Math.min(totalElements, pageSize * pageNumber);
  for (let i = startIndex; i < endIndex; i += 1) {
    result.push({
      title: `Entity #${i}`,
    });
  }
  return {
    data: result,
    pageNumber: 1,
    pageSize: result.length,
    totalElements,
  };
}

function setupStory(requestExecutor: (request: ResourceRequest) => Promise<ApiPage<ResourceResponse> | void>) {
  return defineComponent({
    components: { SaPageableItems, ElInput },
    setup() {
      const filter = ref<string | undefined>();

      const pageProvider = async (request: ApiPageRequest, config: RequestInit) => {
        action('request with filter')(request, filter.value);
        if (config.signal) {
          // eslint-disable-next-line no-param-reassign
          config.signal.onabort = () => action('request-aborted')(request);
        }
        return (await requestExecutor(request)) as ApiPage<ResourceResponse>;
      };

      return {
        pageProvider,
        filter,
      };
    },

    template: `
      <SaPageableItems
        :page-provider="pageProvider"
        v-slot="{item: entity}"
      >
      {{ entity.title }}
      </SaPageableItems>`,
  });
}

export const InitialLoading = defineStory(() => ({
  ...setupStory(async () => delay(999999999)),
}));

export const EmptyData = defineStory(() => ({
  ...setupStory(async (request) => resourceEntities(request, 0)),
}), {
   screenshotPreparation: waitForText('No results here'),
});

export const SinglePage = defineStory(() => ({
  ...setupStory(async (request) => resourceEntities(request, 10)),
}), {
   screenshotPreparation: waitForText('Entity #1'),
});

export const MultiplePages = defineStory(() => ({
  ...setupStory(async (request) => resourceEntities(request, 34)),
}), {
   screenshotPreparation: waitForText('Entity #1'),
});

export const WithFilter = defineStory(() => ({
  ...setupStory(async (request) => resourceEntities(request, 13)),
  template: `
    <div>
      <ElInput v-model="filter"/>
      <SaPageableItems
        :page-provider="pageProvider"
        :reload-on="[filter]"
        v-slot="{item: entity}"
      >
      {{ entity.title }}
      </SaPageableItems>
    </div>`,
}), {
   screenshotPreparation: waitForText('Entity #1'),
});
