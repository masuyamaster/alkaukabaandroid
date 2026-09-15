package site.elahady.alkaukaba.model

data class MenuItem(
    val iconRes: Int,
    val label: String,
    val iconBackgroundTint: Int,
    val iconTint: Int,
    val activityClass: Class<*>
)
