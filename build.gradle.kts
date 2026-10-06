import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "org.sakos.camera"
    version = "0.0.0-local"
}

subprojects {
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    plugins.withId("com.android.library") {
        apply(plugin = "maven-publish")
        val prepareNotices = tasks.register<Copy>("prepareArtifactNotices") {
            into(layout.buildDirectory.dir("generated/artifact-notices/META-INF/sakos/${project.name}"))
            from(rootProject.file("LICENSE"))
            from(rootProject.file("third_party"))
        }
        tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(prepareNotices) }
        extensions.configure<LibraryExtension> {
            sourceSets.getByName("main").resources.srcDir(layout.buildDirectory.dir("generated/artifact-notices"))
            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }
        afterEvaluate {
            tasks.withType<org.gradle.api.tasks.bundling.Jar>().matching { it.name == "sourceReleaseJar" }.configureEach {
                dependsOn(prepareNotices)
                from(layout.buildDirectory.dir("generated/artifact-notices"))
            }
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
                                if (project.name == "safety-opennsfw2") {
                                    license {
                                        name.set("MIT License (OpenNSFW2)")
                                        url.set("https://github.com/bhky/opennsfw2/blob/19530b8f08aac12479a901fe18763c0392c8bd8c/LICENSE")
                                    }
                                    license {
                                        name.set("BSD 2-Clause License (Yahoo Open NSFW model)")
                                        url.set("https://github.com/yahoo/open_nsfw/blob/a4e13931465f4380742545932657eeea0a10aa48/LICENSE.md")
                                    }
                                    license {
                                        name.set("BSD 2-Clause License (TensorFlow Open NSFW conversion)")
                                        url.set("https://github.com/mdietrichstein/tensorflow-open_nsfw/blob/ead9f4d1748e8bc80ab14bf0a36f696a5fe4109d/LICENSE")
                                    }
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
