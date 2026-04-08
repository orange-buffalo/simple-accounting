package io.orangebuffalo.simpleaccounting.business.api.generaltaxes

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTaxesService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class EditGeneralTaxMutation(
    private val generalTaxesService: GeneralTaxesService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Updates an existing general tax in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun editGeneralTax(
        @GraphQLDescription("ID of the workspace the general tax belongs to.")
        workspaceId: Long,
        @GraphQLDescription("ID of the general tax to update.")
        id: Long,
        @GraphQLDescription("New title of the general tax.")
        @NotBlank
        @Size(max = 255)
        title: String,
        @GraphQLDescription("New description of the general tax.")
        @Size(max = 255)
        description: String?,
        @GraphQLDescription("New rate of the general tax in basis points (1/100 of a percent).")
        @Min(0)
        @Max(100_00)
        rateInBps: Int,
    ): GeneralTaxGqlDto {
        val tax = generalTaxesService.getTaxByIdAndWorkspace(id, workspaceId)
            ?: throw EntityNotFoundException("GeneralTax $id is not found")

        tax.title = title
        tax.description = description
        tax.rateInBps = rateInBps

        return generalTaxesService.saveTax(tax).toGeneralTaxGqlDto()
    }
}
