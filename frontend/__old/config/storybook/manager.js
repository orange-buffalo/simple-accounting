import { addons } from '@storybook/addons';
import simpleAccountingTheme from './simple-accounting-theme';

addons.setConfig({
  panelPosition: 'right',
  showPanel: false,
  theme: simpleAccountingTheme,
});
