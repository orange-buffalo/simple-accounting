import { router } from '@/stories/utils/stories-app';
// eslint-disable-next-line import/no-extraneous-dependencies
import { action } from '@storybook/addon-actions';

// inspired by storybook-vue-router

export default function createRouterMockDecorator() {
  router.push = async function push(location) {
    action('router-push')(location);
  };

  return () => ({
    template: '<story/>',
  });
}
