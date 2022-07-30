import type { Story, StoryContext, VueFramework } from '@storybook/vue3';
import { fetchMock } from '@/__storybook__/api-mocks';
import { setRequestTimeout } from '@/services/api/interceptors/timeout-interceptor';

export function createApiMockDecorator() {
  return (fn: Story, context: StoryContext<VueFramework>) => {
    fetchMock.reset();
    // support never ending requests
    setRequestTimeout(99999999);

    return fn({}, context);
  };
}
