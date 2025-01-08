# Pipeline Keep Running Step Plugin

[中文介绍](README_zh.md)

## Introduction

This plugin provides `keepRunning` step to keep the process (started by Jenkins) running even if the build has finished.

## What Problem This Plugin Resolve

When we start new process use Jenkins, for example tomcat:

```
sh '/usr/local/apache-tomact/bin/startup.sh
```

When the build finish, tomcat process will also be killed, which cannot keep running in background.

Obviously, this is not what we want. What we want is for the tomcat process to continue running in the background.

This plugin can do it!


## Usage Example

```
keepRunning {
    sh '/usr/local/apache-tomact/bin/startup.sh
}
```

By ues `keepRunning` step, tomcat process will keep running in background.