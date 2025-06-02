# termux-simplest
这是一个根据termux开发的终端模拟器，通过调用系统的/system/bin/sh解释器，运行Linux命令
基于target-28（IPA 28 Android 9.0）开发，可以执行自己的二进制文件和shell脚本，前提是这些二进制文件和shell文件不需要root权限（除非你有ROOT）
这个系统的sh可以执行基本的date,ls,pwd,cd,cp等命令，如果需要高级命令，需要你在应用程序私有目录导入预编译的bash二进制文件，然后通过./bash调用
本软件完全开源，你可以自己复制，修改，编译，发布。但是发布时说明修改位置后添加了什么功能
本软件仅为个人使用，虽然开源，但是严禁用于商业需求
This is a terminal emulator developed according to Termux, which runs Linux commands by calling the system's /system/bin/sh interpreter.
Developed on target-28 (IPA 28 Android 9.0), it is possible to execute its own binaries and shell scripts, provided that these binaries and shell files do not require root privileges (unless you have ROOT).
The sh of this system can execute basic DATE, LS, PWD, CD, CP and other commands, and if you need advanced commands, you need to import the precompiled bash binary file in the application's private directory and then call it via ./bash.
This software is completely open source; you can copy, modify, compile, and distribute it yourself. However, the release states which features were added after the location was modified.
This software is for personal use only and, although open source, is strictly prohibited for commercial use.
