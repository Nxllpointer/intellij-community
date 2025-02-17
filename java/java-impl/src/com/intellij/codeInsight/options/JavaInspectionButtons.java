// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInsight.options;

import com.intellij.codeInsight.NullableNotNullDialog;
import com.intellij.codeInspection.ex.EntryPointsManagerImpl;
import com.intellij.codeInspection.ui.CustomComponentExtensionWithSwingRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class JavaInspectionButtons extends CustomComponentExtensionWithSwingRenderer<JavaInspectionButtons.ButtonKind> {
  public JavaInspectionButtons() {
    super("java.option.buttons");
  }

  @Override
  public @NotNull JComponent render(ButtonKind data, @Nullable Component parent) {
    return switch (data) {
      case NULLABILITY_ANNOTATIONS -> NullableNotNullDialog.createConfigureAnnotationsButton(parent);
      case ENTRY_POINT_CODE_PATTERNS -> EntryPointsManagerImpl.createConfigureClassPatternsButton();
      case ENTRY_POINT_ANNOTATIONS -> EntryPointsManagerImpl.createConfigureAnnotationsButton();
    };
  }

  @Override
  public @NotNull String serializeData(ButtonKind kind) {
    return kind.name();
  }

  @Override
  public ButtonKind deserializeData(@NotNull String data) {
    return ButtonKind.valueOf(data);
  }

  public enum ButtonKind {
    NULLABILITY_ANNOTATIONS,
    ENTRY_POINT_CODE_PATTERNS,
    ENTRY_POINT_ANNOTATIONS
  }
}
