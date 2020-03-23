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
package com.google.idea.blaze.base.editor;

import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import java.util.Map;
import java.util.Set;

/**
 * Super class for Highlight Stats Collectors. All methods can be called multiple times for the same
 * file.
 */
public abstract class BlazeHighlightStatsCollector {
  public static final ExtensionPointName<BlazeHighlightStatsCollector> EP_NAME =
      ExtensionPointName.create("com.google.idea.blaze.BlazeTextHighlightStatsCollector");

  /** Fetch all collectors that can handle a given file. */
  public static Set<BlazeHighlightStatsCollector> getCollectorsSupportingFile(PsiFile file) {
    return BlazeHighlightStatsCollector.EP_NAME.getExtensionList().stream()
        .filter(e -> e.canProcessFile(file))
        .collect(toSet());
  }

  /** Returns a map from {@link HighlightInfoType} to the collectors that support them. */
  public static Map<HighlightInfoType, Set<BlazeHighlightStatsCollector>>
      getHighlightInfoTypesToCollectorsMap(Set<BlazeHighlightStatsCollector> collectors) {
    Map<HighlightInfoType, Set<BlazeHighlightStatsCollector>> map = Maps.newHashMap();
    for (BlazeHighlightStatsCollector collector : collectors) {
      for (HighlightInfoType highlightInfoType : collector.supportedHighlightInfoTypes()) {
        map.computeIfAbsent(highlightInfoType, k -> Sets.newHashSet()).add(collector);
      }
    }
    return map;
  }

  /** Set of {@link HighlightInfoType} supported by a collector */
  public abstract Set<HighlightInfoType> supportedHighlightInfoTypes();

  /**
   * Used to filter collectors that can handle a file. If this method returns false, {@link
   * #processHighlight(PsiElement, HighlightInfo)} will not be called for the given file.
   */
  public abstract boolean canProcessFile(PsiFile file);

  /**
   * For a given file, tests if a HighlightInfo can be processed. Guarantees that {@link
   * #canProcessFile(PsiFile)} was called and asserted true at some point before.
   */
  public abstract boolean canProcessHighlight(PsiElement psiElement, HighlightInfo highlightInfo);

  /**
   * Process the highlights. This will be called under a read-lock, so be aware of how much time
   * this takes.
   *
   * <p>Guarantees that {@link #canProcessFile(PsiFile)} and {@link #canProcessHighlight(PsiElement,
   * HighlightInfo)} both returned true for `psiElement.getContainingFile` and `highlightInfo`.
   */
  public abstract void processHighlight(PsiElement psiElement, HighlightInfo highlightInfo);
}
