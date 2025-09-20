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
   
  const yamlObject = yaml.load(yamlString) as any;

  if (yamlObject?.fullStackTestsConfig?.useViteDevServer === true) {
    let { viteDevServerSpringContextPort } = yamlObject.fullStackTestsConfig;
    if (!viteDevServerSpringContextPort) {
      viteDevServerSpringContextPort = 5174;
    }
    console.log(`Using tests as API backed via port ${viteDevServerSpringContextPort}`);
    return viteDevServerSpringContextPort;
  }

  console.info(`Tests config file found, but useViteDevServer is not set to true. Using default port: ${defaultPort}`);
  return defaultPort;
}
