// noinspection JSUnusedGlobalSymbols

import SaMarkdownOutput from '@/components/SaMarkdownOutput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/SaMarkdownOutput',
};

const markdownSource = `
**This is bold text**

__This is bold text__

*This is italic text*

_This is italic text_

~~Strikethrough~~

+ Create a list by starting a line with \`+\`, \`-\`, or \`*\`
+ Sub-lists are made by indenting 2 spaces:
  - Marker character change forces new list start:
    * Ac tristique libero volutpat at
    + Facilisis in pretium nisl aliquet
    - Nulla volutpat aliquam velit
+ Very easy!

1. Lorem ipsum dolor sit amet
2. Consectetur adipiscing elit
3. Integer molestie lorem at massa
`;

export const Default = defineStory(() => ({
  components: { SaMarkdownOutput },
  setup() {
    return {
      markdownSource,
    };
  },
  template: '<SaMarkdownOutput :source="markdownSource" style="width: 400px" />',
}), {
  screenshotPreparation: waitForText('This is bold text'),
  useRealTime: true,
});

export const Preview = defineStory(() => ({
  components: { SaMarkdownOutput },
  setup() {
    return {
      markdownSource,
    };
  },
  template: '<SaMarkdownOutput :source="markdownSource" preview style="width: 400px" />',
}), {
  screenshotPreparation: waitForText('This is bold text'),
  useRealTime: true,
});
