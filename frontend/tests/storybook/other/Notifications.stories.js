import useNotifications from '@/components/useNotifications';
import { timeout } from '../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Other/Notifications',
};

export const Error = () => ({
  data() {
    return {
      showNotification() {
        const { showErrorNotification } = useNotifications();
        showErrorNotification({
          message: 'Some error happened, please try again later',
        });
      },
    };
  },
  template: '<ElButton @click="showNotification">Show</ElButton>',
  mounted() {
    this.showNotification();
  },
});
Error.parameters = {
  storyshots: {
    async setup() {
      await timeout(1200);
    },
  },
};
