import { decoratorFactory } from '@/__storybook__/decorators/decorator-utils';
import type { SaStoryScreenshotPreparation } from '@/__storybook__/sa-storybook';
import { allOf, skipWhenSucceededOnce } from '@/__storybook__/screenshots';

const waitDelayIncrementMs = 50;

function waitFor(condition: () => boolean, callback: () => void, delayMs: number) {
  setTimeout(() => {
    const conditionMet = condition();
    if (conditionMet) {
      callback();
    } else {
      waitFor(condition, callback, Math.min(delayMs + waitDelayIncrementMs, 150));
    }
  }, delayMs);
}

const waitForRootRenderedWithContent: SaStoryScreenshotPreparation = () => {
  const storyRoot = document.querySelector('#storybook-root');
  return storyRoot !== null && storyRoot.childNodes.length > 0;
};

const disableCssAnimations: SaStoryScreenshotPreparation = () => {
  const style = document.createElement('style');
  document.head.appendChild(style);
  style.appendChild(document.createTextNode(`
      *, *:before, *:after {
        transition: none !important;
        animation: none !important;
        caret-color: transparent !important;
      }
      `));
  return true;
};

const defaultScreenshotPreparation: SaStoryScreenshotPreparation = allOf(
  skipWhenSucceededOnce(disableCssAnimations),
  skipWhenSucceededOnce(waitForRootRenderedWithContent),
);

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
