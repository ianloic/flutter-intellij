/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions.fuchsia;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class CreateModulesAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = AnAction.getEventProject(e);
    System.out.println("project is: " + project.toString());
    System.out.println("base path is: " + project.getBasePath());
  }
}
