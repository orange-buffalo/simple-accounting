package io.orangebuffalo.simpleaccounting.business.api.profile

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class UpdateUserProfileMutationTest(
    @Autowired private val client: ApiTestClient,
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

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
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
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            sizeConstraintTestCases("documentsStorage", maxLength = 255) { value ->
                updateProfileMutation(value, "uk", "el")
            },
            mustNotBeBlankTestCases("locale") { value ->
                updateProfileMutation(null, "uk", value)
            },
            sizeConstraintTestCases("locale", maxLength = 36) { value ->
                updateProfileMutation(null, "uk", value)
            },
            mustNotBeBlankTestCases("language") { value ->
                updateProfileMutation(null, value, "el")
            },
            sizeConstraintTestCases("language", maxLength = 36) { value ->
                updateProfileMutation(null, value, "el")
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.UpdateProfile)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
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
