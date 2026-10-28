package com.graveyard.yingdilite.navigation

// 应用导航层共享的元数据键，供条目注册、场景策略和场景装饰器使用。
// 字符串保持稳定，确保写入方与读取方使用同一份约定。

/** Entry 对应的实际路由。 */
internal const val APP_ROUTE_KEY = "com.graveyard.yingdilite.route"

/** Entry 所属的顶层 Tab，用于限定各功能场景可组合的条目范围。 */
internal const val APP_TOP_LEVEL_KEY = "com.graveyard.yingdilite.topLevel"

/** 场景对外提供的 [AppScenePresentation]。 */
internal const val APP_PRESENTATION_KEY = "com.graveyard.yingdilite.presentation"

/** 单页场景是否显示应用导航；多 pane 场景通过 [APP_PRESENTATION_KEY] 显式声明。 */
internal const val APP_NAVIGATION_SCENE_KEY = "com.graveyard.yingdilite.navigation.hasNavigation"
