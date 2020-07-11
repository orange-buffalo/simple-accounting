// eslint-disable-next-line import/no-extraneous-dependencies
import { API_FATAL_ERROR_EVENT } from '@/services/events';

export default {
  title: 'Other|ApiErrorHandling',
};

export const ApiFatalError = () => ({
  data() {
    return {
      emitApiFatalErrorEvent() {
        API_FATAL_ERROR_EVENT.emit();
      },
    };
  },
  template: '<ElButton @click="emitApiFatalErrorEvent">Emit event</ElButton>',
  mounted() {
    this.emitApiFatalErrorEvent();
  },
});
