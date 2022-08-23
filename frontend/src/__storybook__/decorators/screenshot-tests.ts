import { decoratorFactory } from '@/__storybook__/decorators/decorator-utils';
import type { SaStoryScreenshotPreparation } from '@/__storybook__/sa-storybook';

const waitDelayIncrementMs = 50;

function waitFor(condition: () => boolean, callback: () => void, delayMs: number) {
  setTimeout(() => {
    const conditionMet = condition();
    if (conditionMet) {
      callback();
    } else {
      waitFor(condition, callback, delayMs + waitDelayIncrementMs);
    }
  }, delayMs);
}

const defaultScreenshotPreparation: SaStoryScreenshotPreparation = () => {
  const storyRoot = document.querySelector('#root');
  if (!storyRoot) {
    return false;
  }
  return storyRoot.childNodes.length > 0;
};

export const createScreenshotTestsDecorator = decoratorFactory((parameters) => {
  waitFor(
    () => {
      if (window.saScreenshotRequired === true) {
        console.info('Requested to take screenshot');
        return true;
      }
      return false;
    },
    () => {
      const readyForScreenshotCondition = () => {
        if (defaultScreenshotPreparation()) {
          if (parameters.screenshotPreparation) {
            return parameters.screenshotPreparation();
          }
          return true;
        }
        return false;
      };
      waitFor(readyForScreenshotCondition, () => {
        console.info('Ready to take screenshot');
        window.saReadyForScreenshot = true;
      }, 0);
    },
    0,
  );

  return {
    template: '<story/>',
  };
});

declare global {
  // noinspection JSUnusedGlobalSymbols
  interface Window {
    saScreenshotRequired?: boolean,
    saReadyForScreenshot?: boolean
  }
}
