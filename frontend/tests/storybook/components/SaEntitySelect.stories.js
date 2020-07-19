import SaEntitySelect from '@/components/SaEntitySelect';
import { onGetToWorkspacePath, apiPage, responseDelay } from '../utils/stories-api-mocks';
import { action } from '@storybook/addon-actions';

export default {
  title: 'Components|SaEntitySelect',
};

let nextId = 1;

function generateEntitiesPage(pageSize, totalSize, searchText) {
  const prefix = searchText ? searchText.eq : 'Entity';
  const data = [];
  for (let i = 0; i < pageSize; i++) {
    data[i] = {
      id: nextId++,
      name: `${prefix} ${i}`,
    };
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

export const InitialLading = () => ({
  ...setupStory(),
  beforeCreate() {
    startApiSpec()
      .neverEndingRequest();
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

export const NoDataAfterInitialLoading = () => ({
  ...setupStory(),
  beforeCreate() {
    startApiSpec()
      .successJson(apiPage([]));
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
