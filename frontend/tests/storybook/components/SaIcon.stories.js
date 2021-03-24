import { action } from '@storybook/addon-actions';
import SaIcon from '@/components/SaIcon';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components/SaIcon',
};

// noinspection JSUnusedGlobalSymbols
export const Default = () => ({
  components: { SaIcon },
  methods: {
    onClick() {
      action('on-click')();
    },
  },
  template: '<SaIcon icon="hourglass" @click="onClick" />',
});

// noinspection JSUnusedGlobalSymbols
export const AllIcons = () => ({
  components: { SaIcon },
  data() {
    return {
      supportedIcons: ['attachment', 'calendar', 'cancel', 'category', 'copy', 'customer', 'customers-overview',
        'dashboard', 'delete', 'doc', 'draft', 'empty-box', 'error', 'expense', 'file', 'gear', 'google-drive',
        'hourglass', 'income', 'income-solid', 'income-tax-payments-overview', 'invoice', 'invoices-overview',
        'jpg', 'login', 'logout', 'multi-currency', 'notes', 'password', 'pdf', 'pencil-solid',
        'percent', 'plus-thin', 'profile', 'profit', 'reporting', 'send-solid', 'share', 'success', 'tax',
        'taxes-overview', 'upload', 'warning-circle', 'workspaces', 'zip'],
    };
  },
  template: `<div>
      <SaIcon style="width: 30px; margin-right: 10px; margin-top: 10px;"
              v-for="supportedIcon in supportedIcons"
              :key="supportedIcon"
              :icon="supportedIcon" />
      </div>`,
});
