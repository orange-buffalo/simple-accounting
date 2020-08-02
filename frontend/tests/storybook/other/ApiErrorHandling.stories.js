import { API_FATAL_ERROR_EVENT } from '@/services/events';
import { NO_STORYSHOTS_STORY } from '../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
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
ApiFatalError.story = NO_STORYSHOTS_STORY;
