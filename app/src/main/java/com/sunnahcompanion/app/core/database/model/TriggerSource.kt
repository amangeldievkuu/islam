package com.sunnahcompanion.app.core.database.model

/** Where a habit log entry came from. Persisted by [name]. */
enum class TriggerSource {
    IN_APP,
    NOTIFICATION,
    GEOFENCE,
    DRIVING,
    BLUETOOTH,
    SCHEDULED,
    TASBIH,
}
