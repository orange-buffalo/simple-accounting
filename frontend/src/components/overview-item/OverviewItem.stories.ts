// noinspection JSUnusedGlobalSymbols

import { allOf, openOverviewPanelDetailsAndDisableAnimations, waitForText } from '@/__storybook__/screenshots';
import OverviewItem from '@/components/overview-item/OverviewItem.vue';
import OverviewItemPrimaryAttribute from '@/components/overview-item/OverviewItemPrimaryAttribute.vue';
import OverviewItemAttributePreviewIcon from '@/components/overview-item/OverviewItemAttributePreviewIcon.vue';
import OverviewItemAmountPanel from '@/components/overview-item/OverviewItemAmountPanel.vue';
import OverviewItemDetailsSectionActions from '@/components/overview-item/OverviewItemDetailsSectionActions.vue';
import OverviewItemDetailsSection from '@/components/overview-item/OverviewItemDetailsSection.vue';
import OverviewItemDetailsSectionAttribute from '@/components/overview-item/OverviewItemDetailsSectionAttribute.vue';
import SaStatusLabel from '@/components/SaStatusLabel.vue';
import SaActionLink from '@/components/SaActionLink.vue';
import { defineStory } from '@/__storybook__/sa-storybook';

export default {
  title: 'Components/OverviewItem',
};

export const NoDetails = defineStory(() => ({
  components: {
    OverviewItem,
    SaStatusLabel,
    OverviewItemPrimaryAttribute,
    OverviewItemAttributePreviewIcon,
    OverviewItemAmountPanel,
  },
  template: `
    <OverviewItem title="Entity Name">
    <template #primary-attributes>
      <OverviewItemPrimaryAttribute
        tooltip="Customer"
        icon="customer"
      >
        Customer Name
      </OverviewItemPrimaryAttribute>

      <OverviewItemPrimaryAttribute
        tooltip="Date"
        icon="calendar"
      >
        2017-09-28
      </OverviewItemPrimaryAttribute>
    </template>

    <template #attributes-preview>
      <OverviewItemAttributePreviewIcon
        icon="notes"
        tooltip="Notes"
      />

      <OverviewItemAttributePreviewIcon
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
      <OverviewItemAmountPanel
        currency="EUR"
        :amount="749327"
      />
    </template>
    </OverviewItem>
  `,
}), {
  screenshotPreparation: waitForText('Entity Name'),
  asPage: true,
});

export const WithDetails = defineStory(() => ({
  components: {
    OverviewItem,
    SaStatusLabel,
    OverviewItemPrimaryAttribute,
    OverviewItemAttributePreviewIcon,
    OverviewItemAmountPanel,
    OverviewItemDetailsSectionActions,
    SaActionLink,
    OverviewItemDetailsSection,
    OverviewItemDetailsSectionAttribute,
  },
  template: `
    <OverviewItem title="Entity Name">
    <template #primary-attributes>
      <OverviewItemPrimaryAttribute
        tooltip="Customer"
        icon="customer"
      >
        Customer Name
      </OverviewItemPrimaryAttribute>
    </template>

    <template #attributes-preview>
      <OverviewItemAttributePreviewIcon
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
      <OverviewItemAmountPanel
        currency="USD"
        :amount="939423"
      />
    </template>

    <template #details>
      <OverviewItemDetailsSectionActions>
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
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection
        title="Section"
      >
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            label="Attribute 1"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            Attribute 1 Value
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Attribute 2"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            Attribute 2 Value
          </OverviewItemDetailsSectionAttribute>
        </div>
      </OverviewItemDetailsSection>
    </template>
    </OverviewItem>
  `,
}), {
  screenshotPreparation: allOf(
    openOverviewPanelDetailsAndDisableAnimations(),
    waitForText('Attribute 1 Value'),
  ),
  asPage: true,
});
