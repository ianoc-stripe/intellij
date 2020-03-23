/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.android.editor;

import static java.util.stream.Collectors.joining;

import com.google.android.collect.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.idea.blaze.base.logging.EventLoggingService;
import com.google.idea.blaze.base.logging.utils.HighlightInfo;
import com.google.idea.blaze.base.logging.utils.HighlightStats;
import com.google.idea.blaze.base.sync.SyncMode;
import com.google.idea.blaze.base.sync.SyncResult;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.Nullable;

/** Class to log unresolved resource symbols per project. */
class ProjectUnresolvedResourceStatsCollector {

  private static final int MAX_RECURSE_DEPTH = 4;

  private static final String RESOURCE_PREFIX = "R";
  private static final ImmutableSet<String> RESOURCE_TYPES =
      ImmutableSet.of(
          "animator",
          "anim",
          "color",
          "drawable",
          "layout",
          "id",
          "menu",
          "string",
          "style",
          "font",
          "bool",
          "integer",
          "dimen",
          "array");

  // Regex pattern: R.((animator)|(anim)|(color)|...)
  private static final Pattern RESOURCE_TYPE_PATTERN;
  // Regex pattern: R.((animator)|(anim)|(color)|...)\.\w+
  private static final Pattern RESOURCE_PATTERN;

  static {
    String resourceTypeRegex =
        RESOURCE_TYPES.stream().map(t -> "(" + t + ")").collect(joining("|", "(", ")"));
    resourceTypeRegex = RESOURCE_PREFIX + "\\." + resourceTypeRegex;
    RESOURCE_TYPE_PATTERN = Pattern.compile(resourceTypeRegex);
    RESOURCE_PATTERN = Pattern.compile(resourceTypeRegex + "\\.\\w+");
  }

  private final Map<PsiFile, List<HighlightInfo>> fileToHighlightStats;
  private SyncMode lastSyncMode;
  private SyncResult lastSyncResult;

  ProjectUnresolvedResourceStatsCollector(Project project) {
    fileToHighlightStats = Maps.newHashMap();
  }

  boolean willProcessFile(PsiFile psiFile) {
    return !fileToHighlightStats.containsKey(psiFile);
  }

  void processHighlights(
      PsiFile psiFile, List<com.intellij.codeInsight.daemon.impl.HighlightInfo> highlightInfos) {
    for (com.intellij.codeInsight.daemon.impl.HighlightInfo highlightInfo : highlightInfos) {
      PsiElement psiElement = psiFile.findElementAt(highlightInfo.getActualStartOffset());
      processHighlight(psiElement, highlightInfo);
    }
  }

  void processHighlight(
      PsiElement psiElement, com.intellij.codeInsight.daemon.impl.HighlightInfo highlightInfo) {
    PsiElement resourcePsi = getFullResourceElement(psiElement);
    if (resourcePsi != null) {
      HighlightInfo fileHighlightInfo =
          HighlightInfo.builder()
              .setText(resourcePsi.getText())
              .setSeverity(highlightInfo.getSeverity())
              .setType(highlightInfo.type)
              .setStartOffset(highlightInfo.startOffset)
              .setEndOffset(highlightInfo.endOffset)
              .build();
      fileToHighlightStats
          .computeIfAbsent(psiElement.getContainingFile(), k -> Lists.newArrayList())
          .add(fileHighlightInfo);
    }
  }

  private static PsiElement getFullResourceElement(PsiElement psiElement) {
    return getFullResourceElement(psiElement, 0);
  }

  /**
   * Attempts to recursively find the smallest psiElement that contains the full resource accessor
   * string of type R.abc.xyz
   *
   * @param psiElement element to find the resource string from
   * @param depth current depth. Safeguards against infinite recursion.
   */
  @Nullable("null means psiElement is not a part of a resource string")
  private static PsiElement getFullResourceElement(PsiElement psiElement, int depth) {
    if (depth >= MAX_RECURSE_DEPTH) {
      return null;
    }

    if (RESOURCE_PATTERN.matcher(psiElement.getText()).matches()) {
      return psiElement;
    }

    psiElement = psiElement.getParent();
    String text = psiElement.getText();

    if (RESOURCE_PATTERN.matcher(text).matches()) {
      return psiElement;
    }

    if (RESOURCE_PREFIX.equals(text) || RESOURCE_TYPE_PATTERN.matcher(text).matches()) {
      return getFullResourceElement(psiElement, depth + 1);
    }
    return null;
  }

  void logStats() {
    if (fileToHighlightStats.isEmpty() || lastSyncResult == null) {
      return;
    }
    HighlightStats highlightStats =
        HighlightStats.builder()
            .setType(HighlightStats.Type.ANDROID_RESOURCE_MISSING_REF)
            .setLastSyncMode(lastSyncMode)
            .setLastSyncResult(lastSyncResult)
            .setFileToHighLights(ImmutableMap.copyOf(fileToHighlightStats))
            .build();
    // Log the stats
    EventLoggingService.getInstance().logHighlightStats(highlightStats);
  }

  void onSyncStart(SyncMode syncMode) {
    logStats();
    // Clear for next sync
    fileToHighlightStats.clear();
    lastSyncMode = syncMode;
    lastSyncResult = null;
  }

  void onSyncComplete(SyncMode syncMode, SyncResult syncResult) {
    lastSyncMode = syncMode;
    lastSyncResult = syncResult;
  }
}
