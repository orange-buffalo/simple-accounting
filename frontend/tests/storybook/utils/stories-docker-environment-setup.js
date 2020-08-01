import Docker from 'dockerode';
import { timeout } from './stories-utils';

const docker = new Docker();

const networkName = 'sa-screenshot-tests';
const isRunningInCiMode = process.env.CI;
const isRunningOnGitHub = process.env.GITHUB_RUN_ID != null;

if (isRunningInCiMode) {
  console.info('Setting up Docker environment for CI');
} else {
  console.info('Setting up development Docker environment');
}

module.exports = async () => {
  try {
    const gitHubBuildContainer = await getGitHubBuildContainer();

    process.dockerEnvironment = {
      ...(await setupNetwork()),
      ...(await setupStorybookContainer(gitHubBuildContainer)),
      ...(await setupChromeContainer(gitHubBuildContainer)),
    };
  } catch (e) {
    console.error('Failed to create Docker environment', e);
    throw e;
  }
};

async function getGitHubBuildContainer() {
  if (isRunningOnGitHub) {
    console.info('Running on GitHub CI, searching for the build container...');
    const containers = await docker.listContainers(); // get all containers running on the Docker host
    for (let i = 0; i < containers.length; i += 1) {
      const container = containers[i];
      if (container.Image.indexOf('orangebuffalo/simple-accounting-ci') === 0) {
        console.info(`${container.Id} is the build container`);
        return container;
      }
    }
    throw Error(`Cannot find the CI container, containers are ${JSON.stringify(containers)}`);
  }
  return null;
}

async function setupNetwork() {
  const networkRuntime = {};
  if (isRunningInCiMode && !isRunningOnGitHub) {
    console.info('Creating network..');

    networkRuntime.network = await docker.createNetwork({
      Name: networkName,
    });

    console.info(`Network ${networkRuntime.network.id} created`);
  }
  return networkRuntime;
}

async function setupStorybookContainer(gitHubBuildContainer) {
  const storybookRuntime = {
    storybookUrl: 'http://localhost:6006',
  };
  if (isRunningInCiMode) {
    console.info('Creating storybook container..');

    const containerName = 'sa-screenshot-tests-storybook';
    const image = 'node:lts';

    await pullImage(image);

    let networkToJoin;
    if (isRunningOnGitHub) {
      networkToJoin = gitHubBuildContainer.HostConfig.NetworkMode;
    } else if (isRunningInCiMode) {
      networkToJoin = networkName;
    }
    console.info(`Will join ${networkToJoin} network...`);

    storybookRuntime.storybookContainer = await docker.createContainer({
      name: containerName,
      Image: image,
      WorkingDir: '/home/node/app',
      Cmd: ['npm', 'run', 'storybook:serve'],
      HostConfig: {
        Binds: [`${await getHostFrontendDirectory(gitHubBuildContainer)}:/home/node/app`],
        NetworkMode: networkToJoin,
      },
    });

    await storybookRuntime.storybookContainer.start();

    await waitForOutput(storybookRuntime.storybookContainer, 'On your network:');

    storybookRuntime.storybookUrl = `http://${containerName}:6006`;

    console.log(`Created storybook container ${storybookRuntime.storybookContainer.id}`);
  }

  return storybookRuntime;
}

async function getHostFrontendDirectory(gitHubBuildContainer) {
  if (process.env.GITHUB_WORKSPACE) {
    // we run Docker in Docker on CI and must bind the host path
    const containerAbsolutePath = process.env.GITHUB_WORKSPACE;
    console.info(`Running on GitHub CI, looking for the host path mounting ${containerAbsolutePath}...`);
    const mounts = gitHubBuildContainer.Mounts;
    if (mounts) {
      for (let i = 0; i < mounts.length; i += 1) {
        const mount = mounts[i];
        // find a mount that corresponds to the workspace and get its source
        if (mount.Destination && containerAbsolutePath.indexOf(mount.Destination) === 0) {
          console.info(`Found a mount ${mount.Source}:${mount.Destination} for the workspace...`);
          const relativeWorkspacePath = containerAbsolutePath.replace(mount.Destination, '');
          const hostAbsolutePath = mount.Source + relativeWorkspacePath;
          console.info(`Host path is ${hostAbsolutePath}`);
          return `${hostAbsolutePath}/frontend`;
        }
      }
    }
    throw Error(`Cannot find the workspace mount, container is ${JSON.stringify(gitHubBuildContainer)}`);
  } else {
    const frontendDir = process.cwd();
    console.info(`Running on local CI, mounting ${frontendDir} for storybook`);
    return frontendDir;
  }
}

async function setupChromeContainer(gitHubBuildContainer) {
  console.log('Creating chrome container...');

  let chromeConfig = {};
  if (isRunningInCiMode && !isRunningOnGitHub) {
    chromeConfig = {
      ExposedPorts: {
        3000: {},
      },
    };
  }

  let chromeHostConfig;
  if (isRunningOnGitHub) {
    const gitHubBuildNetworkName = gitHubBuildContainer.HostConfig.NetworkMode;
    console.info(`Will join ${gitHubBuildNetworkName} network...`);
    chromeHostConfig = {
      NetworkMode: gitHubBuildNetworkName,
    };
  } else if (isRunningInCiMode) {
    console.info(`Will join ${networkName} network...`);
    chromeHostConfig = {
      PortBindings: {
        3000: [{}],
      },
      NetworkMode: networkName,
    };
  } else {
    chromeHostConfig = {
      NetworkMode: 'host',
    };
  }

  const chromeRuntime = {
    chromeUrl: 'ws://localhost:3000',
  };

  const image = 'browserless/chrome';
  await pullImage(image);

  const containerName = 'sa-screenshot-tests-chrome';
  chromeRuntime.chromeContainer = await docker.createContainer({
    name: containerName,
    Image: image,
    Env: [
      'CONNECTION_TIMEOUT=600000',
      'MAX_CONCURRENT_SESSIONS=1',
    ],
    ...chromeConfig,
    HostConfig: {
      ...chromeHostConfig,
    },
  });

  await chromeRuntime.chromeContainer.start();

  await waitForOutput(chromeRuntime.chromeContainer, 'Final configuration');

  if (isRunningOnGitHub) {
    chromeRuntime.chromeUrl = `ws://${containerName}:3000`;
  } else if (isRunningInCiMode) {
    const inspect = await chromeRuntime.chromeContainer.inspect();
    const chromePort = inspect.NetworkSettings.Ports['3000/tcp'][0].HostPort;
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

  let logs = null;
  for (let i = 0; i < 120; i += 1) {
    // eslint-disable-next-line no-await-in-loop
    const logsBuffer = await container.logs({
      tail: 1000,
      stdout: true,
      stderr: true,
    });

    logs = logsBuffer.toString();
    if (logs.indexOf(logSubString) >= 0) {
      console.info('Container started');
      return;
    }

    // eslint-disable-next-line no-await-in-loop
    await timeout(1000);
  }

  throw Error(`Cannot find '${logSubString}' in ${container.id} logs. The last fetched logs are: \n${logs}`);
}
