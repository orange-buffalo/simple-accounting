import { setGlobalRequestTimeout } from '@/services/api';
import { decoratorFactory } from '@/__storybook__/decorators/decorator-utils';
import { fetchMock } from '@/__storybook__/api-mocks';

export const createApiMockDecorator = decoratorFactory(() => {
  fetchMock.reset();
  // support never ending requests
  setGlobalRequestTimeout(99999999);

  return {
    template: '<story/>',
  };
});
