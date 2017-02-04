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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 *
 * @author zhanhb
 */
public class CommandFactory {

  private final Map<String, Command<?>> commands = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

  public CommandFactory enableDefaultCommands() {
    return registerCommands(new InitCommand(),
            new GetFoldersCommand(),
            new GetFilesCommand(),
            new ThumbnailCommand(),
            new DownloadFileCommand(),
            new CreateFolderCommand(),
            new RenameFileCommand(),
            new RenameFolderCommand(),
            new DeleteFolderCommand(),
            new CopyFilesCommand(),
            new MoveFilesCommand(),
            new DeleteFilesCommand(),
            new FileUploadCommand(),
            new QuickUploadCommand());
  }

  public CommandFactory registerCommands(Command<?>... commands) {
    Map<String, Command<?>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (Command<?> command : commands) {
      String className = command.getClass().getSimpleName();
      final String suffix = "Command";
      if (!className.endsWith(suffix)) {
        throw new IllegalArgumentException("Can't detect command name for class " + command.getClass().getName());
      }
      String name = className.substring(0, className.length() - suffix.length());
      if (map.put(name, command) != null) {
        throw new IllegalArgumentException("duplicate command '" + name + "'");
      }
    }
    return registerCommands(map);
  }

  public CommandFactory registerCommand(String name, Command<?> command) {
    return registerCommands(Collections.singletonMap(name, command));
  }

  private CommandFactory registerCommands(Map<String, ? extends Command<?>> commands) {
    for (Map.Entry<String, ? extends Command<?>> entry : commands.entrySet()) {
      String name = entry.getKey();
      Objects.requireNonNull(entry.getValue());
      if (this.commands.containsKey(name)) {
        throw new IllegalArgumentException("duplicate command '" + name + "'");
      }
    }
    this.commands.putAll(commands);
    return this;
  }

  public Command<?> getCommand(String commandName) {
    return commands.get(commandName);
  }

}
