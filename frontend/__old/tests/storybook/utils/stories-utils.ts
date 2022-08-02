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
