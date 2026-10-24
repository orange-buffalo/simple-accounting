<template>
  <div class="sa-wizard">
    <ElSteps :active="activeIndex" align-center finish-status="success">
      <ElStep v-for="step in steps" :key="step.id" :description="step.description ?? undefined" :status="step.status">
        <template #title>
          <button type="button" class="sa-wizard__step" :disabled="actionPending || step.navigationDisabled || step.id === model"
                  :aria-current="step.id === model ? 'step' : undefined" @click="navigate(step.id)">
            {{ step.title }}
          </button>
        </template>
      </ElStep>
    </ElSteps>
    <div class="sa-wizard__content">
      <slot :name="model" :navigate="navigate" />
    </div>
    <div v-if="activeStep?.back || activeStep?.next" class="sa-wizard__navigation">
      <ElButton v-if="activeStep.back" link :disabled="isActionDisabled(activeStep.back)" @click="runAction(activeStep.back)">
        {{ activeStep.back.label }}
      </ElButton>
      <ElButton v-if="activeStep.next" type="primary" :disabled="isActionDisabled(activeStep.next)" @click="runAction(activeStep.next)">
        {{ activeStep.next.label }}
      </ElButton>
    </div>
  </div>
</template>

<script lang="ts" setup generic="StepId extends string">
  import { computed, nextTick, ref } from 'vue';
  import { ElButton, ElStep, ElSteps } from 'element-plus';
  import type { WizardAction, WizardStep } from './sa-wizard';

  const props = defineProps<{ steps: WizardStep<StepId>[] }>();
  const model = defineModel<StepId>({ required: true });
  const actionPending = ref(false);
  const activeIndex = computed(() => props.steps.findIndex(step => step.id === model.value));
  const activeStep = computed(() => props.steps[activeIndex.value]);
  const navigate = async (id: StepId) => {
    await nextTick();
    const target = props.steps.find(step => step.id === id);
    if (actionPending.value || !target || target.navigationDisabled || id === model.value) return;
    target.onEnter?.();
    model.value = id;
  };
  const isActionDisabled = (action: WizardAction<StepId>) => actionPending.value || action.disabled
    || (action.target !== undefined
      && !props.steps.some(step => step.id === action.target && !step.navigationDisabled));
  const runAction = async (action: WizardAction<StepId>) => {
    if (isActionDisabled(action)) return;
    if (action.target !== undefined) await navigate(action.target);
    else if (action.action) {
      actionPending.value = true;
      try {
        await action.action();
      } finally {
        actionPending.value = false;
      }
    }
  };
</script>

<style scoped lang="scss">
  @use "@/styles/vars.scss" as *;

  .sa-wizard {
    padding: 20px;
    border: 1px solid $secondary-grey;
    border-radius: 2px;
    background: $white;
    overflow: hidden;

    &__content { margin-top: 24px; }
    &__navigation { display: flex; justify-content: center; gap: 20px; margin: 24px 0; }
    &__step {
      padding: 0;
      border: 0;
      background: none;
      color: inherit;
      font: inherit;
      cursor: pointer;
      &:disabled { cursor: default; }
      &:enabled:hover { text-decoration: underline; }
      &:focus-visible { outline: 2px solid $accent-primary-color; outline-offset: 3px; }
    }
  }
</style>
