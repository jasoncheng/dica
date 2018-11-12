package cool.mixi.dica.bean

import android.location.Address

data class Geo(
    var type: String?,
    var coordinates: DoubleArray?,
    var address: Address?
)