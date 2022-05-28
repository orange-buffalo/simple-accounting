import { Page } from 'puppeteer';

export function timeout(ms: number) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export async function pauseAndResetInputLoaderAnimation(page: Page) {
  await pauseAndResetAnimation(page, '.sa-input-loader__indicator');
}

export async function pauseAndResetDocumentLoaderAnimation(page: Page) {
  await pauseAndResetAnimation(page, '.sa-document__loader__file-icon');
  await pauseAndResetAnimation(page, '.sa-document__loader__file-description__header');
  await pauseAndResetAnimation(page, '.sa-document__loader__file-description__link');
  await pauseAndResetAnimation(page, '.sa-document__loader__file-description__size');
}

export async function pauseAndResetOutputLoaderAnimation(page: Page) {
  await pauseAndResetAnimation(page, '.sa-output-loader__placeholder');
}

export async function pauseAndResetAnimation(page: Page, selector: string) {
  const elements = await page.$$(selector);
  for (let i = 0; i < elements.length; i += 1) {
    // eslint-disable-next-line no-await-in-loop
    await elements[i].evaluate((elementNode) => {
      // eslint-disable-next-line no-param-reassign
      (elementNode as HTMLElement).style.animation = 'none';
    });
  }
}

export async function removeSvgAnimations(page: Page) {
  const elements = await page.$$('.sa-icon__svg');
  for (let i = 0; i < elements.length; i += 1) {
    // eslint-disable-next-line no-await-in-loop
    await elements[i].evaluate((elementNode) => {
      const animateTransform = elementNode.querySelector('animateTransform');
      if (animateTransform && animateTransform.parentNode) {
        animateTransform.parentNode.removeChild(animateTransform);
      }
    });
  }
}

export async function setViewportHeight(page: Page, height: number) {
  await page.setViewport({
    width: 1200,
    height,
  });
}
