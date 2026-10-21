package com.graveyard.feature.news.ui.articleDetail

import com.graveyard.core.designsystem.webview.template.RichContentTemplate
import org.jetbrains.compose.resources.ExperimentalResourceApi
import yingdilite.feature.news.generated.resources.Res

internal object NewsDetailTemplate : RichContentTemplate(
    id = "news.detail", revision = 6, baseUrl = "https://mob.iyingdi.com/",
) {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun loadHtml(): String = Res.readBytes("files/news_detail.html").decodeToString()
}
