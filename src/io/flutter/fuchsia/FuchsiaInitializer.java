/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.fuchsia;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class FuchsiaInitializer implements StartupActivity {
  @Override
  public void runActivity(@NotNull Project project) {
    VirtualFile projectRoot = project.getBaseDir();
    VirtualFile jiriManifest = projectRoot.findChild(".jiri_manifest");
    if (jiriManifest != null) {
      System.out.println("This looks like a Fuchsia source tree");
    }
  }
}
