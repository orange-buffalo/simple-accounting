package io.orangebuffalo.simpleaccounting.infra

import arrow.core.Either

typealias Maybe<A> = Either<Unit, A>
