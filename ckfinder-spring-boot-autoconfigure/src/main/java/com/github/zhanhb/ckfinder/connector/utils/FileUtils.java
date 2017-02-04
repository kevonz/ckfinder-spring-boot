/*
 * CKFinder
 * ========
 * http://cksource.com/ckfinder
 * Copyright (C) 2007-2015, CKSource - Frederico Knabben. All rights reserved.
 *
 * The software, this file and its contents are subject to the CKFinder
 * License. Please read the license.txt file before using, installing, copying,
 * modifying or distribute this file or part of its contents. The contents of
 * this file is part of the Source Code of CKFinder.
 */
package com.github.zhanhb.ckfinder.connector.utils;

import com.github.zhanhb.ckfinder.connector.configuration.Constants;
import com.github.zhanhb.ckfinder.connector.configuration.IConfiguration;
import com.github.zhanhb.ckfinder.connector.data.ResourceType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.servlet.http.Part;

/**
 * Utils for files.
 *
 */
@SuppressWarnings({"UtilityClassWithoutPrivateConstructor", "NestedAssignment"})
public class FileUtils {

  /**
   * max read file buffer size.
   */
  private static final int MAX_BUFFER_SIZE = 1024;
  private static final Pattern invalidFileNamePatt = Pattern.compile(Constants.INVALID_FILE_NAME_REGEX);

  private static final URLEncoder URI_COMPONENT = new URLEncoder("-_.*!'()~");
  // https://tools.ietf.org/html/rfc5987#section-3.2.1
  // we will encoding + for some browser will decode + to a space
  private static final URLEncoder CONTENT_DISPOSITION = new URLEncoder("!#$&-.^_`|~");

  /**
   * Gets list of children folder or files for dir, according to searchDirs
   * param.
   *
   * @param dir folder to search.
   * @param searchDirs if true method return list of folders, otherwise list of
   * files.
   * @return list of files or subdirectories in selected directory
   * @throws java.io.IOException
   */
  public static List<String> findChildrensList(Path dir, boolean searchDirs)
          throws IOException {
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, file -> searchDirs == Files.isDirectory(file))) {
      return StreamSupport.stream(ds.spliterator(), false)
              .map(Path::getFileName)
              .map(Object::toString)
              .collect(Collectors.toList());
    }
  }

  /**
   * Gets file extension.
   *
   * @param fileName name of file.
   * @param shortExtensionMode
   * @return file extension
   */
  @Nullable
  public static String getFileExtension(String fileName, boolean shortExtensionMode) {
    if (shortExtensionMode) {
      return FileUtils.getFileExtension(fileName);
    }
    int indexOf;
    if (fileName == null || (indexOf = fileName.indexOf('.')) == -1
            || indexOf == fileName.length() - 1) {
      return null;
    }
    return fileName.substring(indexOf + 1);
  }

  /**
   * Gets file last extension.
   *
   * @param fileName name of file.
   * @return file extension
   */
  @Nullable
  public static String getFileExtension(String fileName) {
    int lastIndexOf;
    if (fileName == null || (lastIndexOf = fileName.lastIndexOf('.')) == -1
            || lastIndexOf == fileName.length() - 1) {
      return null;
    }
    return fileName.substring(lastIndexOf + 1);
  }

  /**
   * Gets file name without its extension.
   *
   * @param fileName name of file
   * @param shortExtensionMode
   * @return file extension
   */
  @Nullable
  public static String getFileNameWithoutExtension(String fileName, boolean shortExtensionMode) {
    if (shortExtensionMode) {
      return FileUtils.getFileNameWithoutExtension(fileName);
    }

    int indexOf;
    if (fileName == null || (indexOf = fileName.indexOf('.')) == -1) {
      return null;
    }
    return fileName.substring(0, indexOf);
  }

  /**
   * Gets file name without its last extension.
   *
   * @param fileName name of file
   * @return file extension
   */
  @Nullable
  public static String getFileNameWithoutExtension(String fileName) {
    int lastIndexOf;
    if (fileName == null || (lastIndexOf = fileName.lastIndexOf('.')) == -1) {
      return null;
    }
    return fileName.substring(0, lastIndexOf);
  }

  /**
   * Print file content to outputstream.
   *
   * @param file file to be printed.
   * @param out outputstream.
   * @throws IOException when io error occurs.
   */
  public static void printFileContentToResponse(Path file, OutputStream out)
          throws IOException {
    Files.copy(file, out);
  }

  /**
   *
   * @param sourceFile source file
   * @param destFile destination file
   * @param move if source file should be deleted.
   * @return true if file moved/copied correctly
   * @throws IOException when IOerror occurs
   */
  public static boolean copyFromSourceToDestFile(Path sourceFile, Path destFile,
          boolean move) throws IOException {
    createPath(destFile, true);
    if (move) {
      Files.move(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING);
    } else {
      Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING);
    }
    return true;

  }

  /**
   * Parse date with pattern yyyyMMddHHmm. Pattern is used in get file command
   * response XML.
   *
   * @param file input file.
   * @return parsed file modification date.
   * @throws java.io.IOException
   */
  public static String parseLastModifDate(Path file) throws IOException {
    Instant instant = Files.getLastModifiedTime(file).toInstant();
    return DateTimeFormatterHolder.FORMATTER.format(instant);
  }

  /**
   * check if dirname matches configuration hidden folder regex.
   *
   * @param dirName dir name
   * @param conf connector configuration
   * @return true if matches.
   */
  public static boolean isDirectoryHidden(String dirName,
          IConfiguration conf) {
    if (dirName == null || dirName.isEmpty()) {
      return false;
    }
    String dir = PathUtils.removeSlashFromEnd(PathUtils.escape(dirName));
    StringTokenizer sc = new StringTokenizer(dir, "/");
    Pattern pattern = Pattern.compile(getHiddenFileOrFolderRegex(
            conf.getHiddenFolders()));
    while (sc.hasMoreTokens()) {
      if (pattern.matcher(sc.nextToken()).matches()) {
        return true;
      }
    }
    return false;
  }

  /**
   * check if filename matches configuration hidden file regex.
   *
   * @param fileName file name
   * @param conf connector configuration
   * @return true if matches.
   */
  public static boolean isFileHidden(String fileName, IConfiguration conf) {
    return Pattern.compile(getHiddenFileOrFolderRegex(
            conf.getHiddenFiles())).matcher(fileName).matches();
  }

  /**
   * get hidden folder regex pattern.
   *
   * @param hiddenList list of hidden file or files patterns.
   * @return full folder regex pattern
   */
  private static String getHiddenFileOrFolderRegex(List<String> hiddenList) {
    StringBuilder sb = new StringBuilder("(");
    for (String item : hiddenList) {
      if (sb.length() > 3) {
        sb.append("|");
      }

      sb.append("(");
      sb.append(item.replace(".", "\\.").replace("*", ".+").replace("?", "."));
      sb.append(")");
    }
    sb.append(")+");
    return sb.toString();
  }

  /**
   * deletes file or folder with all subfolders and subfiles.
   *
   * @param file file or directory to delete.
   * @return true if all files are deleted.
   */
  public static boolean delete(Path file) {
    try {
      DeleteHelper.delete(file);
      return true;
    } catch (IOException ex) {
      return false;
    }
  }

  /**
   * check if file or folder name doesn't match invalid name.
   *
   * @param fileName file name
   * @return true if file name is correct
   */
  public static boolean isFileNameInvalid(String fileName) {
    return !(fileName == null || fileName.isEmpty()
            || fileName.charAt(fileName.length() - 1) == '.'
            || fileName.contains("..")
            || isFileNameCharacterInvalid(fileName));
  }

  /**
   * check if new folder name contains disallowed chars.
   *
   * @param fileName file name
   * @return true if it does contain disallowed characters.
   */
  private static boolean isFileNameCharacterInvalid(String fileName) {
    return invalidFileNamePatt.matcher(fileName).find();
  }

  /**
   * checks if file extension is on denied list or isn't on allowed list.
   *
   * @param fileName filename
   * @param type resource type
   * @return 0 if ok, 1 if not ok, 2 if rename required
   */
  public static int checkFileExtension(String fileName,
          ResourceType type) {
    if (type == null || fileName == null) {
      return 1;
    }

    if (fileName.indexOf('.') == -1) {
      return 0;
    }

    return isExtensionAllowed(getFileExtension(fileName), type) ? 0 : 1;
  }

  /**
   * Checks whether files extension is allowed.
   *
   * @param fileExt a string representing file extension to test
   * @param type a {@code ResourceType} object holding list of allowed and
   * denied extensions against which parameter fileExt will be tested
   * @return {@code true} is extension is on allowed extensions list or if
   * allowed extensions is empty. The {@code false} is returned when file is on
   * denied extensions list or if none of the above conditions is met.
   */
  private static boolean isExtensionAllowed(String fileExt, ResourceType type) {
    StringTokenizer st = new StringTokenizer(type.getDeniedExtensions(), ",");
    while (st.hasMoreTokens()) {
      if (st.nextToken().equalsIgnoreCase(fileExt)) {
        return false;
      }
    }

    st = new StringTokenizer(type.getAllowedExtensions(), ",");
    //The allowedExtensions is empty. Allow everything that isn't dissallowed.
    if (!st.hasMoreTokens()) {
      return true;
    }

    do {
      if (st.nextToken().equalsIgnoreCase(fileExt)) {
        return true;
      }
    } while (st.hasMoreTokens());
    return false;
  }

  /**
   * converts filename to ASCII.
   *
   * @param fileName file name
   * @return encoded file name
   */
  public static String convertToASCII(String fileName) {
    return Utf8AccentsHolder.convert(fileName);
  }

  /**
   * creates file and all above folders that do not exist.
   *
   * @param file file to create.
   * @param asFile if it is path to folder.
   * @throws IOException when io error occurs.
   */
  public static void createPath(Path file, boolean asFile) throws IOException {
    if (asFile) {
      Path path = file.toAbsolutePath();
      Path dir = path.getParent();
      if (dir != null) {
        Files.createDirectories(dir);
      }
      Files.createFile(path);
    } else {
      Files.createDirectories(file);
    }
  }

  /**
   * check if file size isn't bigger then max size for type.
   *
   * @param type resource type
   * @param fileSize file size
   * @return true if file size isn't bigger then max size for type.
   */
  public static boolean isFileSizeInRange(ResourceType type, long fileSize) {
    final long maxSize = type.getMaxSize();
    return (maxSize == 0 || maxSize > fileSize);
  }

  /**
   * check if file has html file extension.
   *
   * @param file file name
   * @param configuration connector configuration
   * @return true if has
   */
  public static boolean isExtensionHtml(String file,
          IConfiguration configuration) {
    String extension = getFileExtension(file);
    return extension != null && configuration.getHtmlExtensions().contains(
            extension.toLowerCase());
  }

  /**
   * Detect HTML in the first KB to prevent against potential security issue
   * with IE/Safari/Opera file type auto detection bug. Returns true if file
   * contain insecure HTML code at the beginning.
   *
   * @param item file upload item
   * @return true if detected.
   * @throws IOException when io error occurs.
   */
  public static boolean hasHtmlContent(Part item) throws IOException {
    byte[] buff = new byte[MAX_BUFFER_SIZE];
    try (InputStream is = item.getInputStream()) {
      is.read(buff, 0, MAX_BUFFER_SIZE);
      String content = new String(buff);
      content = content.toLowerCase().trim();

      if (Pattern.compile("<!DOCTYPE\\W+X?HTML.+",
              Pattern.CASE_INSENSITIVE
              | Pattern.DOTALL
              | Pattern.MULTILINE).matcher(content).matches()) {
        return true;
      }

      String[] tags = {"<body", "<head", "<html", "<img", "<pre",
        "<script", "<table", "<title"};

      for (String tag : tags) {
        if (content.contains(tag)) {
          return true;
        }
      }

      if (Pattern.compile("type\\s*=\\s*['\"]?\\s*(?:\\w*/)?(?:ecma|java)",
              Pattern.CASE_INSENSITIVE
              | Pattern.DOTALL
              | Pattern.MULTILINE).matcher(content).find()) {
        return true;
      }

      if (Pattern.compile(
              "(?:href|src|data)\\s*=\\s*['\"]?\\s*(?:ecma|java)script:",
              Pattern.CASE_INSENSITIVE
              | Pattern.DOTALL
              | Pattern.MULTILINE).matcher(content).find()) {
        return true;
      }

      if (Pattern.compile("url\\s*\\(\\s*['\"]?\\s*(?:ecma|java)script:",
              Pattern.CASE_INSENSITIVE
              | Pattern.DOTALL
              | Pattern.MULTILINE).matcher(content).find()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if folder has any subfolders but respects ACL and hideFolders
   * setting from configuration.
   *
   * @param accessControl
   * @param dirPath path to current folder.
   * @param dir current folder being checked. Represented by File object.
   * @param configuration configuration object.
   * @param resourceType name of resource type, folder is assigned to.
   * @param currentUserRole user role.
   * @return true if there are any allowed and non-hidden subfolders.
   */
  public static boolean hasChildren(AccessControl accessControl, String dirPath,
          Path dir, IConfiguration configuration, String resourceType,
          String currentUserRole) {
    try (DirectoryStream<Path> list = Files.newDirectoryStream(dir, Files::isDirectory)) {
      if (list != null) {
        for (Path path : list) {
          String subDirName = path.getFileName().toString();
          if (!FileUtils.isDirectoryHidden(subDirName, configuration)
                  && accessControl.hasPermission(resourceType,
                          dirPath + subDirName, currentUserRole, AccessControl.CKFINDER_CONNECTOR_ACL_FOLDER_VIEW)) {
            return true;
          }
        }
      }
    } catch (IOException ex) {
    }
    return false;
  }

  /**
   * rename file with double extension.
   *
   * @param type a {@code ResourceType} object holding list of allowed and
   * denied extensions against which file extension will be tested.
   * @param fileName file name
   * @return new file name with . replaced with _ (but not last)
   */
  public static String renameFileWithBadExt(ResourceType type, String fileName) {
    if (type == null || fileName == null) {
      return null;
    }

    if (fileName.indexOf('.') == -1) {
      return fileName;
    }

    StringTokenizer tokens = new StringTokenizer(fileName, ".");
    String currToken = tokens.nextToken();
    if (tokens.hasMoreTokens()) {
      StringBuilder cfileName = new StringBuilder(fileName.length()).append(currToken);
      boolean more;
      do {
        currToken = tokens.nextToken();
        more = tokens.hasMoreElements();
        if (more) {
          cfileName.append(isExtensionAllowed(currToken, type) ? '.' : '_').append(currToken);
        } else {
          cfileName.append('.').append(currToken);
        }
      } while (more);
      return cfileName.toString();
    }
    return currToken;
  }

  public static String encodeURIComponent(String fileName) {
    return URI_COMPONENT.encode(fileName);
  }

  public static String encodeContentDisposition(String fileName) {
    return CONTENT_DISPOSITION.encode(fileName);
  }

  public static boolean isFolderNameInvalid(String folderName, IConfiguration configuration) {
    return !((configuration.isDisallowUnsafeCharacters()
            && (folderName.contains(".") || folderName.contains(";")))
            || FileUtils.isFileNameCharacterInvalid(folderName));
  }

  public static boolean isFileNameInvalid(String fileName, IConfiguration configuration) {
    return !((configuration.isDisallowUnsafeCharacters() && fileName.contains(";"))
            || !FileUtils.isFileNameInvalid(fileName));
  }

  public static String backupWithBackSlash(String fileName, String toReplace) {
    return fileName.replace(toReplace, "\\" + toReplace);
  }

  private static class EncodingMapHolder {

    static final Map<String, String> encodingMap;

    static {
      Map<String, String> mapHelper = new HashMap<>(6);
      mapHelper.put("%21", "!");
      mapHelper.put("%27", "'");
      mapHelper.put("%28", "(");
      mapHelper.put("%29", ")");
      mapHelper.put("%7E", "~");
      mapHelper.put("[+]", "%20");
      encodingMap = Collections.unmodifiableMap(mapHelper);
    }

  }

  private static class DateTimeFormatterHolder {

    static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm", Locale.US)
            .withZone(ZoneId.of("GMT"));

  }

}
