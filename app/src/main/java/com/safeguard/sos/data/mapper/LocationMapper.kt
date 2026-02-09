
package com.safeguard.sos.data.mapper

import com.safeguard.sos.data.local.entity.LocationEntity
import com.safeguard.sos.domain.model.Location

fun LocationEntity.toDomain(): Location {
    return Location(
        latitude = this.latitude,
        longitude = this.longitude,
        accuracy = this.accuracy,
        altitude = this.altitude,
        address = this.address,
        city = this.city,
        state = this.state,
        timestamp = this.timestamp
    )
}

fun Location.toEntity(): LocationEntity {
    return LocationEntity(
        latitude = this.latitude,
        longitude = this.longitude,
        accuracy = this.accuracy,
        altitude = this.altitude,
        address = this.address,
        city = this.city,
        state = this.state,
        timestamp = this.timestamp
    )
}
