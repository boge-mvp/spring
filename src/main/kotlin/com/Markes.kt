package com

import org.slf4j.Marker
import org.slf4j.MarkerFactory
import kotlin.reflect.KProperty


object MarkerHttp {
    val HTTP by MarkerDelegate()
}

class MarkerDelegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Marker {
        return MarkerFactory.getMarker(property.name)
    }
}
