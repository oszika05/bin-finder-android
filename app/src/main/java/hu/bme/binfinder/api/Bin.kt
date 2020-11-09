package hu.bme.binfinder.api

data class Bin(
    val id: Int,
    val lat: Double,
    val long: Double,
    val type: BinType,
    val isReported: Boolean
)