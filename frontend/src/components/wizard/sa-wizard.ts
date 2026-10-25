type WizardActionOptions = {
  label?: string;
  disabled?: boolean;
};

type WizardNavigationAction<StepId extends string> = WizardActionOptions & {
  target: StepId;
  action?: never;
};

type WizardCallbackAction = WizardActionOptions & {
  action: () => unknown;
  target?: never;
};

export type WizardAction<StepId extends string> = WizardNavigationAction<StepId> | WizardCallbackAction;

export type WizardStep<StepId extends string> = {
  id: StepId;
  title: string;
  description?: string | null;
  complete?: boolean;
  onEnter?: () => void;
  back?: WizardAction<StepId>;
  next?: WizardAction<StepId>;
};
