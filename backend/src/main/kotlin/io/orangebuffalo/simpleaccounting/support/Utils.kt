package io.orangebuffalo.simpleaccounting.support

import arrow.core.Either

typealias Maybe<A> = Either<Unit, A>
