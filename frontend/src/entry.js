import '@/styles/loader.scss';

const appDiv = document.getElementById('app');
appDiv.style.display = 'none';

const loaderDiv = document.createElement('div');
loaderDiv.className = 'app-loader-screen';

loaderDiv.innerHTML += `
<div class="app-loader-container">
  <h1>simple-accounting</h1>
  <div class="app-loader">
  </div>
</div>
`;

document.body.appendChild(loaderDiv);

import('./main').then(() => {
  loaderDiv.setAttribute('style', 'opacity: 0');
  setTimeout(() => loaderDiv.remove(), 500);
});
