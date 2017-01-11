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

import com.github.zhanhb.ckfinder.connector.data.AccessControlLevel;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.ToString;

/**
 * Class to generate ACL values.
 */
@Builder(builderClassName = "Builder")
@SuppressWarnings({"FinalClass", "AccessingNonPublicFieldOfAnotherObject"})
@ToString
public final class AccessControl {

  /**
   * Folder view mask.
   */
  public static final int CKFINDER_CONNECTOR_ACL_FOLDER_VIEW = 1;
  /**
   * Folder create mask.
   */
  public static final int CKFINDER_CONNECTOR_ACL_FOLDER_CREATE = 1 << 1;
  /**
   * Folder rename mask.
   */
  public static final int CKFINDER_CONNECTOR_ACL_FOLDER_RENAME = 1 << 2;
  /**
   * Folder delete mask.
   */
  public static final int CKFINDER_CONNECTOR_ACL_FOLDER_DELETE = 1 << 3;
  /**
   * File view mask.
   */
  public static final int CKFINDER_CONNECTOR_ACL_FILE_VIEW = 1 << 4;
  /**
   * File upload mask.
   */
  public static final int CKFINDER_CONNECTOR_ACL_FILE_UPLOAD = 1 << 5;
  /**
   * File rename mask.
   */
  public static final int CKFINDER_CONNECTOR_ACL_FILE_RENAME = 1 << 6;
  /**
   * File delete mask.
   */
  public static final int CKFINDER_CONNECTOR_ACL_FILE_DELETE = 1 << 7;

  /**
   * mask configuration.
   */
  @Singular
  private final List<AccessControlLevel> aclEntries;

  /**
   * check ACL for folder.
   *
   * @param resourceType resource type name
   * @param folder folder name
   * @param acl mask to check.
   * @param currentUserRole user role
   * @return true if acl flag is true
   */
  public boolean hasPermission(String resourceType, String folder, String currentUserRole, int acl) {
    return (checkACLForRole(resourceType, folder, currentUserRole) & acl) == acl;
  }

  /**
   * Checks ACL for given role.
   *
   * @param resourceType resource type
   * @param folder current folder
   * @param currentUserRole current user role
   * @return mask value
   */
  public int checkACLForRole(String resourceType, String folder, String currentUserRole) {
    final CheckEntry[] ce = currentUserRole != null ? new CheckEntry[]{
      new CheckEntry("*", "*"),
      new CheckEntry("*", resourceType),
      new CheckEntry(currentUserRole, "*"),
      new CheckEntry(currentUserRole, resourceType)
    } : new CheckEntry[]{
      new CheckEntry("*", "*"),
      new CheckEntry("*", resourceType)
    };

    int acl = 0;
    for (CheckEntry checkEntry : ce) {
      List<AccessControlLevel> aclEntrieForType = findACLEntryByRoleAndType(checkEntry);

      for (AccessControlLevel aclEntry : aclEntrieForType) {
        String cuttedPath = folder;

        while (true) {
          if (cuttedPath.length() > 1
                  && cuttedPath.lastIndexOf('/') == cuttedPath.length() - 1) {
            cuttedPath = cuttedPath.substring(0, cuttedPath.length() - 1);
          }
          if (aclEntry.getFolder().equals(cuttedPath)) {
            acl |= checkACLForFolder(aclEntry, cuttedPath);
            break;
          } else if (cuttedPath.length() == 1) {
            break;
          } else if (cuttedPath.lastIndexOf('/') > -1) {
            cuttedPath = cuttedPath.substring(0,
                    cuttedPath.lastIndexOf('/') + 1);
          } else {
            break;
          }
        }
      }
    }
    return acl;
  }

  /**
   * Checks ACL for given folder.
   *
   * @param entry current ACL entry
   * @param folder current folder
   * @return mask value
   */
  private int checkACLForFolder(AccessControlLevel entry, String folder) {
    int acl = 0;
    if (folder.contains(entry.getFolder()) || entry.getFolder().equals(File.separator)) {
      acl ^= entry.getMask();
    }
    return acl;
  }

  /**
   * Gets a list of ACL entries for current role and resource type.
   *
   * @param type resource type
   * @param role current user role
   * @return list of ACL entries.
   */
  private List<AccessControlLevel> findACLEntryByRoleAndType(CheckEntry checkEntry) {
    return aclEntries.stream()
            .filter(item -> (item.getRole().equals(checkEntry.role) && item.getResourceType().equals(checkEntry.type)))
            .collect(Collectors.toList());
  }

  /**
   * simple check ACL entry.
   */
  @RequiredArgsConstructor
  private static class CheckEntry {

    private final String role;
    private final String type;

  }

}
