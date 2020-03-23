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

import com.google.idea.common.experiments.BoolExperiment;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.HighlightVisitor;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Class to visit all highlights in a file, and pipe them to {@link BlazeHighlightStatsCollector}
 * for collecting stats on the highlighting information.
 *
 * <p>{@link com.intellij.codeInsight.daemon.impl.GeneralHighlightingPass} shows how classes
 * implementing {@link HighlightVisitor} are called.
 *
 * <pre>
 * Call order:
 *   1. {@link #clone()} : once for each file
 *   2. {@link #suitableForFile(PsiFile)} : to filter visitors
 *   3. {@link #analyze(PsiFile, boolean, HighlightInfoHolder, Runnable)} : analyze is used to
 *          propagate results. Runnable must be run, and the method must return `true`.
 *   4. {@link #visit(PsiElement)} : called once per psiElement in the file.
 *          highlightInfoHolder will contain relevant HighlightInfo for the PsiElement
 * </pre>
 */
public class BlazeHighlightVisitor implements HighlightVisitor {

  private static final BoolExperiment visitorIsEnabled =
      new BoolExperiment("blaze.highlight.visitor.enabled", true);

  private HighlightInfoHolder highlightInfoHolder;
  private Map<HighlightInfoType, Set<BlazeHighlightStatsCollector>> infoTypesToCollectorsMap;

  @Override
  public boolean suitableForFile(@NotNull PsiFile psiFile) {
    if (!visitorIsEnabled.getValue()) {
      return false;
    }
    Set<BlazeHighlightStatsCollector> collectors =
        BlazeHighlightStatsCollector.getCollectorsSupportingFile(psiFile);
    if (!collectors.isEmpty()) {
      // Preemptively fetch the highlight info types we need.
      infoTypesToCollectorsMap =
          BlazeHighlightStatsCollector.getHighlightInfoTypesToCollectorsMap(collectors);
      return true;
    }
    return false;
  }

  @Override
  public void visit(@NotNull PsiElement psiElement) {
    if (highlightInfoHolder == null) {
      return;
    }

    for (int i = 0; i < highlightInfoHolder.size(); i++) {
      HighlightInfo highlightInfo = highlightInfoHolder.get(i);
      if (infoTypesToCollectorsMap.containsKey(highlightInfo.type)) {
        infoTypesToCollectorsMap.get(highlightInfo.type).stream()
            .filter(c -> c.canProcessHighlight(psiElement, highlightInfo))
            .forEach(c -> c.processHighlight(psiElement, highlightInfo));
      }
    }
  }

  @Override
  public boolean analyze(
      @NotNull PsiFile psiFile,
      boolean updateWholeFile,
      @NotNull HighlightInfoHolder highlightInfoHolder,
      @NotNull Runnable runnable) {
    try {
      if (!visitorIsEnabled.getValue() || !updateWholeFile) {
        return true;
      }
      this.highlightInfoHolder = highlightInfoHolder;
    } finally {
      try {
        runnable.run();
      } finally {
        this.highlightInfoHolder = null;
      }
    }
    return true;
  }

  @Override
  public HighlightVisitor clone() {
    return new BlazeHighlightVisitor();
  }
}
