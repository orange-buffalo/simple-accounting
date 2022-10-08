// noinspection JSUnusedGlobalSymbols

import {
  allOf,
  openOverviewPanelDetails,
  waitForText,
} from '@/__storybook__/screenshots';
import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
import SaOverviewItemPrimaryAttribute from '@/components/overview-item/SaOverviewItemPrimaryAttribute.vue';
import SaOverviewItemAttributePreviewIcon from '@/components/overview-item/SaOverviewItemAttributePreviewIcon.vue';
import SaOverviewItemAmountPanel from '@/components/overview-item/SaOverviewItemAmountPanel.vue';
import SaOverviewItemDetailsSectionActions from '@/components/overview-item/SaOverviewItemDetailsSectionActions.vue';
import SaOverviewItemDetailsSection from '@/components/overview-item/SaOverviewItemDetailsSection.vue';
import SaOverviewItemDetailsSectionAttribute
  from '@/components/overview-item/SaOverviewItemDetailsSectionAttribute.vue';
import SaStatusLabel from '@/components/SaStatusLabel.vue';
import SaActionLink from '@/components/SaActionLink.vue';
import { defineStory } from '@/__storybook__/sa-storybook';

export default {
  title: 'Components/Basic/SaOverviewItem',
};

export const NoDetails = defineStory(() => ({
  components: {
    SaOverviewItem,
    SaStatusLabel,
    SaOverviewItemPrimaryAttribute,
    SaOverviewItemAttributePreviewIcon,
    SaOverviewItemAmountPanel,
  },
  template: `
    <SaOverviewItem title="Entity Name">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute
        tooltip="Customer"
        icon="customer"
      >
        Customer Name
      </SaOverviewItemPrimaryAttribute>

      <SaOverviewItemPrimaryAttribute
        tooltip="Date"
        icon="calendar"
      >
        2017-09-28
      </SaOverviewItemPrimaryAttribute>
    </template>

    <template #attributes-preview>
      <SaOverviewItemAttributePreviewIcon
        icon="notes"
        tooltip="Notes"
      />

      <SaOverviewItemAttributePreviewIcon
        icon="attachment"
      />
    </template>

    <template #middle-column>
      <SaStatusLabel
        status="pending"
      >
        Waiting for action
      </SaStatusLabel>
    </template>

    <template #last-column>
      <SaOverviewItemAmountPanel
        currency="EUR"
        :amount="749327"
      />
    </template>
    </SaOverviewItem>
  `,
}), {
  screenshotPreparation: waitForText('Entity Name'),
  asPage: true,
});

export const WithDetails = defineStory(() => ({
  components: {
    SaOverviewItem,
    SaStatusLabel,
    SaOverviewItemPrimaryAttribute,
    SaOverviewItemAttributePreviewIcon,
    SaOverviewItemAmountPanel,
    SaOverviewItemDetailsSectionActions,
    SaActionLink,
    SaOverviewItemDetailsSection,
    SaOverviewItemDetailsSectionAttribute,
  },
  template: `
    <SaOverviewItem title="Entity Name">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute
        tooltip="Customer"
        icon="customer"
      >
        Customer Name
      </SaOverviewItemPrimaryAttribute>
    </template>

    <template #attributes-preview>
      <SaOverviewItemAttributePreviewIcon
        icon="notes"
        tooltip="Notes"
      />
    </template>

    <template #middle-column>
      <SaStatusLabel
        status="pending"
      >
        Waiting for action
      </SaStatusLabel>
    </template>

    <template #last-column>
      <SaOverviewItemAmountPanel
        currency="USD"
        :amount="939423"
      />
    </template>

    <template #details>
      <SaOverviewItemDetailsSectionActions>
        <SaActionLink
          icon="pencil-solid"
        >
          First Action
        </SaActionLink>

        <SaActionLink
          icon="send-solid"
        >
          Second Action
        </SaActionLink>
      </SaOverviewItemDetailsSectionActions>

      <SaOverviewItemDetailsSection
        title="Section"
      >
        <div class="row">
          <SaOverviewItemDetailsSectionAttribute
            label="Attribute 1"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            Attribute 1 Value
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            label="Attribute 2"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            Attribute 2 Value
          </SaOverviewItemDetailsSectionAttribute>
        </div>
      </SaOverviewItemDetailsSection>
    </template>
    </SaOverviewItem>
  `,
}), {
  screenshotPreparation: allOf(
    openOverviewPanelDetails(),
    waitForText('Attribute 1 Value'),
  ),
  asPage: true,
});
