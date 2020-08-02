export function timeout(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function pauseAndResetInputLoaderAnimation(page) {
  await pauseAndResetAnimation(page, '.sa-input-loader__indicator');
}

export async function pauseAndResetDocumentLoaderAnimation(page) {
  await pauseAndResetAnimation(page, '.sa-document__loader__file-icon');
  await pauseAndResetAnimation(page, '.sa-document__loader__file-description__header');
  await pauseAndResetAnimation(page, '.sa-document__loader__file-description__link');
  await pauseAndResetAnimation(page, '.sa-document__loader__file-description__size');
}

export async function pauseAndResetOutputLoaderAnimation(page) {
  await pauseAndResetAnimation(page, '.sa-output-loader__placeholder');
}

export async function pauseAndResetAnimation(page, selector) {
  const elements = await page.$$(selector);
  for (let i = 0; i < elements.length; i += 1) {
    elements[i].evaluate((elementNode) => {
      // eslint-disable-next-line no-param-reassign
      elementNode.style = 'animation:none';
    });
  }
}

export async function removeSvgAnimations(page) {
  const elements = await page.$$('.sa-icon__svg');
  for (let i = 0; i < elements.length; i += 1) {
    // eslint-disable-next-line no-await-in-loop
    await elements[i].evaluate((elementNode) => new Promise((resolve) => {
      const animateTransform = elementNode.querySelector('animateTransform');
      if (animateTransform) {
        animateTransform.parentNode.removeChild(animateTransform);
        setTimeout(() => {
          resolve();
        }, 0);
      } else {
        resolve();
      }
    }));
  }
}

export const NO_STORYSHOTS_STORY = {
  parameters: {
    storyshots: false,
  },
};

export function storyshotsStory(storyshotsConfig) {
  return {
    parameters: {
      storyshots: {
        ...storyshotsConfig,
      },
    },
  };
}

export async function setViewportHeight(page, height) {
  await page.setViewport({
    width: 1200,
    height,
  });
}
