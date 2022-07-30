import { createStoriesAppDecorator } from '@/__storybook__/decorators/stories-app';
import { createWorkspaceMockDecorator } from '@/__storybook__/decorators/stories-workspace-mocks';
import { createApiMockDecorator } from '@/__storybook__/decorators/stories-api-mocks';

export const decorators = [
  createWorkspaceMockDecorator(),
  createApiMockDecorator(),
  createStoriesAppDecorator(),
  // TODO
  // addDecorator(createRouterMockDecorator());
];
