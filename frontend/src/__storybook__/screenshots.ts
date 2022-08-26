import type { SaStoryScreenshotPreparation } from '@/__storybook__/sa-storybook';

export function waitForText(text: string, selector?: string): SaStoryScreenshotPreparation {
  return () => {
    const elements = document.querySelectorAll(selector || 'div,span,p,h1,h2,h3,h4');
    let result = false;
    elements.forEach((element) => {
      result = result || (element.innerText.indexOf(text) >= 0);
    });
    return result;
  };
}

export function waitForOutputLoaderText(text: string): SaStoryScreenshotPreparation {
  return waitForText(text, '.sa-output-loader');
}

export function disableIconsSvgAnimations(): SaStoryScreenshotPreparation {
  return () => {
    // noinspection CssInvalidHtmlTagReference
    const animations = document.querySelectorAll('animateTransform');
    if (animations.length === 0) {
      return false;
    }
    animations.forEach((animation) => animation.remove());
    return true;
  };
}

export function disableCssAnimations(querySelector: string): SaStoryScreenshotPreparation {
  return () => {
    let result = false;
    document.querySelectorAll(querySelector)
      .forEach((element) => {
        result = true;
        // eslint-disable-next-line no-param-reassign
        element.style.animation = 'none';
        // eslint-disable-next-line no-param-reassign
        element.style.transition = 'none';
      });
    return result;
  };
}

export function disableOutputLoaderAnimations() {
  return disableCssAnimations('.sa-output-loader__placeholder');
}

export function disableDocumentLoaderAnimation() {
  return allOf(
    disableCssAnimations('.sa-document__loader__file-icon'),
    disableCssAnimations('.sa-document__loader__file-description__header'),
    disableCssAnimations('.sa-document__loader__file-description__link'),
    disableCssAnimations('.sa-document__loader__file-description__size'),
  );
}

export function openOverviewPanelDetails(): SaStoryScreenshotPreparation {
  return () => {
    const detailsTrigger = document.querySelector('.overview-item__details-trigger') as HTMLElement;
    if (!detailsTrigger) return false;
    if (detailsTrigger.className.indexOf('overview-item__details-trigger_open') < 0) {
      detailsTrigger.click();
    }
    return true;
  };
}

export function disableOverviewPanelAnimations() {
  return allOf(
    disableCssAnimations('.overview-item__panel'),
    disableCssAnimations('.overview-item__details-trigger'),
    disableCssAnimations('.overview-item__details-transition-enter-active'),
  );
}

export function openOverviewPanelDetailsAndDisableAnimations(): SaStoryScreenshotPreparation {
  return allOf(openOverviewPanelDetails(), disableOverviewPanelAnimations());
}

export function allOf(...steps: SaStoryScreenshotPreparation[]): SaStoryScreenshotPreparation {
  return () => steps.every((step) => step());
}
