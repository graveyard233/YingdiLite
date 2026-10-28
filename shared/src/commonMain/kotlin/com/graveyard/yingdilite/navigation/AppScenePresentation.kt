package com.graveyard.yingdilite.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

/** 应用层统一消费的场景状态；各功能的布局模式由各自的场景元数据管理。 */
internal data class AppScenePresentation(
    val showsApplicationNavigation: Boolean,
    val visibleEntryKeys: Set<NavKey>,
    // 多 pane 默认都可交互；弹层可单独收窄这个集合，而无需隐藏下方内容。
    val interactiveEntryKeys: Set<NavKey> = visibleEntryKeys,
)

/** 读取场景显式声明的状态，普通单页场景则从现有元数据推导默认值。 */
internal fun scenePresentation(scene: Scene<NavKey>): AppScenePresentation {
    (scene.metadata[APP_PRESENTATION_KEY] as? AppScenePresentation)?.let { return it }
    val keys = scene.entries.mapNotNull { it.metadata[APP_ROUTE_KEY] as? NavKey }.toSet()
    // 多 pane 场景应自行声明展示状态，避免把某一个条目的导航栏配置套用到整个场景。
    return AppScenePresentation(
        showsApplicationNavigation = scene.entries.size == 1 && scene.metadata[APP_NAVIGATION_SCENE_KEY] == true,
        visibleEntryKeys = keys,
    )
}
