package io.orangebuffalo.simpleaccounting.business.oauthproviders

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface OAuthAuthenticationRequestsRepository : AbstractEntityRepository<OAuthAuthenticationRequest> {
    fun findByState(state: String): OAuthAuthenticationRequest?

    /**
     * Consumes the request, returning the number of removed rows. Concurrent callbacks for the same
     * state therefore see a single successful consumption.
     */
    @Modifying
    @Query("delete from OAUTH_AUTHENTICATION_REQUEST where STATE = :state")
    fun deleteByState(@Param("state") state: String): Int

    @Modifying
    @Query("delete from OAUTH_AUTHENTICATION_REQUEST where EXPIRES_AT <= :expirationTime")
    fun deleteExpired(@Param("expirationTime") expirationTime: Instant)

    /**
     * A browser only ever needs a single pending flow, so starting a new one discards its own
     * previous attempt. Requests of other browsers are deliberately left alone: they are not
     * reachable by a caller that does not hold their binding secret.
     */
    @Modifying
    @Query("delete from OAUTH_AUTHENTICATION_REQUEST where BROWSER_BINDING = :browserBinding")
    fun deleteByBrowserBinding(@Param("browserBinding") browserBinding: String)
}
