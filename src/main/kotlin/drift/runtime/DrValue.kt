package drift.runtime

interface DrValue {
    fun asString() : String
    fun type() : DrType
}
