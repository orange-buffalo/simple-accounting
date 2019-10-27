<template>
  <div class="">
    <div
      class="row overview-item__panel"
      :class="{'row overview-item__panel_with-details': detailsVisible}"
    >
      <div class="col col-xs-6 col-lg-8">
        <span class="overview-item__title">{{ title }}</span>
        <br>

        <slot name="primary-attributes" />

        <span class="overview-item__attributes-preview">
          <slot name="attributes-preview" />
        </span>
      </div>

      <div class="col col-xs-3 col-lg-2 overview-item__middle-column">
        <slot name="middle-column" />
      </div>

      <div class="col col-xs-3 col-lg-2 overview-item__last-column">
        <slot name="last-column" />
      </div>

      <ElButton
        v-if="detailsAvailable"
        circle
        icon="el-icon-arrow-down"
        class="overview-item__details-trigger"
        :class="{ 'overview-item__details-trigger_open' : detailsVisible }"
        @click="toggleDetailsVisibility"
      />
    </div>

    <div
      v-if="detailsVisible"
      class="overview-item__details"
    >
      <slot name="details" />
    </div>
  </div>
</template>

<script>
import '@/components/icons/empty-box';

export default {
  name: 'OverviewItem',

  props: {
    title: {
      type: String,
      required: true,
    },
  },

  data() {
    return {
      detailsAvailable: false,
      detailsVisible: false,
    };
  },

  mounted() {
    this.detailsAvailable = this.$slots.details !== undefined;
  },

  methods: {
    toggleDetailsVisibility() {
      this.detailsVisible = !this.detailsVisible;
      this.$emit(this.detailsVisible ? 'details-shown' : 'details-closed');
    },
  },
};
</script>

<style lang="scss">
  /*todo #73: common component refers to app styles - redesign dependencies  */
  @import "@/styles/vars.scss";

  $trigger-button-size: 30px;

  .overview-item {
    &__panel {
      padding: 20px;
      border: 1px solid $secondary-grey;
      background-color: $white;
      border-radius: 2px;
      position: relative;
      transition: all 250ms;
      margin-bottom: $trigger-button-size;

      .col {
        padding: 0;
      }

      &_with-details {
        transform: scale(1.01);
        box-shadow: 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04)
      }
    }

    &__title {
      font-weight: bold;
      font-size: 110%;
      padding-right: 10px;
      display: inline-block;
      margin-bottom: 5px;
    }

    &__attributes-preview {
      display: inline-flex;
      align-items: center;
    }

    &__middle-column {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    &__last-column {
      display: flex;
      align-items: center;
      justify-content: flex-end;
    }

    &__details-trigger {
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%) translateY(50%);
      background: $white;
      transition: all 250ms;

      &_open {
        transform: translateX(-50%) translateY(50%) rotate(180deg);
        box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.12)
      }
    }

    &__details {
      border: 1px solid $secondary-grey;
      background-color: $white;
      padding: 20px 20px 0 20px;
      margin-top: -$trigger-button-size;
    }
  }
</style>
