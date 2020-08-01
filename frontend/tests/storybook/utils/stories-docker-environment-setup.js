import Docker from 'dockerode';
import { timeout } from './stories-utils';

const docker = new Docker();

const networkName = 'sa-screenshot-tests';

const ciChromeConfig = {
  ExposedPorts: {
    3000: {},
  },
};

const devChromeConfig = {};

const ciChromeHostConfig = {
  PortBindings: {
    3000: [{}],
  },
  NetworkMode: networkName,
};

const devChromeHostConfig = {
  NetworkMode: 'host',
};

const isRunningInCiMode = process.env.CI;

if (isRunningInCiMode) {
  console.info('Setting up Docker environment for CI');
} else {
  console.info('Setting up development Docker environment');
}

module.exports = async () => {
  try {
    process.dockerEnvironment = {
      ...(await setupNetwork()),
      ...(await setupStorybookContainer()),
      ...(await setupChromeContainer()),
    };
  } catch (e) {
    console.error('Failed to create Docker environment', e);
    throw e;
  }
};

async function setupNetwork() {
  const networkRuntime = {};
  if (isRunningInCiMode) {
    console.info('Creating network..');

    networkRuntime.network = await docker.createNetwork({
      Name: networkName,
    });

    console.info(`Network ${networkRuntime.network.id} created`);
  }
  return networkRuntime;
}

async function setupStorybookContainer() {
  const storybookRuntime = {
    storybookUrl: 'http://localhost:6006',
  };
  if (isRunningInCiMode) {
    console.info('Creating storybook container..');

    const containerName = 'sa-screenshot-tests-storybook';
    const image = 'node:lts';

    await pullImage(image);

    storybookRuntime.storybookContainer = await docker.createContainer({
      name: containerName,
      Image: image,
      WorkingDir: '/home/node/app',
      Cmd: ['npm', 'run', 'storybook:serve'],
      HostConfig: {
        AutoRemove: true,
        Binds: [`${process.cwd()}:/home/node/app`],
        NetworkMode: networkName,
      },
    });

    await storybookRuntime.storybookContainer.start();

    await waitForOutput(storybookRuntime.storybookContainer, 'On your network:');

    storybookRuntime.storybookUrl = `http://${containerName}:6006`;

    console.log(`Created storybook container ${storybookRuntime.storybookContainer.id}`);
  }

  return storybookRuntime;
}

async function setupChromeContainer() {
  const chromeConfig = isRunningInCiMode ? ciChromeConfig : devChromeConfig;
  const chromeHostConfig = isRunningInCiMode ? ciChromeHostConfig : devChromeHostConfig;
  const chromeRuntime = {
    chromeUrl: 'ws://localhost:3000',
  };

  console.log('Creating chrome container...');

  const image = 'browserless/chrome';
  await pullImage(image);

  chromeRuntime.chromeContainer = await docker.createContainer({
    name: 'sa-screenshot-tests-chrome',
    Image: image,
    Env: [
      'CONNECTION_TIMEOUT=600000',
      'MAX_CONCURRENT_SESSIONS=1',
    ],
    ...chromeConfig,
    HostConfig: {
      AutoRemove: true,
      ...chromeHostConfig,
    },
  });

  await chromeRuntime.chromeContainer.start();

  await waitForOutput(chromeRuntime.chromeContainer, 'Final configuration');

  if (isRunningInCiMode) {
    const inspect = await chromeRuntime.chromeContainer.inspect();
    const chromePort = await inspect.NetworkSettings.Ports['3000/tcp'][0].HostPort;
    chromeRuntime.chromeUrl = `ws://localhost:${chromePort}`;
  }

  console.info(`Chrome container started ${chromeRuntime.chromeContainer.id}, listening at ${chromeRuntime.chromeUrl}`);

  return chromeRuntime;
}

function pullImage(tag) {
  console.info(`Pulling ${tag}...`);

  return new Promise((resolve, reject) => {
    // eslint-disable-next-line consistent-return
    docker.pull(tag, (err, stream) => {
      docker.modem.followProgress(stream, onFinished);

      function onFinished(e, output) {
        if (e) reject(e); else resolve(output);
      }
    });
  });
}

async function waitForOutput(container, logSubString) {
  console.info('Waiting for container to start...');

  for (let i = 0; i < 60; i += 1) {
    // eslint-disable-next-line no-await-in-loop
    const logsBuffer = await container.logs({
      tail: 1000,
      stdout: true,
      stderr: true,
    });

    const logs = logsBuffer.toString();
    if (logs.indexOf(logSubString) >= 0) {
      console.info('Container started');
      return;
    }

    // eslint-disable-next-line no-await-in-loop
    await timeout(1000);
  }
  throw Error(`Cannot find '${logSubString}' in ${container.id} logs`);
}
