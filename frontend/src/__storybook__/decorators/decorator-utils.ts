import type { Story, StoryContext } from '@storybook/vue3';
import type { SaStoryParameters, SaStoryComponent } from '@/__storybook__/sa-storybook';

export function decoratorFactory(decorator: (parameters: SaStoryParameters) => SaStoryComponent) {
  return () => ((fn: Story, { parameters }: StoryContext) => decorator(parameters as SaStoryParameters));
}
