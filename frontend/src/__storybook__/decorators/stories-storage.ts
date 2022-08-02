import { decoratorFactory } from '@/__storybook__/decorators/decorator-utils';

export const createStorageDecorator = decoratorFactory(() => {
  localStorage.clear();

  return {
    template: '<story/>',
  };
});
