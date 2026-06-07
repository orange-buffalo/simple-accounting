package io.orangebuffalo.simpleaccounting.business.common.exceptions

/**
 * Thrown when a user submits changes based on an entity version that is no longer current.
 *
 * This protects edit operations from silently overwriting changes saved after the user loaded
 * the entity for editing. GraphQL maps this exception to a generic error that asks the user
 * to reload the data and submit their changes again.
 */
class SubmittedOutdatedStateException(message: String) : RuntimeException(message)
