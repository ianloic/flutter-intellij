/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.fuchsia;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FuchsiaInitializer implements StartupActivity {
  @Override
  public void runActivity(@NotNull Project project) {
    if (FuchsiaModuleUtils.isFuchsiaProject(project)) {
      final Application app = ApplicationManager.getApplication();

      // TODO(ianloic): prompt before doing this?
      app.runWriteAction(() ->
                           FuchsiaModuleUtils.configureDartSdk(project));

      // TODO(ianloic): show progress if this is slow.

      FuchsiaModuleUtils.findNewDartAndFlutterDirs(project, (dartDirs, flutterDirs) -> {
        if (dartDirs.isEmpty() && flutterDirs.isEmpty()) {
          // Nothing new.
          return;
        }

        final StringBuilder content = new StringBuilder("New Dart and/or Flutter modules were found.\n");
        final List<String> allDirs = new ArrayList<>(dartDirs.size() + flutterDirs.size());
        allDirs.addAll(dartDirs);
        allDirs.addAll(flutterDirs);
        Collections.sort(allDirs);
        for (String dir : allDirs) {
          content.append(dir).append('\n');
        }
        content.append("Would you like to add them to this project?");

        final Notification notification = new Notification("Fuchsia Tree",
                                                           "New Modules Found", content.toString(),
                                                           NotificationType.INFORMATION);

        notification.addAction(new AnAction("Add") {
          @Override
          public void actionPerformed(AnActionEvent e) {
            notification.expire();

            app.runWriteAction(() -> {
              final FuchsiaModuleMaker maker = new FuchsiaModuleMaker(project);
              for (String dir : dartDirs) {
                maker.makeDartModule(dir);
              }
              for (String dir : flutterDirs) {
                maker.makeFlutterModule(dir);
              }
              maker.commit();
            });
          }
        });
        notification.addAction(new AnAction("Don't Add") {
          @Override
          public void actionPerformed(AnActionEvent e) {
            notification.expire();
          }
        });
        Notifications.Bus.notify(notification);
      });
    }
  }
}
