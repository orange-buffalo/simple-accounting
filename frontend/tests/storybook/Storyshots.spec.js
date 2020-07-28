import initStoryshots from '@storybook/addon-storyshots';
import { imageSnapshot } from '@storybook/addon-storyshots-puppeteer';
import puppeteer from 'puppeteer';
import { setViewportHeight } from './utils/stories-utils';
import { setupDockerEnvironment, shutdownDockerEnvironment } from './utils/stories-docker-environment';

let dockerEnvironment;

beforeAll(async () => {
  dockerEnvironment = await setupDockerEnvironment();
}, 300000);

let browser;

async function getCustomBrowser() {
  browser = await puppeteer.connect({ browserWSEndpoint: `ws://localhost:${dockerEnvironment.chromePort}` });
  return browser;
}

afterAll(async () => {
  if (browser) {
    await browser.close();
  }
  if (dockerEnvironment) {
    await shutdownDockerEnvironment(dockerEnvironment);
  }
}, 300000);

const getGotoOptions = ({ context: { kind, name }, url }) => {
  console.info(`Executing story ${kind} / ${name} using URL ${url}`);
  return {
    waitUntil: 'networkidle2',
  };
};

const getMatchOptions = ({ context: { parameters } }) => {
  if (parameters.storyshots && parameters.storyshots.matchOptions) {
    return parameters.storyshots.matchOptions;
  }
  return {
    failureThreshold: 0,
    failureThresholdType: 'percent',
  };
};

const beforeScreenshot = async (page, { context: { kind, name, parameters } }) => {
  await setViewportHeight(page, parameters.fullWidth ? 400 : 200);

  // eliminate flaky tests due to cursor blinking in focused inputs
  const body = await page.$('body');
  body.evaluate((bodyNode) => {
    // eslint-disable-next-line no-param-reassign
    bodyNode.style = 'caret-color: transparent';
  });

  if (parameters.storyshots && parameters.storyshots.setup) {
    console.log(`executing custom setup for ${kind} / ${name}`);
    await parameters.storyshots.setup(page);
  }
};

initStoryshots({
  storyNameRegex: process.env.STORYSHOTS_STORY_NAME && new RegExp(process.env.STORYSHOTS_STORY_NAME),
  storyKindRegex: process.env.STORYSHOTS_STORY_KIND && new RegExp(process.env.STORYSHOTS_STORY_KIND),
  configPath: 'config/storybook',
  test: imageSnapshot({
    getGotoOptions,
    beforeScreenshot,
    getMatchOptions,
    getCustomBrowser,
    storybookUrl: 'http://localhost:6006',
  }),
});
