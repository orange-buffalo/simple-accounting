declare module '*.vue' {
  // eslint-disable-next-line import/no-duplicates
  import Vue from 'vue';

  export default Vue;
}

declare module '@/*' {
  // eslint-disable-next-line import/no-duplicates
  import Vue from 'vue';

  export default Vue;
}
