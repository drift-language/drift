package drift.runtime

class DrFunction(
    val impl: (List<DrValue>) -> DrValue) : DrValue {

    fun invoke(args: List<DrValue>): DrValue = impl(args)

    override fun asString(): String = "function"
}