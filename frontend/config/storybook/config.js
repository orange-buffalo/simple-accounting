// eslint-disable-next-line import/no-extraneous-dependencies
import { addDecorator } from '@storybook/vue';
// eslint-disable-next-line import/no-extraneous-dependencies
import centered from '@storybook/addon-centered/vue';
import { createStoriesAppDecorator } from '@/stories/utils/stories-app';
import { createStoreMockDecorator } from '@/stories/utils/stories-store-mocks';
import { createApiMockDecorator } from '@/stories/utils/stories-api-mocks';

addDecorator(centered);
addDecorator(createStoreMockDecorator());
addDecorator(createApiMockDecorator());
addDecorator(createStoriesAppDecorator());
