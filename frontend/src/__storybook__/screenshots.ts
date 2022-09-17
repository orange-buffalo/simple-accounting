import type { SaStoryScreenshotPreparation } from '@/__storybook__/sa-storybook';

export function waitForText(text: string, selector?: string): SaStoryScreenshotPreparation {
  return skipWhenSucceededOnce(() => {
    const elements = document.querySelectorAll(selector || 'div,span,p,h1,h2,h3,h4');
    let result = false;
    elements.forEach((element) => {
      result = result || (element.innerText.indexOf(text) >= 0);
    });
    return result;
  });
}

export function clickOnElement(selector: string): SaStoryScreenshotPreparation {
  return skipWhenSucceededOnce(() => {
    const element = document.querySelector(selector);
    if (!element) return false;
    element.click();
    return true;
  });
}

export function waitForElementToBeVisible(selector: string): SaStoryScreenshotPreparation {
  return skipWhenSucceededOnce(() => {
    const elements = document.querySelectorAll(selector);
    return elements.length > 0;
  });
}

export function waitForElementsToBeHidden(selector: string): SaStoryScreenshotPreparation {
  return skipWhenSucceededOnce(() => {
    const elements = document.querySelectorAll(selector);
    return elements.length === 0;
  });
}

export function waitForInputLoadersToLoad(): SaStoryScreenshotPreparation {
  return waitForElementsToBeHidden('.sa-input-loader__indicator');
}

export function disableIconsSvgAnimations(): SaStoryScreenshotPreparation {
  return skipWhenSucceededOnce(() => {
    // noinspection CssInvalidHtmlTagReference
    const animations = document.querySelectorAll('animateTransform');
    if (animations.length === 0) {
      return false;
    }
    animations.forEach((animation) => animation.remove());
    return true;
  });
}

export function openOverviewPanelDetails(): SaStoryScreenshotPreparation {
  return skipWhenSucceededOnce(() => {
    const detailsTrigger = document.querySelector('.overview-item__details-trigger') as HTMLElement;
    if (!detailsTrigger) return false;
    detailsTrigger.click();
    return true;
  });
}

export function allOf(...steps: SaStoryScreenshotPreparation[]): SaStoryScreenshotPreparation {
  return () => steps.every((step) => step());
}

export function skipWhenSucceededOnce(delegate: SaStoryScreenshotPreparation): SaStoryScreenshotPreparation {
  let successfullyExecuted = false;
  return () => {
    if (!successfullyExecuted) {
      successfullyExecuted = delegate();
    }
    return successfullyExecuted;
  };
}
