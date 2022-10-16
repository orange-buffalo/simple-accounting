import type { SaStoryScreenshotPreparation } from '@/__storybook__/sa-storybook';

export function waitForInputValue(value: string, selector: string): SaStoryScreenshotPreparation {
  return skipWhenSucceededOnce(() => {
    const input = document.querySelector(selector);
    return input && input.value && input.value.indexOf(value) >= 0;
  });
}

export function waitForText(text: string, selector?: string): SaStoryScreenshotPreparation {
  return skipWhenSucceededOnce(() => {
    const elements = document.querySelectorAll(selector || 'div,span,p,h1,h2,h3,h4,li,label');
    for (const element of elements) {
      const elementText = [...element.childNodes].filter((child) => child.nodeType === Node.TEXT_NODE)
        .map((child) => child.textContent.trim())
        .filter((child) => child.length)
        .join('');

      const hasText = elementText.indexOf(text) >= 0;
      if (hasText && isVisible(element)) {
        return true;
      }
    }
    return false;
  });
}

function isVisible(element: Node) {
  const computedStyle = window.getComputedStyle(element);
  if (computedStyle.display === 'none') {
    return false;
  }
  if (element.parentElement && element.parentElement !== document.body) {
    return isVisible(element.parentElement);
  }
  return true;
}

export function clickOnElement(selector: string): SaStoryScreenshotPreparation {
  return skipWhenSucceededOnce(() => {
    const element = document.querySelector(selector);
    if (!element) return false;
    element.click();
    return true;
  });
}

export function openSelectDropdown(selector: string): SaStoryScreenshotPreparation {
  return skipWhenSucceededOnce(() => {
    const element = document.querySelector(selector);
    if (!element) return false;

    element.dispatchEvent(new MouseEvent('mouseenter'));
    element.dispatchEvent(new MouseEvent('click'));

    return true;
  });
}

export function waitForElementToBeVisible(selector: string): SaStoryScreenshotPreparation {
  return skipWhenSucceededOnce(() => {
    const elements = document.querySelectorAll(selector);
    for (const element of elements) {
      if (!isVisible(element)) {
        return false;
      }
    }
    return true;
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
