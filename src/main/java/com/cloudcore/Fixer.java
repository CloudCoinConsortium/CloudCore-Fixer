package com.cloudcore;

import com.cloudcore.desktop.core.FileSystem;
import com.cloudcore.desktop.utils.SimpleLogger;
import com.cloudcore.desktop.utils.Utils;

import java.io.File;
import java.io.FileWriter;

public class Fixer {

    final static String BasePath = Utils.GetWorkDirPath();
    final static String CommandPath = BasePath + "Command";
    final static String LogPath = BasePath + "Logs";
    static Echoer echoer = new Echoer();
    public static void main(String[] args) {
        // TODO code application logic here

        File dirCommand = new File(CommandPath);
        File dirLog = new File(LogPath);


        // Create Command and Log folders if they dont exist.

        if (! dirCommand.exists()){
            dirCommand.mkdirs();
        }

        if (! dirLog.exists()){
            dirLog.mkdirs();
        }
        FileSystem.createDirectories();
        SimpleLogger.writeLog("ServantEchoerStarted", "");
        System.out.println("FrackFixer Started");

        //echoer.Echo(FileSystem.BasePath);
    }
}
