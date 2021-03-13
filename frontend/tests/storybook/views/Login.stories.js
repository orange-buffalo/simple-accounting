import Login from '@/views/Login';
import { setViewportHeight } from '../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Pages/Login',
  parameters: {
    fullScreen: true,
    storyshots: {
      async setup(page) {
        await setViewportHeight(page, 1000);
      },
    },
  },
};

// noinspection JSUnusedGlobalSymbols
export const Default = () => ({
  components: { Login },
  template: '<Login/>',
});
