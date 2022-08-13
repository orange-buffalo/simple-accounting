import type { ComponentOptionsMixin } from 'vue';
import type { WorkspaceDto } from '@/services/api';

export type SaStoryComponent = ComponentOptionsMixin;

export type SaStoryScreenshotPreparation = () => boolean;

export interface SaStoryParameters {
  fullScreen?: boolean,
  asPage?: boolean,
  workspace?: Partial<WorkspaceDto>,
  screenshotPreparation?: SaStoryScreenshotPreparation
}

export function defineStory(storyComponent: () => SaStoryComponent, parameters?: SaStoryParameters) {
  // eslint-disable-next-line
  const story: any = storyComponent;
  story.parameters = parameters;
  return story;
}
