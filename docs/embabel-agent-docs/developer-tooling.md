Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.34. Developer Tooling

### 4.35. IntelliJ IDEA Plugin
The Embabel Agent IntelliJ IDEA plugin provides IDE-level support for developing agents with the Embabel framework.
It integrates directly with the IntelliJ platform to give you a cleaner, warning-free development experience.
#### 4.35.1. What It Does
When you annotate methods with Embabel’s core annotations — `@Action`, `@Condition`, or `@Cost` — IntelliJ has no way of knowing that the framework will invoke those methods reflectively at runtime.
Without the plugin, the IDE flags these methods as **unused**, producing false "never used" warnings throughout your agent code.The plugin registers an `ImplicitUsageProvider` with the IntelliJ platform that tells the IDE:
Any method annotated with `@Action`, `@Condition`, or `@Cost` is implicitly used by the Embabel Agent framework — do not warn.
This means you can write your agents cleanly without suppressing legitimate IDE inspections or littering your code with `@SuppressWarnings`.Without the pluginWith the plugin
#### 4.35.2. Installation
The plugin is published to the JetBrains Marketplace (plugin ID: `31142`).
##### Via the IDE (Recommended)

1.Open IntelliJ IDEA.
1.Go to **Settings** → **Plugins** → **Marketplace** tab.
1.Search for **Embabel Agent**.
1.Click **Install**, then restart the IDE when prompted.
##### Via the Marketplace Website

1.Visit plugins.jetbrains.com/plugin/31142-embabel-agent.
1.Click **Get**, then follow the browser prompt to open IntelliJ IDEA and install.
#### 4.35.3. Compatibility

| Requirement | Value || Minimum IntelliJ IDEA version | 2023.3 (build `233`) || Maximum IntelliJ IDEA version | No upper cap — compatible with all future releases || JVM | 21+ || Plugin ID | `com.embabel.agent.intellij-plugin` |

| | The plugin targets IntelliJ IDEA (both Community and Ultimate editions).
As of IDEA 2025.3, the Community and Ultimate editions were merged into a single unified distribution. |

#### 4.35.4. Source & Contributing
The plugin source is maintained in its own repository:
github.com/embabel/embabel-agent-intellijContributions are welcome.
If you use additional Embabel annotations that should also be treated as implicitly used, please open an issue or pull request against that repository.