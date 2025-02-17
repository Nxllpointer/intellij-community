// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInspection;

import com.intellij.java.i18n.JavaI18nBundle;
import com.intellij.lang.properties.PropertiesImplUtil;
import com.intellij.lang.properties.PropertiesUtil;
import com.intellij.lang.properties.ResourceBundle;
import com.intellij.lang.properties.ResourceBundleImpl;
import com.intellij.lang.properties.customizeActions.DissociateResourceBundleAction;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UI;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

public final class SuspiciousLocalesLanguagesInspection extends LocalInspectionTool {
  private static final String ADDITIONAL_LANGUAGES_ATTR_NAME = "additionalLanguages";
  private final static Supplier<Set<String>> JAVA_LOCALES = NotNullLazyValue.softLazy(() -> {
    final Set<String> result = new HashSet<>();
    for (Locale locale : Locale.getAvailableLocales()) {
      result.add(locale.getLanguage());
    }
    return result;
  });

  private final List<String> myAdditionalLanguages = new ArrayList<>();

  @TestOnly
  void setAdditionalLanguages(List<String> additionalLanguages) {
    myAdditionalLanguages.clear();
    myAdditionalLanguages.addAll(additionalLanguages);
  }

  @Override
  public void readSettings(@NotNull Element node) throws InvalidDataException {
    final String rawLanguages = node.getAttributeValue(ADDITIONAL_LANGUAGES_ATTR_NAME);
    if (rawLanguages != null) {
      myAdditionalLanguages.clear();
      myAdditionalLanguages.addAll(StringUtil.split(rawLanguages, ","));
    }
  }

  @Override
  public void writeSettings(@NotNull Element node) throws WriteExternalException {
    if (!myAdditionalLanguages.isEmpty()) {
      List<String> uniqueLanguages = ContainerUtil.sorted(new HashSet<>(myAdditionalLanguages));
      final String locales = StringUtil.join(uniqueLanguages, ",");
      node.setAttribute(ADDITIONAL_LANGUAGES_ATTR_NAME, locales);
    }
  }

  @Override
  public @NotNull JComponent createOptionsPanel() {
    return new MyOptions().getComponent();
  }

  @Override
  public ProblemDescriptor @Nullable [] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    final PropertiesFile propertiesFile = PropertiesImplUtil.getPropertiesFile(file);
    if (propertiesFile == null) {
      return null;
    }
    final ResourceBundle resourceBundle = propertiesFile.getResourceBundle();
    final List<PropertiesFile> files = resourceBundle.getPropertiesFiles();
    if (!(resourceBundle instanceof ResourceBundleImpl) || files.size() < 2) {
      return null;
    }
    List<Locale> bundleLocales = ContainerUtil.mapNotNull(files, propertiesFile1 -> {
      final Locale locale = propertiesFile1.getLocale();
      return locale == PropertiesUtil.DEFAULT_LOCALE ? null : locale;
    });
    bundleLocales = ContainerUtil.filter(bundleLocales, locale -> !JAVA_LOCALES.get().contains(locale.getLanguage()) && !myAdditionalLanguages.contains(locale.getLanguage()));
    if (bundleLocales.isEmpty()) {
      return null;
    }
    final ProblemDescriptor descriptor = manager.createProblemDescriptor(file,
                                                                         JavaI18nBundle.message(
                                                                           "resource.bundle.contains.locales.with.suspicious.locale.languages.desciptor"),
                                                                         new DissociateResourceBundleQuickFix(resourceBundle),
                                                                         ProblemHighlightType.WEAK_WARNING,
                                                                         true);
    return new ProblemDescriptor[] {descriptor};
  }

  private static final class DissociateResourceBundleQuickFix implements LocalQuickFix {
    private final ResourceBundle myResourceBundle;

    private DissociateResourceBundleQuickFix(ResourceBundle bundle) {
      myResourceBundle = bundle;
    }

    @Override
    public @NotNull String getFamilyName() {
      return JavaI18nBundle.message("dissociate.resource.bundle.quick.fix.name");
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      DissociateResourceBundleAction.dissociate(Collections.singleton(myResourceBundle), project);
    }
  }

  private final class MyOptions {
    private final JBList<String> myAdditionalLocalesList;

    private MyOptions() {
      myAdditionalLocalesList = new JBList<>(new CollectionListModel<>(myAdditionalLanguages, true));
      myAdditionalLocalesList.setCellRenderer(new DefaultListCellRenderer());
    }

    public JPanel getComponent() {
      final JPanel panel = new JPanel(new BorderLayout());
      panel.add(
        ToolbarDecorator.createDecorator(myAdditionalLocalesList)
          .setAddAction(new AnActionButtonRunnable() {
            @Override
            public void run(AnActionButton button) {
              Messages.showInputDialog(panel, JavaI18nBundle.message("dissociate.resource.bundle.quick.fix.options.input.text"),
                                       JavaI18nBundle.message("dissociate.resource.bundle.quick.fix.options.input.title"), null, "", new InputValidator() {
                @Override
                public boolean checkInput(String inputString) {
                  return 1 < inputString.length() && inputString.length() < 9 && !myAdditionalLanguages.contains(inputString);
                }

                @Override
                public boolean canClose(String inputString) {
                  if (inputString != null) {
                    myAdditionalLanguages.add(inputString);
                    ((CollectionListModel<String>)myAdditionalLocalesList.getModel()).allContentsChanged();
                  }
                  return true;
                }
              });
            }
          })
          .setRemoveAction(new AnActionButtonRunnable() {
            @Override
            public void run(AnActionButton button) {
              final int index = myAdditionalLocalesList.getSelectedIndex();
              if (index > -1 && index < myAdditionalLanguages.size()) {
                myAdditionalLanguages.remove(index);
                ((CollectionListModel<String>)myAdditionalLocalesList.getModel()).allContentsChanged();
              }
            }
          })
          .setPreferredSize(new Dimension(-1, 100))
          .disableUpDownActions()
          .setToolbarPosition(ActionToolbarPosition.RIGHT)
          .createPanel());
      return UI.PanelFactory
        .panel(panel)
        .withLabel(JavaI18nBundle.message("dissociate.resource.bundle.quick.fix.options.label"))
        .moveLabelOnTop()
        .resizeY(true)
        .createPanel();
    }
  }
}
