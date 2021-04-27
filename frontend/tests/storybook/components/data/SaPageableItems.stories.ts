import { action } from '@storybook/addon-actions';
import SaPageableItems from '@/components/data/SaPageableItems';
import { usePageableItems } from '@/components/data/pageableItems';
import { defineComponent, ref } from '@vue/composition-api';
import { apiClient, ApiPage, ApiPageRequest } from '@/services/api';
import { Page } from 'puppeteer';
import { onGet } from '../../utils/stories-api-mocks';
import { pauseAndResetAnimation } from '../../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components/SaPageableItems',
  parameters: {
    fullWidth: true,
    skipMockTime: true,
  },
};

interface ResourceRequest extends ApiPageRequest {
  'filter[eq]'?: string,
}

interface ResourceResponse {
  title: string,
}

function resourceEntities(startIndex: number, endIndex: number): ResourceResponse[] {
  const result: ResourceResponse[] = [];
  for (let i = startIndex; i <= endIndex; i += 1) {
    result.push({
      title: `Entity #${i}`,
    });
  }
  return result;
}

function mockApiResponse(totalElements: number, data: ResourceResponse[]) {
  return onGet('resource')
    .intercept((req, res) => {
      action('api-request')(req.query);
      res.json({
        pageSize: req.params.pageSize as any,
        pageNumber: req.params.pageNumber as any,
        totalElements,
        data,
      } as ApiPage<ResourceResponse>);
    });
}

function setupStory() {
  return defineComponent({
    components: { SaPageableItems },
    setup() {
      const filter = ref<string|null>(null);
      const { items: pageableItems } = usePageableItems<ResourceRequest, ResourceResponse>({
        'filter[eq]': filter as any,
      }, (request, config) => apiClient.get('resource', {
        ...config,
        params: request,
      }));

      return {
        pageableItems,
        filter,
      };
    },

    template: `
      <SaPageableItems
        :items="pageableItems"
        v-slot="{item: entity}"
      >
        {{ entity.title }}
      </SaPageableItems>`,
  });
}

export const InitialLoading = () => ({
  ...setupStory(),
  beforeCreate() {
    onGet('resource')
      .neverEndingRequest();
  },
});
InitialLoading.parameters = {
  storyshots: {
    async setup(page: Page) {
      await pauseAndResetAnimation(page, '.sa-pageable-items__loader-item');
    },
  },
};

export const EmptyData = () => ({
  ...setupStory(),
  beforeCreate() {
    mockApiResponse(0, []);
  },
});
EmptyData.parameters = {
  storyshots: {
    async setup(page: Page) {
      await page.waitForSelector('.sa-pageable-items__empty-results__icon');
    },
  },
};

export const SinglePage = () => ({
  ...setupStory(),
  beforeCreate() {
    mockApiResponse(5, resourceEntities(1, 5));
  },
});
SinglePage.parameters = {
  storyshots: {
    async setup(page: Page) {
      await page.waitForSelector('.el-pagination');
    },
  },
};

export const MultiplePages = () => ({
  ...setupStory(),
  beforeCreate() {
    mockApiResponse(50, resourceEntities(1, 10));
  },
});
MultiplePages.parameters = {
  storyshots: {
    async setup(page: Page) {
      await page.waitForSelector('.el-pagination');
    },
  },
};

export const WithFilter = () => ({
  ...setupStory(),
  template: `
    <div>
      <ElInput v-model="filter"/>
      <SaPageableItems
        :items="pageableItems"
        v-slot="{item: entity}"
      >
        {{ entity.title }}
      </SaPageableItems>
    </div>`,
  beforeCreate() {
    mockApiResponse(50, resourceEntities(1, 10));
  },
});
WithFilter.parameters = {
  storyshots: {
    async setup(page: Page) {
      await page.waitForSelector('.el-pagination');
    },
  },
};
