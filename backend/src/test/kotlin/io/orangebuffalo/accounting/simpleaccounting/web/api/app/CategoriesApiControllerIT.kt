package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Fry
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CategoryRepository
import io.orangebuffalo.accounting.simpleaccounting.web.DbHelper
import io.orangebuffalo.accounting.simpleaccounting.web.expectThatJsonBody
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Categories API ")
internal class CategoriesApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val categoryRepository: CategoryRepository,
    @Autowired val dbHelper: DbHelper
) {

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should add a new category to the workspace`(fry: Fry) {
        val categoryId = dbHelper.getNextId()

        client.post()
            .uri("/api/workspaces/${fry.workspace.id}/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "name": "1990s stuff",
                    "description": "Stuff from the best time",
                    "income": false,
                    "expense": true
                }"""
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                isEqualTo(
                    json(
                        """{
                        name: "1990s stuff",
                        id: $categoryId,
                        version: 0,
                        description: "Stuff from the best time",
                        income: false,
                        expense: true
                    }"""
                    )
                )
            }

        val newCategory = categoryRepository.findById(categoryId)
        assertThat(newCategory).isPresent.hasValueSatisfying {
            assertThat(it.workspace).isEqualTo(fry.workspace)
        }
    }

    //todo #66: uncomment and adapt
//    @Test
//    @WithMockUser(roles = ["USER"], username = "Fry")
//    fun `should return 400 if workspace belongs to another user when posting new category`(
//        testData: WorkspacesApiTestData
//    ) {
//        client.post()
//            .uri("/api/workspaces/${testData.fryWorkspace.id}/categories")
//            .contentType(MediaType.APPLICATION_JSON)
//            .syncBody(
//                """{
//                    "name": "fry-to-professor",
//                    "description": null,
//                    "income": false,
//                    "expense": true
//                }"""
//            )
//            .exchange()
//            .expectStatus().isNotFound
//            .expectBody<String>().consumeWith {
//                assertThat(it.responseBody).contains("Workspace ${testData.farnsworthWorkspace.id} is not found")
//            }
//    }
}