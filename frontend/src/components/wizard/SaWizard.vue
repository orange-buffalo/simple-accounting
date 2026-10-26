<template>
  <div class="sa-wizard">
    <ElSteps :active="activeIndex" align-center finish-status="success">
      <ElStep v-for="step in steps" :key="step.id" :description="step.description ?? undefined"
              :status="step.complete ? 'success' : step.id === model ? 'process' : 'wait'">
        <template #title>
          <span :aria-current="step.id === model ? 'step' : undefined">
            {{ step.title }}
          </span>
        </template>
      </ElStep>
    </ElSteps>
    <div class="sa-wizard__content">
      <slot :name="model" />
    </div>
    <div class="sa-wizard__navigation">
      <ElButton v-if="back" link :disabled="isActionDisabled(back)" @click="runAction(back)">
        {{ back.label ?? $t.common.back() }}
      </ElButton>
      <ElButton v-if="next" type="primary" :disabled="isActionDisabled(next)" @click="runAction(next)">
        {{ next.label ?? $t.common.next() }}
      </ElButton>
    </div>
  </div>
</template>

<script lang="ts" setup generic="StepId extends string">
  import { computed, ref } from 'vue';
  import { ElButton, ElStep, ElSteps } from 'element-plus';
  import type { WizardAction, WizardStep } from './sa-wizard';
  import { $t } from '@/services/i18n';

  const props = defineProps<{ steps: WizardStep<StepId>[]; loading?: boolean }>();
  const model = defineModel<StepId>({ required: true });
  const actionPending = ref(false);
  const activeIndex = computed(() => props.steps.findIndex(step => step.id === model.value));
  const activeStep = computed(() => props.steps[activeIndex.value]);
  const back = computed<WizardAction<StepId> | undefined>(() => activeIndex.value > 0
    ? activeStep.value.back ?? { target: props.steps[activeIndex.value - 1].id } : undefined);
  const next = computed<WizardAction<StepId> | undefined>(() => (
    activeIndex.value >= 0 && activeIndex.value < props.steps.length - 1
      ? activeStep.value.next ?? { target: props.steps[activeIndex.value + 1].id } : undefined
  ));
  const navigate = (id: StepId) => {
    const target = props.steps.find(step => step.id === id);
    if (props.loading || actionPending.value || !target || id === model.value) return;
    target.onEnter?.();
    model.value = id;
  };
  const isActionDisabled = (action: WizardAction<StepId>) => props.loading || actionPending.value || action.disabled;
  const runAction = async (action: WizardAction<StepId>) => {
    if (isActionDisabled(action)) return;
    if (action.target !== undefined) navigate(action.target);
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
    padding: clamp(16px, 3vw, 32px);
    border: 1px solid $secondary-grey;
    border-radius: $panel-border-radius;
    background: $white;
    overflow: hidden;

    :deep(.el-step__head.is-process),
    :deep(.el-step__title.is-process) { color: $components-color; border-color: $components-color; }
    :deep(.el-step__head.is-wait) { color: $secondary-text-color; border-color: $components-border-color; }
    :deep(.el-step__title.is-wait),
    :deep(.el-step__description) { color: $secondary-text-color; }
    :deep(.el-step__icon) { background: $white; }
    :deep(.el-step__line) { background: $secondary-grey; }
    :deep(.el-step__title) { font-size: 14px; line-height: 1.5; padding-top: 8px; }
    :deep(.el-step__description) { line-height: 1.5; }

    &__content { margin-top: 32px; }
    &__navigation {
      display: flex;
      justify-content: center;
      flex-wrap: wrap;
      gap: 40px;
      margin-top: 32px;
      padding-top: 24px;
      border-top: 1px solid $secondary-grey;
      :deep(.el-button + .el-button) { margin-left: 0; }
    }
  }
</style>
