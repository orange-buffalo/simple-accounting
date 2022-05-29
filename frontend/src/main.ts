import '@/styles/loader.scss';

const appDiv = document.getElementById('simple-accounting');
if (appDiv == null) {
  throw new Error('Broken setup');
}
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

import(/* webpackPreload: true, webpackChunkName: "setup-app" */ '@/setup/setup-app')
  .then(setupAppDeferred => {
     console.log(setupAppDeferred);
setupAppDeferred.default();
  })
//
// const setupAppDeferred = await import(/* webpackPreload: true, webpackChunkName: "setup-app" */ '@/setup/setup-app');
// console.log(setupAppDeferred);
// setupAppDeferred.default();
