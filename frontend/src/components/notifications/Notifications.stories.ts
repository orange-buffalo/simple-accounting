// noinspection JSUnusedGlobalSymbols

import useNotifications from '@/components/notifications/use-notifications';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, clickOnElement, waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/Other/Notifications',
};

export const Error = defineStory(() => ({
  data() {
    return {
      showNotification() {
        const { showErrorNotification } = useNotifications();
        showErrorNotification('Some error happened, please try again later');
      },
    };
  },
  template: '<ElButton id="triggerButton" @click="showNotification">Show</ElButton>',
}));
