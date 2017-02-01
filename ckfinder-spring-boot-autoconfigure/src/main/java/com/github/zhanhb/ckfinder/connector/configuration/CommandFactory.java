package com.github.zhanhb.ckfinder.connector.configuration;

import com.github.zhanhb.ckfinder.connector.handlers.command.Command;
import com.github.zhanhb.ckfinder.connector.handlers.command.CopyFilesCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.CreateFolderCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.DeleteFilesCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.DeleteFolderCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.DownloadFileCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.FileUploadCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.GetFilesCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.GetFoldersCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.InitCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.MoveFilesCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.QuickUploadCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.RenameFileCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.RenameFolderCommand;
import com.github.zhanhb.ckfinder.connector.handlers.command.ThumbnailCommand;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zhanhb
 */
public class CommandFactory {

  private final Map<String, Command<?>> commands;

  public CommandFactory() {
    Map<String, Command<?>> map = new HashMap<>(20);
    /**
     * init command.
     */
    map.put("INIT", new InitCommand());
    /**
     * get subfolders for selected location command.
     */
    map.put("GETFOLDERS", new GetFoldersCommand());
    /**
     * get files from current folder command.
     */
    map.put("GETFILES", new GetFilesCommand());
    /**
     * get thumbnail for file command.
     */
    map.put("THUMBNAIL", new ThumbnailCommand());
    /**
     * download file command.
     */
    map.put("DOWNLOADFILE", new DownloadFileCommand());
    /**
     * create subfolder.
     */
    map.put("CREATEFOLDER", new CreateFolderCommand());
    /**
     * rename file.
     */
    map.put("RENAMEFILE", new RenameFileCommand());
    /**
     * rename folder.
     */
    map.put("RENAMEFOLDER", new RenameFolderCommand());
    /**
     * delete folder.
     */
    map.put("DELETEFOLDER", new DeleteFolderCommand());
    /**
     * copy files.
     */
    map.put("COPYFILES", new CopyFilesCommand());
    /**
     * move files.
     */
    map.put("MOVEFILES", new MoveFilesCommand());
    /**
     * delete files.
     */
    map.put("DELETEFILES", new DeleteFilesCommand());
    /**
     * file upload.
     */
    map.put("FILEUPLOAD", new FileUploadCommand());
    /**
     * quick file upload.
     */
    map.put("QUICKUPLOAD", new QuickUploadCommand());
    commands = map;
  }

  public Command<?> getCommand(String commandName) {
    return commands.get(commandName.toUpperCase());
  }

}
