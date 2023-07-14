# sqliteorz

a simple sqlite orm which names orz

> 这是一个简单的sqlite orm  
> 如果你希望可以使用少量的代码操作安卓sqlite   
> 并且是作为学习或者小项目开发（不建议上生产，郭神的[LitePal](https://github.com/guolindev/LitePal)还是有保障的）  
> 那么它很适合你   
> 你只需要将你的数据库model继承DBBaseModel，初始化数据库   
> 就可以不再对每个字段从cursor赋值了   
> 一起来看看吧  
  
  
  
### 如何引入
```groovy
// corp from [jitpack](https://jitpack.io)
// step 1. Add the JitPack repository to your repositories
repositories {
    mavenCentral()
    maven { url "https://jitpack.io" }
}

// Step 2. Add the dependency
dependencies {
    implementation 'com.github.lawnvi:sqliteorz:1.0.0'
}

```
  
  
  
### 如何使用
```kotlin
/**
 * @DemoModel 让你的数据库的模型继承DBBaseModel
 * 请注意，至少需要一个空的构造函数，用于反射获取表信息
 */
data class DemoModel(
    var name: String = "",
    var age: Int = 18,
    var gender: Boolean = true,
    var content: String = "",
)

/**
 * 配置数据库名称，位置，表
 * 在可以获取到context的地方初始化数据库
 * 比如MainActivity的onCreate()
 */

/**
 * 三个参数均有默认值
 * name=package name
 * dirPath=/sdcard/Android/package/files/database
 * version=1, 需要注意的是，当增加或者变更model字段后需要改变version，此时会重新创建数据库，数据丢失
 * 郭神的LitePal好像是可以保留数据更新的
 */
val driver = DBDriver(name = "db", version = 1, dirPath = "/sdcard")
// 注册模型到数据库
driver.registerModel(DemoModel::class.java)
// 初始化数据库，为啥是array，因为支持多个数据库，厉害吧
OrzHelper.initialize(this.applicationContext, drivers = arrayOf(driver))

/**
 * 以下是增改查删的操作
 * 只有以下操作
 */

// insert
val obj = DemoModel("张三", age = (System.currentTimeMillis()%100).toInt())
obj.save()

// update, 存在id时 where: "id = $id"，set: map
obj.update(mapOf("name" to "李四"))
// 或者new一个对像来update
DemoModel("").update(mapOf("name" to "李四"), "id > 1")
// 或者使用DBOrz
DBOrz.update<DemoModel>(mapOf("name" to "李四"), "id > 1")

// select
val objList = DBOrz.findModels<DemoModel>("id > 0")

// delete
obj.delete()

// count
val count = DBOrz.count<DemoModel>("id > 0")

```




### 有图有真相
<div style="display: inline-block;">
<img  src="https://raw.githubusercontent.com/lawnvi/sqliteorz/main/.github/images/1.jpg" width="22%" />
<img  src="https://raw.githubusercontent.com/lawnvi/sqliteorz/main/.github/images/2.jpg" width="22%" />
<img  src="https://raw.githubusercontent.com/lawnvi/sqliteorz/main/.github/images/3.jpg" width="22%"/>
<img  src="https://raw.githubusercontent.com/lawnvi/sqliteorz/main/.github/images/4.jpg" width="22%">
</div>