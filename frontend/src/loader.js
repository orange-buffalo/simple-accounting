import '@/styles/loader.scss';

document.body.innerHTML += `
<div class="app-loader-screen" id="app-loader-screen">
  <div class="app-loader-container">
    <h1>simple-accounting</h1>
    <div class="app-loader">
    </div>
  </div>
</div>
`;

import('./main').then(() => {
  const loaderScreen = document.getElementById('app-loader-screen');
  loaderScreen.setAttribute('style', 'opacity: 0');
  setTimeout(() => loaderScreen.remove(), 500);
});
