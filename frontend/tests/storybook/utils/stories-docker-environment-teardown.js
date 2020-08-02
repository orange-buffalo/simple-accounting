module.exports = async () => {
  const { dockerEnvironment } = process;
  if (!dockerEnvironment) {
    console.warn('Docker environment was not started, nothing to shutdown');
    return;
  }

  console.info('Starting shutdown..');
  if (dockerEnvironment.chromeContainer) {
    console.info(`Removing chrome container ${dockerEnvironment.chromeContainer.id}`);
    await dockerEnvironment.chromeContainer.stop();
    await dockerEnvironment.chromeContainer.remove();
  }
  if (dockerEnvironment.storybookContainer) {
    console.info(`Removing storybook container ${dockerEnvironment.storybookContainer.id}`);
    await dockerEnvironment.storybookContainer.stop();
    await dockerEnvironment.storybookContainer.remove();
  }
  if (dockerEnvironment.network) {
    console.info(`Removing network ${dockerEnvironment.network.id}`);
    await dockerEnvironment.network.remove();
  }
  console.info('Shutdown complete');
};
