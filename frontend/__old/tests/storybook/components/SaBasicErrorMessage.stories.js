import SaBasicErrorMessage from '@/components/SaBasicErrorMessage';

export default {
  title: 'Components/SaBasicErrorMessage',
};

export const Default = () => ({
  components: { SaBasicErrorMessage },
  template: '<SaBasicErrorMessage />',
});

export const CustomMessage = () => ({
  components: { SaBasicErrorMessage },
  template: '<SaBasicErrorMessage message="Custom error happened" />',
});
