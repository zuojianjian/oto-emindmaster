# YMfileManager功能需求统一和界面统一

内容：

* 1、功能需求
* 3、存在问题
* 4、项目计划
* 5、设计实现


# 1、项目简介
　本应用属于Openthos项目的一部分，提供 Openthos 系统本地文件管理以及Seafile/owncloud云存储支持。

##　当前开发人员 (20160801-20160831)
 左剑剑   王琪
 
# 2、功能需求
####详细的功能需求请参考：<br>
[总体功能需求](https://github.com/openthos/oto-filemanager/tree/master/requirement)   
[seafile功能说明](https://github.com/openthos/oto-filemanager/blob/master/doc/summary/seafile%E5%8A%9F%E8%83%BD%E5%91%BD%E4%BB%A4%E5%88%97%E8%A1%A8.md)<br>
####完成情况如下所示：
| 完成     | 描述     | 模块     | 完成度 |
| ---- |-------    |:---------|:---------| 
| √     | FileManager顶部导航栏    | 界面     | 100% |
| √     | FileManager左侧面板      | 界面     | 100% |
| √     | FileManager右侧文件列表  | 界面     | 100% |
| √     | 云服务界面               | 界面     | 100% |
| √     | 右键菜单                 | 界面     | 100% |
| √     | FileManager左侧USB列表   | 界面     | 100% |
| √     | 属性显示界面             | 界面     | 100% |
| √     | 文件复制进度界面         | 界面     | 100% |
| √     | 目录前进、后退           | 功能     | 100% |
| √     | 鼠标点击区分单击和双击事件（单击锁定，双击打开）                                      | 功能     | 100% |
| √     | 两种本地文件浏览视图：图标形式，列表形式                                              | 功能     | 100% |
| √     | 右键菜单：打开、打开方式、刷新、新建文件/文件夹、复制、剪切、粘贴、删除、重命名、属性 | 功能     | 100% |
| √     | 路径框（显示当前路径，输入路径）                                                      | 功能     | 100% |
| √     | 搜索框文件搜索                                                                        | 功能     | 100% |
| √     | 热键功能支持：Ctrl+C、Ctrl+V、Ctrl+A、Ctrl+X、Ctrl+D                                  | 功能     | 100% |
| √     | U盘动态识别，手动弹出U盘                                                              | 功能     | 100% |


| 计划进行     | 描述     | 模块     | 完成度 |
| ---- |-------    |:---------|:---------| 
| x     | 文件拖拽                                                                              | 功能     |   0% |
| x     | 文件框选                                                                              | 功能     |   0% |
| x     | 网上邻居                                                                              | 功能     |   0% |
| x     | Seafile文件/文件夹自动同步                                                            | 功能     |  0% |
| x     | Seafile显示文件目录列表盘                                                             | 功能     |  0% |
| x     | Seafile增加/解除同步文件夹                                                            | 功能     |  0% |
| x     | Owncloud文件/文件夹自动同步                                                            | 功能     | 0% |
| x     | Owncloud显示文件目录列表盘                                                             | 功能     | 0% |
| x     | Owncloud增加/解除同步文件夹                                                            | 功能     | 0% |

# 3、项目进度计划

| 开始时间  | 结束时间  | 内容 | 人员|
| ---- |------- |-------|:---------|
|2016.08.08| 2016.08.12| 确定UI更改方案并修改调整UI和功能代码調整| 左剑剑|
|2016.08.15|	2016.08.19|	实现Filemanager 文件拖拽功能|左剑剑|
|2016.08.22|	2016.08.26|	实现文件框选功能需求|左剑剑|
|2016.08.29|	2016.08.31|	Seafile云同步功能开始实现|左剑剑|

#4、设计与实现
##5.1主要代码结构统一
（采用YMfileManager）
##5.2界面打统一设计与实现
（由吴老师提供打设计方案）
##5.3功能的统一与实现
（实现功能与oto-filemanager有略微的区别统一调整）
##5.4构建 && 安装 && 运行
请查看：[building.md](https://github.com/openthos/oto-filemanager/blob/master/doc/summary/building.md)<br>
apk程序及压缩包地址：https://github.com/openthos/oto-filemanager/tree/master/app（后续添加与修改）
##5.5工程文件地址
请查看：（后续添加）
