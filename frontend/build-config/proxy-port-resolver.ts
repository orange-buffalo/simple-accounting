import * as yaml from 'js-yaml';
import * as fs from 'fs';

export function resolveProxyPort(): number {
  const defaultPort = 9393;
  const yamlFilePath = '../app/src/test/.test-config.yaml';

  if (!fs.existsSync(yamlFilePath)) {
    console.info(`Tests config file not found. Using default port: ${defaultPort}`);
    return defaultPort;
  }

  const yamlString = fs.readFileSync(yamlFilePath, 'utf8');
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const yamlObject = yaml.load(yamlString) as any;

  if (yamlObject?.fullStackTestsConfig?.useViteDevServer === true) {
    const { viteDevServerSpringContextPort } = yamlObject.fullStackTestsConfig;
    if (!viteDevServerSpringContextPort) {
      throw new Error('viteDevServerSpringContextPort is not defined in the tests config file');
    }
    console.log(`Using tests as API backed via port ${viteDevServerSpringContextPort}`);
    return viteDevServerSpringContextPort;
  }

  console.info(`Tests config file found, but useViteDevServer is not set to true. Using default port: ${defaultPort}`);
  return defaultPort;
}
