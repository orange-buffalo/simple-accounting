describe('an event', () => {
  let loginRequiredEvent;

  beforeEach(() => {
    jest.resetModules();
    loginRequiredEvent = require('@/services/events').LOGIN_REQUIRED_EVENT;
  });

  it('should invoke all subscribers', () => {
    const firstSubscriber = jest.fn();
    const secondSubscriber = jest.fn();

    loginRequiredEvent.subscribe(firstSubscriber);
    loginRequiredEvent.subscribe(secondSubscriber);

    loginRequiredEvent.emit();

    expect(firstSubscriber.mock.calls.length)
      .toBe(1);
    expect(secondSubscriber.mock.calls.length)
      .toBe(1);
  });

  it('should pass event data to the subscriber', () => {
    const subscriber = jest.fn();

    loginRequiredEvent.subscribe(subscriber);

    loginRequiredEvent.emit('data');

    expect(subscriber.mock.calls.length)
      .toBe(1);
    expect(subscriber.mock.calls[0][0])
      .toBe('data');
  });

  it('should not invoke unsubscribed subscriber', () => {
    const subscriber = jest.fn();

    loginRequiredEvent.subscribe(subscriber);
    loginRequiredEvent.unsubscribe(subscriber);

    loginRequiredEvent.emit();

    expect(subscriber.mock.calls.length)
      .toBe(0);
  });
});
