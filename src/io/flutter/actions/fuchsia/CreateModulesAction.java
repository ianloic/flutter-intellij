/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions.fuchsia;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.CharsetToolkit;
import io.flutter.fuchsia.ModuleImporter;

public class CreateModulesAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = AnAction.getEventProject(e);
    System.out.println("project is: " + project.toString());
    System.out.println("base path is: " + project.getBasePath());

    ModuleImporter mi = new ModuleImporter(project);
    mi.importModules();
  }
}
