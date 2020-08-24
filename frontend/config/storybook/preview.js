// eslint-disable-next-line import/no-extraneous-dependencies
import { addDecorator } from '@storybook/vue';
import { createStoriesAppDecorator } from '../../tests/storybook/utils/stories-app';
import { createStoreMockDecorator } from '../../tests/storybook/utils/stories-store-mocks';
import { createApiMockDecorator } from '../../tests/storybook/utils/stories-api-mocks';
import createRouterMockDecorator from '../../tests/storybook/utils/stories-router-mocks';

addDecorator(createStoreMockDecorator());
addDecorator(createApiMockDecorator());
addDecorator(createStoriesAppDecorator());
addDecorator(createRouterMockDecorator());
