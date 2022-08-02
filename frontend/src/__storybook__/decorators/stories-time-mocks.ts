import FakeTimers from '@sinonjs/fake-timers';
import { decoratorFactory } from '@/__storybook__/decorators/decorator-utils';

let clock : FakeTimers.InstalledClock | null = null;

export const createTimeMockDecorator = decoratorFactory(() => {
  if (!clock) {
    clock = FakeTimers.install({
      toFake: ['Date'],
    });
  }
  clock.setSystemTime(new Date('2020-01-04T00:00:00'));

  return {
    template: '<story/>',
  };
});
