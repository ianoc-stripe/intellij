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

import com.google.common.collect.ImmutableSet;
import com.google.idea.blaze.base.editor.BlazeHighlightStatsCollector;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.projectview.ProjectViewSet;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.base.settings.BlazeImportSettings;
import com.google.idea.blaze.base.sync.SyncListener;
import com.google.idea.blaze.base.sync.SyncMode;
import com.google.idea.blaze.base.sync.SyncResult;
import com.google.idea.common.experiments.BoolExperiment;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Stats collector to log the number of unresolved references to resource symbols.
 *
 * <p>Data is logged once per sync, or when the project is closed.
 */
public class UnresolvedResourceStatsCollector extends BlazeHighlightStatsCollector
    implements SyncListener, ProjectManagerListener {

  private static final BoolExperiment unresResourceStatsCollectorEnabled =
      new BoolExperiment("android.unresolved.resource.stats.enabled", true);

  private static final ImmutableSet<FileType> SUPPORTED_FILE_TYPES =
      ImmutableSet.of(JavaFileType.INSTANCE);
  private static final ImmutableSet<HighlightInfoType> SUPPORTED_HIGHLIGHT_TYPES =
      ImmutableSet.of(HighlightInfoType.WRONG_REF);

  // Keep track of project specific collectors
  // Most tasks are delegated to the project specific collector.
  private final Map<Project, ProjectUnresolvedResourceStatsCollector> projectToCollector;

  UnresolvedResourceStatsCollector() {
    projectToCollector = new HashMap<>();

    // Listen for project open/close events.
    Application application = ApplicationManager.getApplication();
    application.getMessageBus().connect(application).subscribe(ProjectManager.TOPIC, this);
  }

  @Override
  public Set<HighlightInfoType> supportedHighlightInfoTypes() {
    return unresResourceStatsCollectorEnabled.getValue()
        ? SUPPORTED_HIGHLIGHT_TYPES
        : ImmutableSet.of();
  }

  @Override
  public boolean canProcessFile(PsiFile psiFile) {
    if (!unresResourceStatsCollectorEnabled.getValue()
        || !SUPPORTED_FILE_TYPES.contains(psiFile.getFileType())) {
      return false;
    }
    return getOrCreateCollector(psiFile).willProcessFile(psiFile);
  }

  @Override
  public boolean canProcessHighlight(
      PsiElement psiElement, com.intellij.codeInsight.daemon.impl.HighlightInfo highlightInfo) {
    return unresResourceStatsCollectorEnabled.getValue()
        && SUPPORTED_HIGHLIGHT_TYPES.contains(highlightInfo.type);
  }

  @Override
  public void processHighlight(PsiElement psiElement, HighlightInfo highlightInfo) {
    if (unresResourceStatsCollectorEnabled.getValue()) {
      getOrCreateCollector(psiElement.getContainingFile())
          .processHighlight(psiElement, highlightInfo);
    }
  }

  @Override
  public void onSyncStart(Project project, BlazeContext context, SyncMode syncMode) {
    if (unresResourceStatsCollectorEnabled.getValue()) {
      getOrCreateCollector(project).onSyncStart(syncMode);
    }
  }

  @Override
  public void onSyncComplete(
      Project project,
      BlazeContext context,
      BlazeImportSettings importSettings,
      ProjectViewSet projectViewSet,
      ImmutableSet<Integer> buildIds,
      BlazeProjectData blazeProjectData,
      SyncMode syncMode,
      SyncResult syncResult) {
    if (unresResourceStatsCollectorEnabled.getValue()) {
      getOrCreateCollector(project).onSyncComplete(syncMode, syncResult);
    }
  }

  @Override
  public void projectOpened(Project project) {
    if (unresResourceStatsCollectorEnabled.getValue()) {
      getOrCreateCollector(project);
    }
  }

  @Override
  public void projectClosing(Project project) {
    if (unresResourceStatsCollectorEnabled.getValue()) {
      ProjectUnresolvedResourceStatsCollector collector = projectToCollector.remove(project);
      if (collector != null) {
        collector.logStats();
      }
    }
  }

  private ProjectUnresolvedResourceStatsCollector getOrCreateCollector(PsiFile psiFile) {
    return getOrCreateCollector(psiFile.getProject());
  }

  private ProjectUnresolvedResourceStatsCollector getOrCreateCollector(Project project) {
    return projectToCollector.computeIfAbsent(
        project, ProjectUnresolvedResourceStatsCollector::new);
  }
}
