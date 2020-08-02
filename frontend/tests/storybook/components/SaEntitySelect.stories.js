import { action } from '@storybook/addon-actions';
import SaEntitySelect from '@/components/SaEntitySelect';
import { onGetToWorkspacePath, apiPage, responseDelay } from '../utils/stories-api-mocks';
import {
  NO_STORYSHOTS_STORY,
  pauseAndResetInputLoaderAnimation,
  removeSvgAnimations, setViewportHeight, storyshotsStory,
  timeout,
} from '../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components|SaEntitySelect',
};

let nextId = 1;

function generateEntitiesPage(pageSize, totalSize, searchText) {
  const prefix = searchText ? searchText.eq : 'Entity';
  const data = [];
  for (let i = 0; i < pageSize; i += 1) {
    data[i] = {
      id: nextId,
      name: `${prefix} ${i}`,
    };
    nextId += 1;
  }
  return {
    ...apiPage(data),
    totalElements: totalSize,
  };
}

function startApiSpec() {
  return onGetToWorkspacePath('entities')
    .on('request', (req) => {
      action('api-request')(req.pathname, req.query);
    });
}

function setupStory() {
  return ({
    components: { SaEntitySelect },
    data() {
      return {
        labelProvider(entity) {
          return entity.name;
        },
        value: null,
      };
    },
    methods: {
      onChange(newValue) {
        this.value = newValue;
        action('on-change')(newValue);
      },
    },
    template: `
      <SaEntitySelect
        :label-provider="labelProvider"
        :value="value"
        entity-path="entities"
        v-slot="{ entity }"
        placeholder="Select or search"
        @input="onChange"
        style="width: 350px"
      >
        {{ entity.name }}
      </SaEntitySelect>`,
  });
}

async function toggleSelectInStoryshots(page) {
  const openIndicator = await page.$('.vs__open-indicator');
  await openIndicator.click();
  await timeout(1000);
}

export const InitialLoading = () => ({
  ...setupStory(),
  beforeCreate() {
    startApiSpec()
      .neverEndingRequest();
  },
});

InitialLoading.story = storyshotsStory({
  async setup(page) {
    await toggleSelectInStoryshots(page);
    await removeSvgAnimations(page);
  },
});

export const InitialLoadingFailure = () => ({
  ...setupStory(),
  beforeCreate() {
    startApiSpec()
      .intercept((req, res) => {
        res.status(404);
      });
  },
});

InitialLoadingFailure.story = storyshotsStory({
  async setup(page) {
    await toggleSelectInStoryshots(page);
  },
});

export const Loading = () => ({
  ...setupStory(),
  beforeCreate() {
    let initialLoadingComplete = false;
    startApiSpec()
      .intercept(async (req, res) => {
        if (initialLoadingComplete) {
          await responseDelay(1500);
          res.json(generateEntitiesPage(5, 11, req.query.freeSearchText));
        } else {
          res.json(generateEntitiesPage(10, 11));
          initialLoadingComplete = true;
        }
      });
  },
});
Loading.story = storyshotsStory({
  async setup(page) {
    await toggleSelectInStoryshots(page);
    await setViewportHeight(page, 350);
  },
});

export const FailingSearch = () => ({
  ...setupStory(),
  beforeCreate() {
    let initialLoadingComplete = false;
    startApiSpec()
      .intercept(async (req, res) => {
        if (initialLoadingComplete) {
          res.status(500);
        } else {
          res.json(generateEntitiesPage(10, 11));
          initialLoadingComplete = true;
        }
      });
  },
});
FailingSearch.story = NO_STORYSHOTS_STORY;

export const NoDataAfterInitialLoading = () => ({
  ...setupStory(),
  beforeCreate() {
    startApiSpec()
      .successJson(apiPage([]));
  },
});
NoDataAfterInitialLoading.story = storyshotsStory({
  async setup(page) {
    await toggleSelectInStoryshots(page);
  },
});

export const NoDataOnSearch = () => ({
  ...setupStory(),
  beforeCreate() {
    let initialLoadingComplete = false;
    startApiSpec()
      .intercept(async (req, res) => {
        if (initialLoadingComplete) {
          res.json(apiPage([]));
        } else {
          res.json(generateEntitiesPage(10, 11));
          initialLoadingComplete = true;
        }
      });
  },
});
NoDataOnSearch.story = NO_STORYSHOTS_STORY;

// noinspection JSUnusedGlobalSymbols
export const PreSelectedValue = () => ({
  ...setupStory(),
  beforeCreate() {
    startApiSpec()
      .intercept(async (req, res) => {
        res.json(generateEntitiesPage(10, 11));
      });
    onGetToWorkspacePath('/entities/-10')
      .successJson({
        id: -10,
        name: 'PreSelected',
      });
  },
  beforeMount() {
    this.value = -10;
  },
});

export const PreSelectedValueLoading = () => ({
  ...setupStory(),
  beforeCreate() {
    startApiSpec()
      .intercept(async (req, res) => {
        res.json(generateEntitiesPage(10, 11));
      });
    onGetToWorkspacePath('/entities/-10')
      .neverEndingRequest();
  },
  beforeMount() {
    this.value = -10;
  },
});
PreSelectedValueLoading.story = storyshotsStory({
  async setup(page) {
    await pauseAndResetInputLoaderAnimation(page);
  },
});

// noinspection JSUnusedGlobalSymbols
export const PreSelectedValueLoadingFailed = () => ({
  ...setupStory(),
  beforeCreate() {
    startApiSpec()
      .intercept(async (req, res) => {
        res.json(generateEntitiesPage(10, 11));
      });
    onGetToWorkspacePath('/entities/-10')
      .intercept((req, res) => {
        res.status(404);
      });
  },
  beforeMount() {
    this.value = -10;
  },
});
