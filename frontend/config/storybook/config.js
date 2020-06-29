// eslint-disable-next-line import/no-extraneous-dependencies
import { addDecorator } from '@storybook/vue';
// eslint-disable-next-line import/no-extraneous-dependencies
import StoryRouter from 'storybook-vue-router';
// eslint-disable-next-line import/no-extraneous-dependencies
import { createStoriesAppDecorator } from '@/stories/utils/stories-app';
import { createStoreMockDecorator } from '@/stories/utils/stories-store-mocks';
import { createApiMockDecorator } from '@/stories/utils/stories-api-mocks';

addDecorator(createStoreMockDecorator());
addDecorator(createApiMockDecorator());
addDecorator(createStoriesAppDecorator());
addDecorator(StoryRouter());
