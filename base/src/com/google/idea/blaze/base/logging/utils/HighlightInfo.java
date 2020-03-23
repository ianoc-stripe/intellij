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
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.lang.annotation.HighlightSeverity;

/** Highlight Information. See {@link HighlightStats} for more information */
@AutoValue
public abstract class HighlightInfo {
  public abstract String text();

  public abstract HighlightSeverity severity();

  public abstract HighlightInfoType type();

  public abstract int startOffset();

  public abstract int endOffset();

  public static Builder builder() {
    return new AutoValue_HighlightInfo.Builder();
  }

  /** Builder for {@link HighlightInfo} */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setText(String value);

    public abstract Builder setSeverity(HighlightSeverity value);

    public abstract Builder setType(HighlightInfoType type);

    public abstract Builder setStartOffset(int value);

    public abstract Builder setEndOffset(int value);

    public abstract HighlightInfo build();
  }
}
