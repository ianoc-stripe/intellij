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
package com.google.idea.blaze.base.logging.utils;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.idea.blaze.base.sync.SyncMode;
import com.google.idea.blaze.base.sync.SyncResult;
import com.intellij.psi.PsiFile;
import java.util.List;
import java.util.Map;

/** Highlight Stats to be logged per project */
@AutoValue
public abstract class HighlightStats {
  public abstract Type type();

  public abstract SyncMode lastSyncMode();

  public abstract SyncResult lastSyncResult();

  public abstract ImmutableMap<PsiFile, List<HighlightInfo>> fileToHighLights();

  public static Builder builder() {
    return new AutoValue_HighlightStats.Builder();
  }

  /** Builder for {@link HighlightStats} */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setType(Type type);

    public abstract Builder setLastSyncMode(SyncMode syncMode);

    public abstract Builder setLastSyncResult(SyncResult syncResult);

    public abstract Builder setFileToHighLights(Map<PsiFile, List<HighlightInfo>> fileToHighlights);

    public abstract HighlightStats build();
  }

  /** Types of {@link HighlightStats} */
  public enum Type {
    ANDROID_RESOURCE_MISSING_REF
  }
}
