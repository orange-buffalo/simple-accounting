export default {
  title: 'Components/Other/ErrorHandler',
};

export const Default = () => ({
  template: `
    Erroneous expression: {{ undefinedVariable.execute() }}
  `,
});
