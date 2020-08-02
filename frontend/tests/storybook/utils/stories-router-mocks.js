import { action } from '@storybook/addon-actions';
import { router } from './stories-app';

// inspired by storybook-vue-router

export default function createRouterMockDecorator() {
  router.push = async function push(location) {
    action('router-push')(location);
  };

  return () => ({
    template: '<story/>',
  });
}
