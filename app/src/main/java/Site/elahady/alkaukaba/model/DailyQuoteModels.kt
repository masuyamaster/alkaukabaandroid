package site.elahady.alkaukaba.model

data class DailyQuoteResponse(
    val data: DailyQuote
)

data class DailyQuote(
    val type: String,
    val arabic: String,
    val latin: String?,
    val translation: String,
    val source: String
)
