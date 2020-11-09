package hu.bme.binfinder.api

data class CreateBinDto(
    val lat: Double,
    val long: Double,
    val typeId: Int
) {
}