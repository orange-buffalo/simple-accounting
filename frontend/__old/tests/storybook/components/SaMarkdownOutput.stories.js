import SaMarkdownOutput from '@/components/SaMarkdownOutput';
import { setViewportHeight } from '../utils/stories-utils';

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

export const Default = () => ({
  components: { SaMarkdownOutput },
  data() {
    return {
      markdownSource,
    };
  },
  template: '<SaMarkdownOutput :source="markdownSource" style="width: 400px" />',
});

export const Preview = () => ({
  components: { SaMarkdownOutput },
  data() {
    return {
      markdownSource,
    };
  },
  template: '<SaMarkdownOutput :source="markdownSource" preview style="width: 400px" />',
});
Preview.parameters = {
  storyshots: {
    async setup(page) {
      await setViewportHeight(page, 450);
    },
  },
};
