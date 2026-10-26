# Upstream state

| Fact | Value | Authority |
|------|-------|-----------|
| Repository | `https://github.com/embabel/dice` | `pom.xml:16`, `<scm>` `:18-22` |
| Reference used | `main` @ `870b9ab`, `pushed_at 2026-09-10` | GitHub API |
| Build | **Maven** (`pom.xml`, `mvnw`, `.mvn/`) - there is **no** `build.gradle` | `pom.xml` |
| Coordinates | `com.embabel.dice:dice-parent:0.2.0-SNAPSHOT` | `pom.xml:10-12` |
| Tags / releases | **none** - `tags` and `releases` both return `[]` | GitHub API |
| CHANGELOG | one section only: `## Unreleased` | `CHANGELOG.md:9` |
| Neighbours | `embabel-agent 1.5.0-SNAPSHOT`, `drivine 0.0.79`, `tuprolog 1.0.4` | `pom.xml` properties |

- **Never write "pin `v0.2.0`".** No tag and no release object exists. Say "build against `main`" or
  depend on the `-SNAPSHOT` version and resolve from Embabel's Artifactory.
- **The README's install block is wrong twice over.** `README.md:2630-2632` says
  `com.embabel:dice:0.1.1-SNAPSHOT`; the poms say groupId `com.embabel.dice` and version
  `0.2.0-SNAPSHOT`. The poms win - do not copy the README snippet.
- Metamodel versioning and the stamping DSL are labelled **EXPERIMENTAL (shape may change before
  1.0)** by upstream. Carry that caveat; do not present the DSL as settled API.
