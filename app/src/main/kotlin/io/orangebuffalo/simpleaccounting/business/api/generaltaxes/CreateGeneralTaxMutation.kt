package io.orangebuffalo.simpleaccounting.business.api.generaltaxes

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTaxesService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CreateGeneralTaxMutation(
    private val generalTaxesService: GeneralTaxesService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Creates a new general tax in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun createGeneralTax(
        @GraphQLDescription("ID of the workspace to create the general tax in.")
        workspaceId: Long,
        @GraphQLDescription("Title of the general tax.")
        @NotBlank
        @Size(max = 255)
        title: String,
        @GraphQLDescription("Description of the general tax.")
        @Size(max = 255)
        description: String?,
        @GraphQLDescription("Rate of the general tax in basis points (1/100 of a percent).")
        @Min(0)
        @Max(100_00)
        rateInBps: Int,
    ): GeneralTaxGqlDto {
        val tax = generalTaxesService.saveTax(
            GeneralTax(
                title = title,
                description = description,
                rateInBps = rateInBps,
                workspaceId = workspaceId,
            )
        )
        return tax.toGeneralTaxGqlDto()
    }
}
