import '@/styles/loader.scss';
import { isRunningInTest } from '@/setup/tests-support.ts';

export function createLoader() {
  if (isRunningInTest()) {
    console.log('Skipping loader creation in test environment');
    return;
  }

  const appDiv = document.getElementById('simple-accounting');
  if (appDiv == null) {
    throw new Error('Broken setup');
  }
  appDiv.style.display = 'none';

  const loaderDiv = document.createElement('div');
  loaderDiv.id = 'app-loader-screen';
  loaderDiv.className = 'app-loader-screen';

  loaderDiv.innerHTML += `
    <div class="app-loader-container">
      <h1>simple-accounting</h1>
      <div class="app-loader">
      </div>
    </div>
  `;

  document.body.appendChild(loaderDiv);
}

export function removeLoader() {
  if (isRunningInTest()) {
    console.log('Skipping loader creation in test environment');
    return;
  }

  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  const loaderDiv = document.getElementById('app-loader-screen')!;
  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  const appDiv = document.getElementById('simple-accounting')!;
  loaderDiv.setAttribute('style', 'opacity: 0');
  setTimeout(() => loaderDiv.remove(), 500);
  appDiv.style.display = '';
}
