import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "2.5.5" apply false
    id("io.spring.dependency-management") version "1.0.11.RELEASE" apply false
    kotlin("jvm") version "1.5.31" apply false
    kotlin("plugin.spring") version "1.5.31" apply false

    id("de.undercouch.download") version "4.1.2"
    java
}

group = "es.unizar"
version = "0.0.1-SNAPSHOT"

val jaxbVersion by extra { "2.1.7" }

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    repositories {
        mavenCentral()
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
        }
    }
    tasks.withType<Test> {
        useJUnitPlatform()
    }
    dependencies {
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }
    tasks.withType<BootJar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

project(":server") {

    val generatedJavaDirs = "$buildDir/generated-sources/xjc"
    val generatedClassesDirs = "$buildDir/classes/xjc"
    val schema = "$projectDir/src/main/resources/ws/translator.xsd"
    val jaxb by configurations.creating

    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.boot:spring-boot-starter-web-services")
        "implementation"("wsdl4j:wsdl4j")
        "runtimeOnly"("org.glassfish.jaxb:jaxb-runtime")
        jaxb("com.sun.xml.bind:jaxb-xjc:2.1.7")
        "implementation"(files(layout.buildDirectory.dir("classes/xjc")) {
            builtBy("genJaxb")
        })
    }

    task("genJaxb") {
        val sourcedestdir = file(generatedJavaDirs)
        val classesdestdir = file(generatedClassesDirs)

        doLast {
            sourcedestdir.mkdirs()
            classesdestdir.mkdirs()
            ant.withGroovyBuilder {
                "taskdef"(
                    "name" to "xjc",
                    "classname" to "com.sun.tools.xjc.XJCTask",
                    "classpath" to jaxb.asPath
                )

                "xjc"(
                    "destdir" to sourcedestdir,
                    "schema" to schema,
                    "package" to "client.translator"
                )

                "javac"(
                    "destdir" to generatedClassesDirs,
                    "source" to 1.8,
                    "target" to 1.8,
                    "debug" to true,
                    "debugLevel" to "lines,vars,source",
                    "classpath" to jaxb.asPath
                ) {
                    "src"("path" to sourcedestdir)
                    "include"("name" to "**/*.java")
                    "include"("name" to "*.java")
                }
            }
        }
    }
    tasks.withType<KotlinCompile> {
        dependsOn("genJaxb")
    }
    java {
        sourceSets {
            getByName("main").java.srcDirs(generatedJavaDirs)
        }
    }
}

project(":client") {

    val generatedJavaDirs = "$buildDir/generated-sources/xjc"
    val generatedClassesDirs = "$buildDir/classes/xjc"
    val generatedResourceDirs = "$buildDir/generated-sources/resources"
    val metaInfDir = "$generatedResourceDirs/META-INF"
    val wsdlDir = "$metaInfDir/wsdl"
    val catalogFile = "$metaInfDir/jax-ws-catalog.xml"
    val translatorWsdlFile = "$wsdlDir/translator.wsdl"
    val translatorResourceWsdl = "wsdl/translator.wsdl"
    val serverWsdlLocation = "http://localhost:8080/ws/translator.wsdl"
    val jaxb by configurations.creating

    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-web-services") {
            exclude("org.springframework.boot", "spring-boot-starter-tomcat")
        }
        "implementation"("org.springframework.ws:spring-ws-core")
        "implementation"("org.glassfish.jaxb:jaxb-runtime")
        jaxb("com.sun.xml.bind:jaxb-xjc:2.1.7")
        "implementation"(files(layout.buildDirectory.dir("classes/xjc")) {
            builtBy("genJaxb")
        })
    }

    /**
     * It is common that the remote wsdl location is hardcoded into the code.
     * As the wsdl location may be not available, we use the JAXWS catalog to map
     * this remote resource to a local copy.
     */
    task("create-catalog") {
        val file = file(catalogFile)
        file.parentFile.mkdirs()
        file.writeText(
            """
            <catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog" prefer="system">
             <system systemId="$serverWsdlLocation" uri="$translatorResourceWsdl"/>
            </catalog>
        """.trimIndent()
        )
    }

    task<Download>("download-wsdl") {
        val file = file(translatorWsdlFile)
        file.parentFile.mkdirs()
        src(serverWsdlLocation)
        dest(file)
        overwrite(true)
    }

    task("genJaxb") {
        dependsOn("download-wsdl", "create-catalog")
        group = BasePlugin.BUILD_GROUP
        val sourcedestdir = file(generatedJavaDirs)
        val classesdestdir = file(generatedClassesDirs)

        doLast {
            sourcedestdir.mkdirs()
            classesdestdir.mkdirs()
            ant.withGroovyBuilder {
                "taskdef"(
                    "name" to "xjc",
                    "classname" to "com.sun.tools.xjc.XJCTask",
                    "classpath" to jaxb.asPath
                )

                "xjc"(
                    "destdir" to sourcedestdir,
                    "schema" to serverWsdlLocation,
                    "package" to "client.translator"
                ) {
                    "arg"("value" to "-wsdl")
                }
                "javac"(
                    "destdir" to generatedClassesDirs,
                    "source" to 1.8,
                    "target" to 1.8,
                    "debug" to true,
                    "debugLevel" to "lines,vars,source",
                    "classpath" to jaxb.asPath
                ) {
                    "src"("path" to sourcedestdir)
                    "include"("name" to "**/*.java")
                    "include"("name" to "*.java")
                }
            }
        }
    }
    tasks.withType<KotlinCompile> {
        dependsOn("genJaxb")
    }
    java {
        sourceSets {
            getByName("main").java.srcDirs(generatedJavaDirs)
            getByName("main").resources.srcDirs(generatedResourceDirs)
        }
    }
}