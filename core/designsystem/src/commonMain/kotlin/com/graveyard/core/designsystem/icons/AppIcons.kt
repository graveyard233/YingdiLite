package com.graveyard.core.designsystem.icons

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 应用内手写图标的登记表：每个条目就是一张图标。
 *
 * 目前底部导航栏的 4 个目的地各自有「线性 + 填充」两张图标，因此它们分别登记为独立条目；
 * 只有单一版本的图标直接补一条即可，本类不涉及选中语义。
 * 新增图标文件后在这里补一条，并在调用处直接交给 Material 3 的 `Icon` 渲染。
 */
enum class AppIcon(val imageVector: ImageVector) {
    News(news),
    NewsFill(newsFill),
    Community(community),
    CommunityFill(communityFill),
    Cards(cards),
    CardsFill(cardsFill),
    AccountCircle(accountCircle),
    AccountCircleFill(accountCircleFill),
}
