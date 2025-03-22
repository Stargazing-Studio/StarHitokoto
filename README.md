# StarHitokoto 

一个适用于Minecraft 1.21.4的插件，用于从Hitokoto API获取一言，并将其作为PlaceholderAPI变量提供。

---



## 功能特点

- 从Hitokoto API获取一言
- 将一言作为PlaceholderAPI变量提供
- 可配置的API URL、缓存时间和更新间隔
- 异步获取以防止服务器卡顿
- 游戏内命令查看和刷新一言
- 支持在TAB计分板上显示每行8个字符的换行一言
- 玩家加入服务器时发送一言消息（可配置）
- 玩家死亡时发送一言消息（可配置）
- 玩家加入服务器时显示Title欢迎，并在副标题显示一言（可配置）

---



## 系统要求

- Minecraft 1.21.4
- Paper/Spigot 服务器
- PlaceholderAPI 插件

---



## 安装说明

1. 从发布页面下载最新的JAR文件
2. 将JAR文件放入服务器的 `plugins` 目录
3. 重启服务器
4. (可选) 编辑配置文件 `plugins/StarHitokoto/config.yml`

---

## 消息系统

StarHitokoto现在支持在特定事件发生时广播一言消息：

1. **玩家加入消息** - 当玩家加入服务器时，会向所有玩家广播一条带有一言的欢迎消息
2. **玩家死亡消息** - 当玩家死亡时，会向所有玩家广播一条带有一言的死亡消息
3. **玩家加入Title** - 当玩家加入服务器时，会向该玩家显示欢迎Title，并在副标题(subtitle)位置显示一言

这些功能都可以在配置文件中启用或禁用，并且可以自定义消息格式。

#### 消息格式变量

在消息格式中，你可以使用以下变量：

- `%player%` - 玩家名称
- `%hitokoto%` - 当前一言内容

#### 测试消息

你可以使用以下命令测试消息系统：

- `/hitokoto test join` - 测试玩家加入消息
- `/hitokoto test death` - 测试玩家死亡消息

---




## 使用方法

### PlaceholderAPI

安装后，您可以在任何支持PlaceholderAPI的插件中使用以下占位符：

```
%hitokoto_quote%  - 显示完整的一言
%hitokoto_lines%  - 返回分行后的总行数
%hitokoto_line_0% - 显示第一行（每行最多8个字符）
%hitokoto_line_1% - 显示第二行
%hitokoto_line_2% - 显示第三行
...
```

这些占位符在TAB插件的侧边栏配置中特别有用，可以按照每行最多8个字符的方式显示一言。

> **注意**: 如果某一行没有内容（例如，一言内容不够长），对应的占位符将不会返回任何内容，而不是返回空字符串。这样在TAB侧边栏中就不会显示空行。

### 在TAB插件中使用

#### 重要提示

使用TAB侧边栏显示一言时，必须使用 `%hitokoto_line_0%`、`%hitokoto_line_1%` 等占位符分别设置每一行，而不是直接使用 `%hitokoto_quote%`。

这是因为TAB侧边栏不支持自动换行，如果使用 `%hitokoto_quote%`，整个一言会显示在一行内，可能导致显示效果不佳

#### TAB插件精确配置方法

由于TAB插件的配置较为复杂，以下是配置侧边栏显示一言的详细步骤：

1. 打开TAB插件的配置文件（通常位于`plugins/TAB/config.yml`）
2. 在`lines`部分添加以下配置：

```yaml
scoreboard:
  lines:
    - "&r%hitokoto_line_0%"
    - "&r%hitokoto_line_1%"
    - "&r%hitokoto_line_2%"
    - "&r%hitokoto_line_3%"
    - "&r%hitokoto_line_4%"
    # 根据需要添加更多行
```

注意：每个占位符前添加了`&r`（重置颜色代码），这有助于确保每行都正确显示。

---



## 命令

插件提供以下命令：

- `/hitokoto` - 显示当前一言
- `/hitokoto refresh` - 强制刷新一言
- `/hitokoto debug` - 显示调试信息，包括一言内容、分行数量及内容
- `/hitokoto config` - 显示当前配置信息
- `/hitokoto config set <分类> <选项> <值>` - 修改配置选项（无需重启服务器）
- `/hitokoto reload` - 重新加载配置文件
- `/hitokoto test join` - 测试玩家加入消息
- `/hitokoto test death` - 测试玩家死亡消息
- `/hitokoto test title` - 测试玩家加入Title

#### 配置修改命令示例

```
# 开启/关闭调试模式
/hitokoto config set debug true
/hitokoto config set debug false

# 修改玩家加入消息
/hitokoto config set message.join enabled false
/hitokoto config set message.join format "&e欢迎 &b%player% &e加入服务器! &7一言: &f%hitokoto%"

# 修改玩家加入Title
/hitokoto config set message.join.title enabled true
/hitokoto config set message.join.title title "&b欢迎加入服务器, %player%"
/hitokoto config set message.join.title subtitle "&f%hitokoto%"
/hitokoto config set message.join.title fade-in 20
/hitokoto config set message.join.title stay 100
/hitokoto config set message.join.title fade-out 20

# 修改玩家死亡消息
/hitokoto config set message.death enabled true
/hitokoto config set message.death format "&c%player% &7已死亡. &7一言: &f%hitokoto%"

# 修改API设置
/hitokoto config set api url "https://v1.hitokoto.cn/?c=f&encode=text"

# 修改缓存时间（毫秒）
/hitokoto config set cache time 60000

# 修改更新间隔（tick）
/hitokoto config set update interval 1200

# 修改分行设置
/hitokoto config set line max-chars 10
/hitokoto config set line add-color-prefix true
```

别名: `/hito`

---



## 权限说明

以下是插件定义的权限节点及其说明：

| 权限节点 | 描述 | 默认权限 |
|---------|------|---------|
| hitokoto.use | 允许使用基本一言命令 | 所有玩家 |
| hitokoto.refresh | 允许刷新一言句子 | OP |
| hitokoto.debug | 允许查看调试信息 | OP |
| hitokoto.config | 允许查看配置信息 | OP |
| hitokoto.reload | 允许重载插件配置 | OP |
| hitokoto.test.join | 允许测试玩家加入消息 | OP |
| hitokoto.test.death | 允许测试玩家死亡消息 | OP |
| hitokoto.test.title | 允许测试玩家加入标题 | OP |
| hitokoto.admin | 继承所有子权限的父权限 | OP |

---



## 配置

```yaml
# StarHitokoto 配置

# 是否启用调试模式（开启后会在控制台显示更多运行信息）
debug: false

# 一言API地址
api:
  url: "https://v1.hitokoto.cn/?c=f&encode=text"
  
# 缓存设置（毫秒）
cache:
  time: 60000  # 1分钟
  
# 更新间隔（单位：tick，20 tick = 1秒）
update:
  interval: 1200  # 1分钟（20 ticks * 60秒）

# 分行设置
line:
  # 每行最大字符数
  max-chars: 8
  # 是否在每行添加颜色代码前缀（有助于在TAB侧边栏中正确显示）
  add-color-prefix: true

# 消息设置
message:
  # 玩家加入服务器时发送一言
  join:
    enabled: true
    # 消息格式 (%player% 为玩家名称, %hitokoto% 为一言内容)
    format: "&e欢迎 &b%player% &e加入服务器! &7今日一言: &f%hitokoto%"
    # 是否显示Title欢迎玩家
    title:
      enabled: true
      # 大标题内容
      title: "&b欢迎 %player% 加入服务器"
      # 小标题内容（一言）
      subtitle: "&7%hitokoto%"
      # 淡入时间（tick）
      fade-in: 10
      # 停留时间（tick）
      stay: 70
      # 淡出时间（tick）
      fade-out: 20
  
  # 玩家死亡时发送一言
  death:
    enabled: true
    # 消息格式 (%player% 为玩家名称, %hitokoto% 为一言内容)
    format: "&c%player% &7已死亡. &7死亡感言: &f%hitokoto%"
```

---

## 疑难解答


如果你在TAB侧边栏中看不到正确分行的一言，请检查以下事项：

1. **确认占位符格式**：使用`/hitokoto debug`命令查看正确的占位符格式，确保在TAB配置中正确使用每行的占位符（例如`%hitokoto_line_0%`）。
2. **检查TAB配置**：确保TAB插件的`scoreboard`功能已启用，并且配置中的行与占位符一一对应。
3. **添加重置颜色代码**：在每个占位符前添加`&r`或`§r`重置颜色代码，如`&r%hitokoto_line_0%`。
4. **重启服务器**：修改配置后，重启服务器以确保更改生效。
5. **检查日志**：查看服务器日志中`StarHitokoto`的输出，寻找可能的错误或警告。

如果问题仍然存在，可以在控制台输入`/hitokoto debug`命令，将输出的信息提供给开发者，以获取更多帮助。

## 构建

1. 克隆此仓库
2. 使用Maven构建：`mvn clean package`
3. JAR文件将位于`target`目录中
