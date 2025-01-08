# Pipeline Keep Running Step Plugin

## 介绍

本插件提供了一个 `keepRunning` 步骤，可以保持通过Jenkins启动的进程处于运行状态，即使构建已结束。 

## 本插件解决了什么问题

当我们使用Jenkins启动新进程时，比如启动tomcat：

```
sh '/usr/local/apache-tomact/bin/startup.sh
```

当构建结束时，启动的tomcat进程也会被中止，无法在后台保持运行状态。

很明显，这不是我们想要的。我们希望的是tomcat进程能在后台持续运行。

本插件就能做到这一点！

## 使用示例

```
keepRunning {
    sh '/usr/local/apache-tomact/bin/startup.sh
}
```

通过使用`keepRunning`步骤包裹命令, 命令启动的tomcat进程不会被中止，可在后台保持运行状态。