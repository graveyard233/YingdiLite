package com.graveyard.feature.news.model

data class NewsCategory(
    val id: Int,
    val label: String,
)

internal fun validateNewsCategories(categories: List<NewsCategory>) {
    require(categories.isNotEmpty()) { "News requires at least one category." }
    require(categories.map { it.id }.distinct().size == categories.size) {
        "News category IDs must be unique."
    }
}
