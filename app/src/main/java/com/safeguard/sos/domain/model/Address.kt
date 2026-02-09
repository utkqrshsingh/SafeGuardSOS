package com.safeguard.sos.domain.model

data class Address(
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val country: String = "India"
)