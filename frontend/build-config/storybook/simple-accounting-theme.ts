import type { ThemeVars } from '@storybook/theming';
import { create } from '@storybook/theming';

const theme : ThemeVars = create({
  base: 'light',
  brandTitle: 'simple-accounting',
  appBg: 'linear-gradient(190deg,#323e51,#284165 25%,#395a6d)',
  appContentBg: '#ffffff',
  appBorderRadius: 2,
  textColor: '#ffffff',
  barBg: '#ffffff',
});

export default theme;
