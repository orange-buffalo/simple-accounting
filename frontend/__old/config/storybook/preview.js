import { addDecorator } from '@storybook/vue';
import { createStoreMockDecorator } from '../../tests/storybook/utils/stories-store-mocks';
import { createApiMockDecorator } from '../../tests/storybook/utils/stories-api-mocks';
import createRouterMockDecorator from '../../tests/storybook/utils/stories-router-mocks';

// eslint-disable-next-line import/prefer-default-export
export const parameters = { layout: 'fullscreen' };

addDecorator(createStoreMockDecorator());
addDecorator(createApiMockDecorator());
// addDecorator(createStoriesAppDecorator());
addDecorator(createRouterMockDecorator());
