package io.orangebuffalo.simpleaccounting.infra.graphql

/** Marks a component whose public functions contribute fields to the root GraphQL query type. */
interface Query

/** Marks a component whose public functions contribute fields to the root GraphQL mutation type. */
interface Mutation

/** Marks a component whose public functions contribute fields to the root GraphQL subscription type. */
interface Subscription

/** Collects root GraphQL operation components used to generate the executable schema. */
interface Schema
