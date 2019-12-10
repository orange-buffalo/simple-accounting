package io.orangebuffalo.simpleaccounting.services.persistence

import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.event.spi.PreInsertEventListener
import org.hibernate.event.spi.PreUpdateEventListener
import org.hibernate.internal.SessionFactoryImpl
import javax.persistence.EntityManagerFactory

inline fun <reified T> EntityManagerFactory.registerEntitySaveListener(crossinline listener: (T) -> Unit) {
    val sessionFactory = unwrap(SessionFactoryImpl::class.java)
    val registry = sessionFactory.serviceRegistry.getService(EventListenerRegistry::class.java)

    registry.getEventListenerGroup(EventType.PRE_INSERT).appendListener(PreInsertEventListener { event ->
        val entity = event.entity
        if (entity is T) {
            listener(entity)
        }
        false
    })

    registry.getEventListenerGroup(EventType.PRE_UPDATE).appendListener(PreUpdateEventListener { event ->
        val entity = event.entity
        if (entity is T) {
            listener(entity)
        }
        false
    })
}
