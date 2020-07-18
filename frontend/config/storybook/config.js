// eslint-disable-next-line import/no-extraneous-dependencies
import { addDecorator } from '@storybook/vue';
// eslint-disable-next-line import/no-extraneous-dependencies
import { createStoriesAppDecorator } from '@/stories/utils/stories-app';
import { createStoreMockDecorator } from '@/stories/utils/stories-store-mocks';
import { createApiMockDecorator } from '@/stories/utils/stories-api-mocks';
import createRouterMockDecorator from '@/stories/utils/stories-router-mocks';

addDecorator(createStoreMockDecorator());
addDecorator(createApiMockDecorator());
addDecorator(createStoriesAppDecorator());
addDecorator(createRouterMockDecorator());
