import { Page } from 'puppeteer';

export async function pauseAndResetInputLoaderAnimation(page: Page) {
  await pauseAndResetAnimation(page, '.sa-input-loader__indicator');
}


