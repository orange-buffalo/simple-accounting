package io.orangebuffalo.simpleaccounting.business.api

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UpdateUserProfileMutationTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(
                userName = "Fry",
                documentsStorage = "google-drive",
                i18nSettings = I18nSettings(
                    locale = "en_AU",
                    language = "en",
                )
            )
            val farnsworth = platformUser(
                userName = "Farnsworth",
                isAdmin = true,
                documentsStorage = "local",
                i18nSettings = I18nSettings(locale = "fr_FR", language = "fr")
            )
            val fryWorkspace = workspace(owner = fry)
            val workspaceToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Test
    fun `should return NOT_AUTHORIZED error for anonymous requests`() {
        client
            .graphqlMutation { updateProfileMutation("new-storage", "uk", "el") }
            .fromAnonymous()
            .executeAndVerifyNotAuthorized(
                path = DgsConstants.MUTATION.UpdateProfile
            )
    }

    @Test
    fun `should return NOT_AUTHORIZED error for workspace token access`() {
        client
            .graphqlMutation { updateProfileMutation("new-storage", "uk", "el") }
            .usingSharedWorkspaceToken(preconditions.workspaceToken.token)
            .executeAndVerifyNotAuthorized(
                path = DgsConstants.MUTATION.UpdateProfile
            )
    }

    @Test
    fun `should update profile for regular user`() {
        client
            .graphqlMutation { updateProfileMutation("new-storage", "uk", "el") }
            .from(preconditions.fry)
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.UpdateProfile to buildJsonObject {
                    put("userName", "Fry")
                    put("documentsStorage", "new-storage")
                    put("i18n", buildJsonObject {
                        put("locale", "el")
                        put("language", "uk")
                    })
                }
            )

        aggregateTemplate.findAll<PlatformUser>()
            .filter { it.id == preconditions.fry.id }
            .shouldBeSingle()
            .shouldBeEntityWithFields(
                PlatformUser(
                    documentsStorage = "new-storage",
                    i18nSettings = I18nSettings(locale = "el", language = "uk"),
                    activated = preconditions.fry.activated,
                    userName = preconditions.fry.userName,
                    passwordHash = preconditions.fry.passwordHash,
                    isAdmin = preconditions.fry.isAdmin,
                )
            )
    }

    @Test
    fun `should update profile for admin user`() {
        client
            .graphqlMutation { updateProfileMutation("new-storage", "uk", "el") }
            .from(preconditions.farnsworth)
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.UpdateProfile to buildJsonObject {
                    put("userName", "Farnsworth")
                    put("documentsStorage", "new-storage")
                    put("i18n", buildJsonObject {
                        put("locale", "el")
                        put("language", "uk")
                    })
                }
            )

        aggregateTemplate.findAll<PlatformUser>()
            .filter { it.id == preconditions.farnsworth.id }
            .shouldBeSingle()
            .shouldBeEntityWithFields(
                PlatformUser(
                    documentsStorage = "new-storage",
                    i18nSettings = I18nSettings(locale = "el", language = "uk"),
                    activated = preconditions.farnsworth.activated,
                    userName = preconditions.farnsworth.userName,
                    passwordHash = preconditions.farnsworth.passwordHash,
                    isAdmin = preconditions.farnsworth.isAdmin,
                )
            )
    }

    @Test
    fun `should clear documents storage setting`() {
        client
            .graphqlMutation { updateProfileMutation(null, "uk", "el") }
            .from(preconditions.fry)
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.UpdateProfile to buildJsonObject {
                    put("userName", "Fry")
                    put("documentsStorage", JsonNull)
                    put("i18n", buildJsonObject {
                        put("locale", "el")
                        put("language", "uk")
                    })
                }
            )

        aggregateTemplate.findAll<PlatformUser>()
            .filter { it.id == preconditions.fry.id }
            .shouldBeSingle()
            .should {
                it.documentsStorage.shouldBeNull()
            }
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when documentsStorage exceeds max length`() {
        client
            .graphqlMutation { updateProfileMutation("a".repeat(256), "uk", "el") }
            .from(preconditions.fry)
            .executeAndVerifyValidationError(
                violationPath = "documentsStorage",
                error = "SizeConstraintViolated",
                message = "size must be between 0 and 255",
                path = DgsConstants.MUTATION.UpdateProfile,
                params = mapOf("min" to "0", "max" to "255")
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when locale exceeds max length`() {
        client
            .graphqlMutation { updateProfileMutation(null, "uk", "a".repeat(37)) }
            .from(preconditions.fry)
            .executeAndVerifyValidationError(
                violationPath = "locale",
                error = "SizeConstraintViolated",
                message = "size must be between 0 and 36",
                path = DgsConstants.MUTATION.UpdateProfile,
                params = mapOf("min" to "0", "max" to "36")
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when language exceeds max length`() {
        client
            .graphqlMutation { updateProfileMutation(null, "a".repeat(37), "el") }
            .from(preconditions.fry)
            .executeAndVerifyValidationError(
                violationPath = "language",
                error = "SizeConstraintViolated",
                message = "size must be between 0 and 36",
                path = DgsConstants.MUTATION.UpdateProfile,
                params = mapOf("min" to "0", "max" to "36")
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when locale is blank`() {
        client
            .graphqlMutation { updateProfileMutation(null, "uk", "  ") }
            .from(preconditions.fry)
            .executeAndVerifyValidationError(
                violationPath = "locale",
                error = "MustNotBeBlank",
                message = "must not be blank",
                path = DgsConstants.MUTATION.UpdateProfile,
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when language is blank`() {
        client
            .graphqlMutation { updateProfileMutation(null, "  ", "el") }
            .from(preconditions.fry)
            .executeAndVerifyValidationError(
                violationPath = "language",
                error = "MustNotBeBlank",
                message = "must not be blank",
                path = DgsConstants.MUTATION.UpdateProfile,
            )
    }

    private fun MutationProjection.updateProfileMutation(
        documentsStorage: String?,
        language: String,
        locale: String,
    ): MutationProjection = updateProfile(
        documentsStorage = documentsStorage,
        language = language,
        locale = locale,
    ) {
        userName
        this.documentsStorage
        i18n {
            this.locale
            this.language
        }
    }
}
