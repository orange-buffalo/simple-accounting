import Docker from 'dockerode';
import { timeout } from './stories-utils';

const docker = new Docker();

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
};

const devChromeHostConfig = {
  NetworkMode: 'host',
};

const isRunningInCiMode = process.env.CI;

export async function setupDockerEnvironment() {
  const chromeConfig = isRunningInCiMode ? ciChromeConfig : devChromeConfig;
  const chromeHostConfig = isRunningInCiMode ? ciChromeHostConfig : devChromeHostConfig;

  const dockerEnvironment = {
    chromePort: 3000,
  };

  try {
    dockerEnvironment.chromeContainer = await docker.createContainer({
      Image: 'browserless/chrome',
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

    await dockerEnvironment.chromeContainer.start();

    await waitForOutput(dockerEnvironment.chromeContainer, 'Final configuration');

    if (isRunningInCiMode) {
      const inspect = await dockerEnvironment.chromeContainer.inspect();
      dockerEnvironment.chromePort = await inspect.NetworkSettings.Ports['3000/tcp'][0].HostPort;
    }

    console.info(`Running Chrome on port ${dockerEnvironment.chromePort},
        container ${dockerEnvironment.chromeContainer.id}`);
  } catch (e) {
    console.error('Failed to start docker', e);
  }

  return dockerEnvironment;
}

export async function shutdownDockerEnvironment(dockerEnvironment) {
  if (dockerEnvironment.chromeContainer) {
    await dockerEnvironment.chromeContainer.stop();
  }
}

async function waitForOutput(container, logSubString) {
  console.info(`Waiting for ${container.id}`);

  for (let i = 0; i < 60; i += 1) {
    // eslint-disable-next-line no-await-in-loop
    const logsBuffer = await container.logs({
      tail: 5,
      stdout: true,
      stderr: true,
    });

    const logs = logsBuffer.toString();
    if (logs.indexOf(logSubString) >= 0) {
      console.info('Successfully waited for container');
      return;
    }

    // eslint-disable-next-line no-await-in-loop
    await timeout(1000);
  }
  throw Error(`Cannot find '${logSubString}' in ${container.id} logs`);
}
