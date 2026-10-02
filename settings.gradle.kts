rootProject.name = "YingdiLite"

pluginManagement {
    repositories {
        maven(url = "https://maven.aliyun.com/repository/public")
        maven(url = "https://maven.aliyun.com/repository/google")
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        mavenCentral()
        google()
        exclusiveContent {
            forRepository {
                mavenCentral()
            }
            filter {
                includeGroupAndSubgroups("org.jetbrains")
                includeGroupAndSubgroups("io.ktor")
                includeGroupAndSubgroups("io.coil-kt")
            }
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
    }
}

include(":androidApp")
include(":shared")

include(":core:designsystem")
include(":core:model")
include(":core:data")
include(":feature:news")
include(":feature:community")
include(":feature:cards")
include(":feature:mine")
