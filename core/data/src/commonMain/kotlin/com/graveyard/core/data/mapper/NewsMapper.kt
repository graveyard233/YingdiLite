package com.graveyard.core.data.mapper

import com.graveyard.core.data.network.model.BannerItem
import com.graveyard.core.data.network.model.NewsListItem
import com.graveyard.core.model.news.NewsArticle
import com.graveyard.core.model.news.NewsAuthor
import com.graveyard.core.model.news.NewsBanner

internal fun NewsListItem.toNewsArticle(): NewsArticle = NewsArticle(
    id = feed.id,
    author = NewsAuthor(
        id = author.id,
        username = author.username,
        head = author.head,
        credits = author.credits,
        badgeJson = author.badgeJson,
    ),
    title = feed.title,
    content = feed.content,
    type = feed.type,
    style = feed.style,
    imgs = feed.imgs,
    cover = feed.cover,
    url = feed.url,
    tagJson = feed.tagJson,
    likeNum = feed.likeNum,
    replyNum = feed.replyNum,
    deckInfoJson = feed.deckInfoJson,
    voteOptionJson = feed.voteOptionJson,
    videoUrl = feed.videoUrl,
    sourceId = feed.sourceId,
    remunerationCreated = feed.remunerationCreated,
    showTime = feed.showTime,
)

internal fun BannerItem.toNewsBanner(): NewsBanner = NewsBanner(
    adId = adId,
    img = img,
    url = url,
    title = title,
)
