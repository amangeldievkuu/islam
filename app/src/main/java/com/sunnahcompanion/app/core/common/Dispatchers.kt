package com.sunnahcompanion.app.core.common

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: AppDispatchers)

enum class AppDispatchers { Default, IO }

/** Process-lifetime scope for work that must outlive any screen (seeding, receiver writes). */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
