androidApplication {
    namespace = "org.example.app"

    dependencies {
        implementation("org.apache.commons:commons-text:1.11.0")
        implementation("androidx.gridlayout:gridlayout:1.0.0")
        implementation(project(":utilities"))
    }
}
