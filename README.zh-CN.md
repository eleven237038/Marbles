# Marbles

**[English](./README.md) | 简体中文**

Java 六边形弹珠消除游戏(课程 Games Programming 作业 Ass-2),含 Sans Boss 关卡与特殊弹珠机制。

## 目录

- [背景](#背景)
- [运行](#运行)
- [玩法](#玩法)
- [计分](#计分)
- [特殊弹珠](#特殊弹珠)
- [项目结构](#项目结构)
- [License](#license)

## 背景

课程 Games Programming 作业(Ass-2),基于六边形网格的弹珠消除玩法,并在第 4 关设计了带 Sans Boss 的进阶机制。开发设计备忘见 `备注.txt`。

## 运行

- 用 IntelliJ IDEA 打开项目目录(确保 `resources/` 被正常加载)。
- JDK 版本:26(见原 `Readme.md`)。
- 入口类:`src/Main.java`。

## 玩法

- 六边形网格:初始 8 行,最多 17 行,六边形边长 24.22(见 `备注.txt`)。
- 发射弹珠,碰到同色弹珠或触顶时吸附;同色三连及以上消除。
- 整盘每秒下移(每秒 y 轴 + 五分之二六边形边长)。
- 第 4 关触发 Sans Boss,周期施放技能(颜色打乱、bedrock 封锁、整盘下移、creeper 等)并配战斗对话。

## 计分

- 消除 3 个及以内:每个 10 分。
- 消除 4–6 个:每个 15 分。
- 消除 6 个以上:每个 20 分。
- 掉落弹珠:每个 20 分。

(规则见 `备注.txt`。)

## 特殊弹珠

| 弹珠 | 行为 | 出现关卡 |
|------|------|----------|
| 普通 | 同色三连消除 | 全部 |
| creeper | 碰撞后消除周围 +3 范围内的普通弹珠 | 2 / 3 / 4 |
| bedrock | 不可消除的障碍,仅可掉落 | 3 / 4 |
| heart | 不可消除,仅可掉落 | 4 |

## 项目结构

```
src/
├─ Main.java            # 入口
├─ GameEngine.java      # 游戏引擎
├─ ScreenGame.java      # 游戏画面
├─ ScreenStart.java     # 开始画面
├─ Level.java           # 关卡
├─ Marbles.java         # 六边形网格与生成逻辑
├─ Marble.java          # 弹珠实体
├─ MarbleLaunch.java    # 发射弹珠
├─ BossSans.java        # Sans Boss(第 4 关)
└─ ResourceManager.java # 资源管理
resources/              # 图片 / 精灵图
备注.txt                # 开发设计备忘
```

## License

MIT License,详见 [LICENSE](LICENSE)。
