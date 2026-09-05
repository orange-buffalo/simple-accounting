<template>
  <div
    v-if="loading || providerLinks.length > 0"
    class="sa-oauth-providers-section"
  >
    <h2>{{ $t.myProfile.oauthProviders.header() }}</h2>

    <p class="sa-oauth-providers-section__description">
      {{ $t.myProfile.oauthProviders.description() }}
    </p>

    <div
      v-if="loading"
      class="sa-oauth-providers-section__loading"
    >
      <div class="sa-oauth-providers-section__loading-placeholder" />
      <div class="sa-oauth-providers-section__loading-placeholder" />
    </div>

    <template v-else>
      <div
        v-for="providerLink in providerLinks"
        :key="providerLink.providerId"
        class="sa-oauth-providers-section__provider"
        :data-provider-id="providerLink.providerId"
      >
        <div class="sa-oauth-providers-section__provider-header">
          <span class="sa-oauth-providers-section__provider-name">
            {{ providerLink.providerName }}
          </span>

          <SaStatusLabel
            v-if="providerLink.externalId"
            status="success"
          >
            {{ $t.myProfile.oauthProviders.linked(providerLink.externalId) }}
          </SaStatusLabel>
          <SaStatusLabel
            v-else
            status="regular"
          >
            {{ $t.myProfile.oauthProviders.notLinked() }}
          </SaStatusLabel>

          <ElButton
            v-if="providerLink.externalId"
            link
            class="sa-oauth-providers-section__provider-action"
            :disabled="actionInProgress"
            @click="unlinkProvider(providerLink.providerId)"
          >
            {{ $t.myProfile.oauthProviders.unlink() }}
          </ElButton>
          <ElButton
            v-else
            link
            class="sa-oauth-providers-section__provider-action"
            :disabled="actionInProgress"
            @click="linkProvider(providerLink.providerId)"
          >
            {{ $t.myProfile.oauthProviders.link() }}
          </ElButton>
        </div>
      </div>
    </template>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery, useMutation, useQuery } from '@/services/api/use-gql-api.ts';
  import useNotifications from '@/components/notifications/use-notifications.ts';
  import { useConfirmation } from '@/components/confirmation/use-confirmation';

  const emit = defineEmits<{
    (e: 'links-changed', hasLinkedIdentities: boolean): void,
  }>();

  const { showSuccessNotification } = useNotifications();

  const providerLinksQuery = graphql(/* GraphQL */ `
    query myOAuthProviderLinks {
      myOAuthProviderLinks {
        providerId
        providerName
        externalId
      }
    }
  `);

  const [loading, providerLinksData] = useQuery(providerLinksQuery, 'myOAuthProviderLinks');
  const reloadProviderLinks = useLazyQuery(providerLinksQuery, 'myOAuthProviderLinks');

  const providerLinks = computed(() => providerLinksData.value ?? []);
  // guards against double submissions while the browser is still navigating to the provider
  const actionInProgress = ref(false);

  watch([loading, providerLinks], () => {
    if (!loading.value) {
      emit('links-changed', providerLinks.value.some((link) => link.externalId));
    }
  }, { immediate: true });

  const startLinkingMutation = useMutation(graphql(/* GraphQL */ `
    mutation startOAuthIdentityLinking($providerId: String!) {
      startOAuthIdentityLinking(providerId: $providerId) {
        authorizationUrl
      }
    }
  `), 'startOAuthIdentityLinking');

  const unlinkMutation = useMutation(graphql(/* GraphQL */ `
    mutation unlinkOAuthIdentity($providerId: String!) {
      unlinkOAuthIdentity(providerId: $providerId) {
        success
      }
    }
  `), 'unlinkOAuthIdentity');

  const linkProvider = async (providerId: string) => {
    if (actionInProgress.value) return;
    actionInProgress.value = true;
    try {
      const { authorizationUrl } = await startLinkingMutation({ providerId });
      window.location.assign(authorizationUrl);
    } catch (e: unknown) {
      actionInProgress.value = false;
      throw e;
    }
  };

  const linkedProvidersCount = computed(() => providerLinks.value.filter((link) => link.externalId).length);

  const unlinkProvider = async (providerId: string) => {
    if (actionInProgress.value) return;
    // password login only returns once the very last identity is removed
    const message = linkedProvidersCount.value > 1
      ? $t.value.myProfile.oauthProviders.unlinkConfirm.message()
      : $t.value.myProfile.oauthProviders.unlinkConfirm.lastProviderMessage();
    const confirmUnlink = useConfirmation(
      message,
      {
        title: $t.value.myProfile.oauthProviders.unlinkConfirm.title(),
        confirmButtonText: $t.value.myProfile.oauthProviders.unlinkConfirm.yes(),
        cancelButtonText: $t.value.myProfile.oauthProviders.unlinkConfirm.no(),
        type: 'warning',
        customClass: 'sa-oauth-providers-section__unlink-confirmation',
      },
      async () => {
        actionInProgress.value = true;
        try {
          await unlinkMutation({ providerId });
          providerLinksData.value = await reloadProviderLinks({});
          showSuccessNotification($t.value.myProfile.oauthProviders.feedback.unlinked());
        } finally {
          actionInProgress.value = false;
        }
      },
    );
    await confirmUnlink();
  };
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .sa-oauth-providers-section {
    @include white-panel;
    margin-bottom: 30px;

    &__description {
      font-size: 90%;
      color: $secondary-text-color;
      margin: 0 0 16px;
    }

    &__loading {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    &__loading-placeholder {
      height: 40px;
      @include loading-placeholder;
      border-radius: 5px;
    }

    &__provider {
      margin-bottom: 4px;
    }

    &__provider-header {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    &__provider-name {
      font-weight: 600;
      color: $primary-text-color;
      flex-grow: 1;
    }

    &__provider-action {
      padding: 0;
    }
  }
</style>
