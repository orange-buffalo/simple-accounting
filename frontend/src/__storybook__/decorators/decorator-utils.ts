import type { StoryFn, StoryContext } from '@storybook/vue3';
import type { SaStoryParameters, SaStoryComponent } from '@/__storybook__/sa-storybook';

export function decoratorFactory(decorator: (parameters: SaStoryParameters) => SaStoryComponent) {
  return () => ((fn: StoryFn, { parameters }: StoryContext) => decorator(parameters as SaStoryParameters));
}
