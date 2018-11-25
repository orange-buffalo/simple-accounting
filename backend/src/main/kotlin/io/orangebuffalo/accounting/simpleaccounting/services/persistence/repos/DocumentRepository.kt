package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Document
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface DocumentRepository : AbstractEntityRepository<Document>, QuerydslPredicateExecutor<Document>