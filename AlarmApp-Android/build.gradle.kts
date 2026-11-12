// 루트 빌드: 플러그인 버전만 선언하고 모듈에서 적용한다 (Spring의 parent POM pluginManagement 같은 역할)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
