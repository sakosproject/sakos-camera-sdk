import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

allprojects {
    group = "org.sakos.camera"
    version = "0.0.0-local"
}

subprojects {
    plugins.withId("com.android.library") {
        apply(plugin = "maven-publish")
        extensions.configure<LibraryExtension> {
            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }
        afterEvaluate {
            extensions.configure<PublishingExtension> {
                publications {
                    register<MavenPublication>("release") {
                        from(components["release"])
                        artifactId = project.name
                        pom {
                            name.set("SakOS Camera ${project.name}")
                            description.set("Provisional local verification artifact for SakOS Camera SDK.")
                            url.set("https://sakosproject.org/camera/")
                            licenses {
                                license {
                                    name.set("Apache License, Version 2.0")
                                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                }
                            }
                            developers {
                                developer {
                                    id.set("sakosproject")
                                    name.set("SakOS Project")
                                }
                            }
                            scm {
                                url.set("https://github.com/sakosproject/sakos-camera-sdk")
                            }
                        }
                    }
                }
                repositories {
                    maven {
                        name = "sakosLocal"
                        url = rootProject.layout.buildDirectory.dir("local-maven").get().asFile.toURI()
                    }
                }
            }
        }
    }
}
