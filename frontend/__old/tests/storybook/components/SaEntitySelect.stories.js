import { action } from '@storybook/addon-actions';
import SaEntitySelect from '@/components/SaEntitySelect';
import { onGetToWorkspacePath, apiPage, responseDelay } from '../utils/stories-api-mocks';
import {
  pauseAndResetInputLoaderAnimation, removeSvgAnimations, setViewportHeight, timeout,
} from '../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components/SaEntitySelect',
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

InitialLoading.parameters = {
  storyshots: {
    async setup(page) {
      await toggleSelectInStoryshots(page);
      await removeSvgAnimations(page);
    },
  },
};

export const InitialLoadingFailure = () => ({
  ...setupStory(),
  beforeCreate() {
    startApiSpec()
      .intercept((req, res) => {
        res.status(404);
      });
  },
});

InitialLoadingFailure.parameters = {
  storyshots: {
    async setup(page) {
      await toggleSelectInStoryshots(page);
    },
  },
};

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
Loading.parameters = {
  storyshots: {
    async setup(page) {
      await toggleSelectInStoryshots(page);
      await setViewportHeight(page, 350);
    },
  },
};

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
FailingSearch.parameters = {
  storyshots: false,
};

export const NoDataAfterInitialLoading = () => ({
  ...setupStory(),
  beforeCreate() {
    startApiSpec()
      .successJson(apiPage([]));
  },
});
NoDataAfterInitialLoading.parameters = {
  storyshots: {
    async setup(page) {
      await toggleSelectInStoryshots(page);
    },
  },
};

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
NoDataOnSearch.parameters = {
  storyshots: false,
};

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
PreSelectedValueLoading.parameters = {
  storyshots: {
    async setup(page) {
      await pauseAndResetInputLoaderAnimation(page);
    },
  },
};

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
