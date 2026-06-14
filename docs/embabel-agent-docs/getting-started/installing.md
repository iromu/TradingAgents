---
name: embabel-installing
description: Installation guide for Embabel Agent framework
source: https://github.com/embabel/embabel-agent/blob/main/embabel-agent-docs/src/main/asciidoc/getting-started/installing/page.adoc
---

[[getting-started.installing]]
=== Getting the Binaries

The easiest way to get started with Embabel Agent is to add the Spring Boot starter dependency to your project.
Embabel release binaries are published to Maven Central.

==== Build Configuration

Add the appropriate Embabel Agent Spring Boot starter to your build file depending on your choice of application type:

===== Shell Starter

Starts the application in console mode with an interactive shell powered by Embabel.

[tabs]
====
Maven (pom.xml)::
+
[source,xml]
----
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter-shell</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
----

Gradle Kotlin DSL (build.gradle.kts)::
+
[source,kotlin]
----
dependencies {
    implementation("com.embabel.agent:embabel-agent-starter-shell:${embabel-agent.version}")
}
----

Gradle Groovy DSL (build.gradle)::
+
[source,groovy]
----
dependencies {
    implementation 'com.embabel.agent:embabel-agent-starter-shell:${embabel-agent.version}'
}
----
====

*Features:*

* Interactive command-line interface
* Agent discovery and registration
* Human-in-the-loop capabilities
* Progress tracking and logging
* Development-friendly error handling

===== MCP Server Starter

Starts the application with HTTP listener where agents are autodiscovered and registered as MCP servers, available for integration via SSE, Streamable-HTTP or Stateless Streamable-HTTP protocols.

[tabs]
====
Maven (pom.xml)::
+
[source,xml]
----
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter-mcpserver</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
----
====

*Features:*

* MCP protocol server implementation
* Tool registration and discovery
* JSON-RPC communication via SSE, Streamable-HTTP or Stateless Streamable-HTTP
* Integration with MCP-compatible clients
* Security and sandboxing

===== Basic Agent Platform Starter

Initializes Embabel Agent Platform in the Spring Container.
Platform beans are available via Spring Dependency Injection mechanism.

[tabs]
====
Maven (pom.xml)::
+
[source,xml]
----
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
----
====

*Features:*

* Application decides on startup mode (console, web application, etc)
* Agent discovery and registration
* Agent Platform beans available via Dependency Injection mechanism
* Progress tracking and logging
* Development-friendly error handling

===== Embabel Snapshots

If you want to use Embabel snapshots, you'll need to add the Embabel repository to your build.

[tabs]
====
Maven (pom.xml)::
+
[source,xml]
----
<repositories>
    <repository>
        <id>embabel-releases</id>
        <url>https://repo.embabel.com/artifactory/libs-release</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
    <repository>
        <id>embabel-snapshots</id>
        <url>https://repo.embabel.com/artifactory/libs-snapshot</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
----
====
