export type WizardAction<StepId extends string> = {
  label: string;
  disabled?: boolean;
} & ({ target: StepId; action?: never } | { action: () => unknown; target?: never });

export type WizardStep<StepId extends string> = {
  id: StepId;
  title: string;
  description?: string | null;
  status?: 'wait' | 'process' | 'finish' | 'error' | 'success';
  navigationDisabled?: boolean;
  onEnter?: () => void;
  back?: WizardAction<StepId>;
  next?: WizardAction<StepId>;
};
