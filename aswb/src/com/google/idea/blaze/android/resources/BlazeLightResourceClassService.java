/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.android.resources;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.android.tools.idea.projectsystem.LightResourceClassService;
import com.android.tools.idea.res.AndroidLightPackage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.idea.blaze.base.sync.data.BlazeDataStorage;
import com.google.idea.common.experiments.BoolExperiment;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Implementation of {@link LightResourceClassService} set up at Blaze sync time. */
public class BlazeLightResourceClassService implements LightResourceClassService {

  private static final BoolExperiment WORKSPACE_RESOURCES_ENABLED =
      new BoolExperiment("blaze.workspace.light.class.enabled", true);
  private static final BoolExperiment CREATE_STUB_RESOURCE_PACKAGES =
      new BoolExperiment("create.stub.resource.packages", true);

  @NotNull private Map<String, BlazeRClass> rClasses = Maps.newHashMap();
  @NotNull private Map<String, PsiPackage> rClassPackages = Maps.newHashMap();
  @NotNull private Map<String, BlazeRClass> workspaceRClasses = Maps.newHashMap();
  @NotNull private Set<String> workspaceRPackages = ImmutableSet.of();

  private RClassesCache allRClassesCache;

  private Module workspaceModule;
  private PsiManager psiManager;

  public static BlazeLightResourceClassService getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, BlazeLightResourceClassService.class);
  }

  /** Builds light R classes */
  public static class Builder {
    Map<String, BlazeRClass> rClassMap = Maps.newHashMap();
    Map<String, PsiPackage> rClassPackages = Maps.newHashMap();
    Set<String> workspacePackages = ImmutableSet.of();
    Module workspaceModule;

    PsiManager psiManager;

    public Builder(Project project) {
      this.psiManager = PsiManager.getInstance(project);
    }

    public void addRClass(String resourceJavaPackage, Module module) {
      BlazeRClass rClass = new BlazeRClass(psiManager, module, resourceJavaPackage);
      rClassMap.put(getQualifiedRClassName(resourceJavaPackage), rClass);
      if (CREATE_STUB_RESOURCE_PACKAGES.getValue()) {
        addStubPackages(resourceJavaPackage);
      }
    }

    public void addWorkspacePackages(Set<String> resourceJavaPackages, Module workspaceModule) {
      if (!WORKSPACE_RESOURCES_ENABLED.getValue()) {
        return;
      }
      this.workspaceModule = workspaceModule;
      this.workspacePackages =
          resourceJavaPackages.stream()
              .map(Builder::getQualifiedRClassName)
              .collect(toImmutableSet());
      resourceJavaPackages.forEach(this::addStubPackages);
    }

    @NotNull
    private static String getQualifiedRClassName(@NotNull String packageName) {
      return packageName + ".R";
    }

    private void addStubPackages(String resourceJavaPackage) {
      while (!resourceJavaPackage.isEmpty()) {
        if (rClassPackages.containsKey(resourceJavaPackage)) {
          return;
        }
        rClassPackages.put(
            resourceJavaPackage,
            AndroidLightPackage.withName(resourceJavaPackage, psiManager.getProject()));
        int nextIndex = resourceJavaPackage.lastIndexOf('.');
        if (nextIndex < 0) {
          return;
        }
        resourceJavaPackage = resourceJavaPackage.substring(0, nextIndex);
      }
    }
  }

  public void installRClasses(Builder builder) {
    this.rClasses = builder.rClassMap;
    this.rClassPackages = builder.rClassPackages;

    this.workspaceRClasses = new HashMap<>();
    this.workspaceRPackages = ImmutableSet.copyOf(builder.workspacePackages);
    this.workspaceModule = builder.workspaceModule;
    this.psiManager = builder.psiManager;

    this.allRClassesCache = new RClassesCache(rClasses);
  }

  @Override
  @NotNull
  public Collection<? extends PsiClass> getLightRClasses(
      @NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
    BlazeRClass rClass = this.rClasses.get(qualifiedName);

    if (rClass == null && WORKSPACE_RESOURCES_ENABLED.getValue()) {
      rClass = this.workspaceRClasses.get(qualifiedName);

      if (rClass == null && workspaceRPackages.contains(qualifiedName)) {
        // Remove .R suffix
        String packageName = qualifiedName.substring(0, qualifiedName.length() - 2);
        rClass = new BlazeRClass(psiManager, workspaceModule, packageName);
        workspaceRClasses.put(qualifiedName, rClass);
        // notify the cache that workspace classes have been updated.
        allRClassesCache.notifyClassesUpdated();
      }
    }

    if (rClass != null && scope.isSearchInModuleContent(rClass.getModule())) {
      return ImmutableList.of(rClass);
    }
    return ImmutableList.of();
  }

  @NotNull
  @Override
  public Collection<? extends PsiClass> getLightRClassesAccessibleFromModule(
      @NotNull Module module, boolean includeTest) {
    if (WORKSPACE_RESOURCES_ENABLED.getValue()
        && module.getName().equals(BlazeDataStorage.WORKSPACE_MODULE_NAME)) {
      return allRClassesCache.getAllRClasses(workspaceRClasses);
    } else {
      return rClasses.values();
    }
  }

  @NotNull
  @Override
  public Collection<? extends PsiClass> getLightRClassesContainingModuleResources(
      @NotNull Module module) {
    return rClasses.values();
  }

  @Override
  @Nullable
  public PsiPackage findRClassPackage(@NotNull String qualifiedName) {
    return rClassPackages.get(qualifiedName);
  }

  @Override
  @NotNull
  public Collection<? extends PsiClass> getAllLightRClasses() {
    return allRClassesCache.getAllRClasses(workspaceRClasses);
  }

  /**
   * Class to store all seen R classes. Intellij calls {@link
   * LightResourceClassService#getAllLightRClasses} and {@link
   * LightResourceClassService#getLightRClassesAccessibleFromModule} multiple times per keystroke.
   * So we want to minimize the number of re-computations as much as possible.
   *
   * <p>Reverts to a single calculation when experiment {@link
   * BlazeLightResourceClassService#WORKSPACE_RESOURCES_ENABLED} is disabled.
   */
  private static class RClassesCache {
    private Set<BlazeRClass> cache;
    private boolean isStale;

    private Collection<BlazeRClass> nonWorkspaceRClasses;

    RClassesCache(Map<String, BlazeRClass> nonWorkspaceRClassMap) {
      this.nonWorkspaceRClasses = nonWorkspaceRClassMap.values();
      // Initialize the cache with nonWorkspaceRClasses.
      updateCache(ImmutableMap.of());
    }

    void notifyClassesUpdated() {
      // Do not mark stale if the experiment is disabled. This effectively means there would be no
      // re-computations.
      isStale = WORKSPACE_RESOURCES_ENABLED.getValue();
    }

    Set<BlazeRClass> getAllRClasses(Map<String, BlazeRClass> workspaceRClassesMap) {
      if (isStale) {
        updateCache(workspaceRClassesMap);
      }
      return cache;
    }

    private void updateCache(Map<String, BlazeRClass> workspaceRClassesMap) {
      Stream<BlazeRClass> workspaceRClasses = workspaceRClassesMap.values().stream();
      cache =
          Stream.concat(nonWorkspaceRClasses.stream(), workspaceRClasses)
              .collect(Collectors.toSet());
      isStale = false;
    }
  }
}
