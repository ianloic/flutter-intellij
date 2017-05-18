/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.fuchsia;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.CharsetToolkit;

import java.util.*;
import java.util.function.Consumer;

public class Gn {
  private final String fuchsiaDir;

  public Gn(Project project) {
    this.fuchsiaDir = project.getBasePath();
  }

  public void refs(String target, Consumer<List<String>> callback) {
    final StringBuffer outBuffer = new StringBuffer();
    final GeneralCommandLine cmd = new GeneralCommandLine();
    cmd.setCharset(CharsetToolkit.UTF8_CHARSET);
    cmd.setExePath(fuchsiaDir + "/packages/gn/gn.py");
    // TODO(ianloic): determine the out directory from environment?
    cmd.addParameters("refs", fuchsiaDir + "/out/debug-x86-64/", target);

    final OSProcessHandler handler;
    try {
      handler = new OSProcessHandler(cmd);
      handler.addProcessListener(new ProcessAdapter() {
        @Override
        public void processTerminated(ProcessEvent event) {
          callback.accept(Arrays.asList(outBuffer.toString().split("[\\r\\n]+")));
        }

        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
          if (outputType == ProcessOutputTypes.STDOUT) {
            outBuffer.append(event.getText());
          }
        }
      });

      handler.startNotify();
    }
    catch (ExecutionException e1) {
      e1.printStackTrace();
      callback.accept(null);
    }
  }

}
