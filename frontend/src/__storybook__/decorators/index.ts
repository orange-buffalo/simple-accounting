import { createStoriesAppDecorator } from '@/__storybook__/decorators/stories-app';
import { createWorkspaceMockDecorator } from '@/__storybook__/decorators/stories-workspace-mocks';
import { createApiMockDecorator } from '@/__storybook__/decorators/stories-api-mocks';
import { createScreenshotTestsDecorator } from '@/__storybook__/decorators/screenshot-tests';
import { createStorageDecorator } from '@/__storybook__/decorators/stories-storage';
import { createTimeMockDecorator } from '@/__storybook__/decorators/stories-time-mocks';
// init API client
import '@/services/api';

export const decorators = [
  createTimeMockDecorator(),
  createStorageDecorator(),
  createWorkspaceMockDecorator(),
  createApiMockDecorator(),
  createStoriesAppDecorator(),
  createScreenshotTestsDecorator(),
  // TODO
  // addDecorator(createRouterMockDecorator());
];
