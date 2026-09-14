package site.elahady.alkaukaba.model

data class DoaApiResponse<T>(
    val data: T
)

data class DoaCategory(
    val id: Int,
    val slug: String,
    val name: String,
    val order: Int,
    val items_count: Int
)

data class DoaCategoryDetail(
    val category: DoaCategoryRef,
    val items: List<DoaItem>
)

data class DoaCategoryRef(
    val id: Int,
    val slug: String,
    val name: String
)

data class DoaItem(
    val id: Int,
    val doa_category_id: Int,
    val title: String,
    val arabic: String,
    val latin: String,
    val translation: String,
    val notes: String?,
    val fawaid: String?,
    val source: String?,
    val order: Int
)
