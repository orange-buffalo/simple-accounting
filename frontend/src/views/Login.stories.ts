import Login from '@/views/Login.vue';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Pages/Login',
};

// noinspection JSUnusedGlobalSymbols
export const Default = () => ({
  components: { Login },
  template: '<Login/>',
});
Default.parameters = {
  fullScreen: true,
};
