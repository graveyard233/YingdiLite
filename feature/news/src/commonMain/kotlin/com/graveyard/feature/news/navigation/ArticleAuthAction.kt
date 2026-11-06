package com.graveyard.feature.news.navigation

import com.graveyard.core.data.repository.ArticleVote
import com.graveyard.core.model.news.ArticleCommentVote
import com.graveyard.core.designsystem.navigation.auth.AuthContinuation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Desired state, rather than a click to replay against the newly signed-in user's state. */
@Serializable
sealed interface ArticleAuthAction : AuthContinuation {
    val postId: Long

    @Serializable
    @SerialName("article.setVote")
    data class SetVote(override val postId: Long, val vote: ArticleVote) : ArticleAuthAction

    @Serializable
    @SerialName("article.setCollected")
    data class SetCollected(override val postId: Long, val collected: Boolean) : ArticleAuthAction

    @Serializable
    @SerialName("article.setCommentVote")
    data class SetCommentVote(
        override val postId: Long,
        val commentId: Long,
        val vote: ArticleCommentVote,
    ) : ArticleAuthAction
}
