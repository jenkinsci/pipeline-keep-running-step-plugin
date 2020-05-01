# Pipeline Keep Running Step Plugin

## Introduction

This plugin provides `keepRunning` step to keep the process running even if the build has finished.

## Example

```
keepRunning {
    sh '/usr/local/apache-tomact/bin/startup.sh
}
```