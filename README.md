# Pipeline Keep Running Step Plugin

[中文介绍](README_zh.md)

## Introduction

This plugin provides `keepRunning` step to keep the process (started by Jenkins) running even if the build has finished.

## What Problem This Plugin Resolve

When we start new process use Jenkins, for example tomcat:

```
sh '/usr/local/apache-tomcat/bin/startup.sh'
```

When the build finish, tomcat process will also be killed, which cannot keep running in background.

Obviously, this is not what we want. What we want is for the tomcat process to continue running in the background.

This plugin can do it!


## Usage Example

```groovy
node {
    keepRunning {
        sh '/usr/local/apache-tomcat/bin/startup.sh'
    }
}
```

By using the `keepRunning` step, tomcat process will keep running in background.

The step needs a workspace and a task listener, so it has to be used inside a `node` block —
the same requirement as the `sh` step it wraps.

## Development

Building and verifying the plugin locally:

```
mvn clean verify
```

The build requires JDK 21 or newer (the Jenkins baseline is 2.568.3, which requires Java 21 or 25 at runtime).
`mvn hpi:run` starts a Jenkins instance for manual testing; see
[Plugin Development](https://www.jenkins.io/doc/developer/) for details.

## Releasing

Releases are fully automated through Jenkins continuous delivery (JEP-229) and are
defined in [`.github/workflows/cd.yaml`](.github/workflows/cd.yaml):

- no maintainer-chosen version number and no `mvn release:prepare` / `release:perform`,
- merging to the default branch lets ci.jenkins.io build the plugin, after which the
  release workflow deploys the artifact to Artifactory and drafts the release notes
  from the pull request titles,
- release versions are derived from the Git history (`<count>.v<commit>`),
- no local `~/.m2/settings.xml` or personal Artifactory credentials are needed.

See [Performing a Plugin Release](https://www.jenkins.io/doc/developer/publishing/releasing/)
for the general documentation.