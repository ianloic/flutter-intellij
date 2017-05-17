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
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.JDOMException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class ModuleImporter {
  private final Project project;
  private StringBuffer gnBuffer;

  public ModuleImporter(Project project) {
    this.project = project;
  }

  public void importModules() {
    final String fuchsiaDir = project.getBasePath();

    final GeneralCommandLine cmd = new GeneralCommandLine();
    cmd.setCharset(CharsetToolkit.UTF8_CHARSET);
    cmd.setExePath(fuchsiaDir + "/packages/gn/gn.py");
    cmd.addParameters("refs", fuchsiaDir + "/out/debug-x86-64/", "//dart:create_sdk(//build/toolchain:host_x64)");

    System.out.println("running: " + cmd);
    final OSProcessHandler handler;
    try {
      gnBuffer = new StringBuffer();
      handler = new OSProcessHandler(cmd);
      handler.addProcessListener(new ProcessAdapter() {
        @Override
        public void processTerminated(ProcessEvent event) {
          processBuffer();
        }

        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
          if ( outputType != ProcessOutputTypes.STDOUT) {
            return;
          }
          gnBuffer.append(event.getText());
          System.out.println("got: " + event.getText());
          System.out.println("on: " + outputType);
        }
      });

      handler.startNotify();
    }
    catch (ExecutionException e1) {
      e1.printStackTrace();
    }

  }

  private void processBuffer() {
    final String[] lines = gnBuffer.toString().split("[\\r\\n]+");
    System.out.println("have " + lines.length +" lines");
    final Set<String> dirs = new HashSet<String>();
    for (String line : lines) {
      if (line.equals("//packages/gn:mkbootfs")) {
        continue;
      }
      dirs.add(line.replaceFirst(":.*", "").substring(2));
    }
    System.out.println("have " + dirs.size() + " dirs");

    makeModules(dirs);
  }

  private void makeModules(Collection<String> dirs) {
    final Application app = ApplicationManager.getApplication();
    app.invokeLater(new Runnable() {
      @Override
      public void run() {
        app.runWriteAction(new Runnable() {
          @Override
          public void run() {
            final String fuchsiaDir = project.getBasePath();
            final ModifiableModuleModel modmodmod = ModuleManager.getInstance(project).getModifiableModel();

            System.out.println("current modules:");
            for (Module m : modmodmod.getModules()) {
              System.out.println(m.getModuleFilePath());
            }
            System.out.println("making modules:");
            for (String dir : dirs) {
              final String fullPath = fuchsiaDir + "/" + dir;
              final String moduleName = dir.replaceAll("/+", "_");
              final String moduleFile = fuchsiaDir + "/.idea/_"+moduleName+".iml";
              System.out.println(moduleFile);

              final Module module = modmodmod.newModule(moduleFile, WebModuleType.WEB_MODULE);
              modmodmod.setModuleGroupPath(module, dir.split("/+"));
              final ModifiableRootModel root = ModuleRootManager.getInstance(module).getModifiableModel();
              final VirtualFile vdir = LocalFileSystem.getInstance().refreshAndFindFileByPath(fullPath);
              root.addContentEntry(vdir);
              root.commit();
              System.out.println("made module: "+module);
            }
            modmodmod.commit();
          }
        });
      }
    });
  }
}
