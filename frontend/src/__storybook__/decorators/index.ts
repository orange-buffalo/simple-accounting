import { createStoriesAppDecorator } from '@/__storybook__/decorators/stories-app';
import { createWorkspaceMockDecorator } from '@/__storybook__/decorators/stories-workspace-mocks';
import { createApiMockDecorator } from '@/__storybook__/decorators/stories-api-mocks';
import { createScreenshotTestsDecorator } from '@/__storybook__/decorators/screenshot-tests';

export const decorators = [
  createWorkspaceMockDecorator(),
  createApiMockDecorator(),
  createStoriesAppDecorator(),
  createScreenshotTestsDecorator(),
  // TODO
  // addDecorator(createRouterMockDecorator());
];
