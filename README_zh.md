# Pipeline Keep Running Step Plugin

## 介绍

本插件提供了一个 `keepRunning` 步骤，可以保持通过Jenkins启动的进程处于运行状态，即使构建已结束。 

## 本插件解决了什么问题

当我们使用Jenkins启动新进程时，比如启动tomcat：

```
sh '/usr/local/apache-tomcat/bin/startup.sh'
```

当构建结束时，启动的tomcat进程也会被中止，无法在后台保持运行状态。

很明显，这不是我们想要的。我们希望的是tomcat进程能在后台持续运行。

本插件就能做到这一点！

## 使用示例

```
node {
    keepRunning {
        sh '/usr/local/apache-tomcat/bin/startup.sh'
    }
}
```

通过使用`keepRunning`步骤包裹命令, 命令启动的tomcat进程不会被中止，可在后台保持运行状态。

该步骤需要工作空间（workspace）与任务监听器上下文，因此必须放在 `node` 块内使用
（与其包裹的 `sh` 步骤要求一致）。

## 开发

本地构建与验证：

```
mvn clean verify
```

构建需要 JDK 21 或更高版本（Jenkins 基线为 2.568.3，运行时要求 Java 21 或 25）。
`mvn hpi:run` 可启动一个 Jenkins 实例用于手动测试，详见
[插件开发文档](https://www.jenkins.io/doc/developer/)。

## 发版

发版已全自动化，走 Jenkins 持续交付（JEP-229），定义在
[`.github/workflows/cd.yaml`](.github/workflows/cd.yaml)：

- 不再需要维护者手写版本号，也不需要 `mvn release:prepare` / `release:perform`；
- 合入默认分支后由 ci.jenkins.io 构建，随后发版工作流把构件部署到 Artifactory，
  并根据 PR 标题自动生成 release notes；
- 发布号由 Git 历史推导（形如 `<count>.v<commit>`）；
- 不需要本机 `~/.m2/settings.xml`，也不需要个人 Artifactory 凭据。