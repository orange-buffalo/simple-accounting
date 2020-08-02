import useNotifications from '@/components/useNotifications';
import { storyshotsStory, timeout } from '../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Other|Notifications',
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
Error.story = storyshotsStory({
  async setup() {
    await timeout(1200);
  },
});
