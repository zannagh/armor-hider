class MixinStringBuilder(firstMixin: String) {

    private val mixins = mutableListOf(firstMixin)

    fun addMixin(mixin: String) {
        mixins.add(mixin)
    }

    fun getMixinString(lastMixin: String): String {
        mixins.add(lastMixin)
        val separator = "\",\n    \""
        return mixins.joinToString(separator)
    }
}
