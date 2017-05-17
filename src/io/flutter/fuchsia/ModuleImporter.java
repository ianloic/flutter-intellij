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
import io.flutter.dart.DartPlugin;
import io.flutter.utils.FlutterModuleUtils;
import org.jdom.JDOMException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModuleImporter {
  private static final String DIR_KEY = "Fuchsia.Dir";
  private final Project project;
  private final String fuchsiaDir;
  private final GnRefsQuery gn;

  public ModuleImporter(Project project) {
    this.project = project;
    fuchsiaDir = project.getBasePath();
    // TODO: check that this is a valid fuchsia dir.
    gn = new GnRefsQuery(fuchsiaDir);
  }

  public void configureDartSdk() {
    final String dartSdkDir = fuchsiaDir + "/out/debug-x86-64/host_x64/dart-sdk";
    DartPlugin.ensureDartSdkConfigured(project, dartSdkDir);
  }

  private Set<String> labelsToDirs(List<String> labels) {
    return labels.stream()
      .filter(label -> !label.equals("//packages/gn:mkbootfs"))
      .filter(label -> !label.startsWith("//flutter"))
      .map(label -> label.replaceFirst(":.*", "").substring(2))
      .distinct()
      .collect(Collectors.toSet());
  }

  public void importModules() {
    configureDartSdk();

    gn.query("//dart:create_sdk(//build/toolchain:host_x64)", dartLabels -> {
      final Set<String> dartDirs = labelsToDirs(dartLabels);

      System.out.println("dartDirs: " + dartDirs.toString());

      gn.query("//flutter/lib/snapshot:generate_snapshot_bin", flutterLabels -> {
        final Set<String> flutterDirs = labelsToDirs(flutterLabels);

        System.out.println("flutterDirs: " + flutterDirs.toString());


        if (!dartDirs.containsAll(flutterDirs)) {
          System.err.println("All Flutter directories should be Dart directories but these are not:");
          final Set<String> tmp = new HashSet<>(flutterDirs);
          tmp.removeAll(dartDirs);
          System.err.println(tmp.toString());
        }

        makeModules(dartDirs, flutterDirs);
      });
    });
  }

  private Module makeModule(ModifiableModuleModel modmodmod, String dir) {
    final String fullPath = fuchsiaDir + "/" + dir;
    final String moduleName = dir.replaceAll("/+", "_");
    final String moduleFile = fuchsiaDir + "/.idea/_"+moduleName+".iml";

    final Module module = modmodmod.newModule(moduleFile, WebModuleType.WEB_MODULE);
    module.setOption(DIR_KEY, dir);
    DartPlugin.enableDartSdk(module);
    modmodmod.setModuleGroupPath(module, dir.split("/+"));
    final ModifiableRootModel root = ModuleRootManager.getInstance(module).getModifiableModel();
    final VirtualFile vdir = LocalFileSystem.getInstance().refreshAndFindFileByPath(fullPath);
    if (vdir == null) {
      System.out.println("failed to find: " + fullPath);
      return null;
    }
    root.addContentEntry(vdir);
    root.commit();

    DartPlugin.enableDartSdk(module);

    return module;
  }

  private void makeDartModule(ModifiableModuleModel modmodmod, String dir) {
    makeModule(modmodmod, dir);
  }

  private void makeFlutterModule(ModifiableModuleModel modmodmod, String dir) {
    final Module module = makeModule(modmodmod, dir);
    if (module != null) {
      FlutterModuleUtils.setFlutterModuleType(module);
    }
  }


  private void makeModules(Set<String> dartDirs, Set<String> flutterDirs) {
    // TODO: check that flutterDirs is a subset of
    final Application app = ApplicationManager.getApplication();
    app.invokeLater(() -> app.runWriteAction(() -> {
      final ModifiableModuleModel modmodmod = ModuleManager.getInstance(project).getModifiableModel();

      final Set<String> existingModuleDirs = Stream.of(modmodmod.getModules())
        .map(module -> module.getOptionValue(DIR_KEY))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

      System.out.println("making modules:");
      for (String dir : dartDirs) {
        if (existingModuleDirs.contains(dir)) {
          System.out.println("Exists: "+dir);
          continue;
        }
        if (flutterDirs.contains(dir)) {
          System.out.println("Flutter: "+dir);
          makeFlutterModule(modmodmod, dir);
        } else {
          System.out.println("Dart: "+dir);
          makeDartModule(modmodmod, dir);
        }
      }
      modmodmod.commit();
    }));
  }
}
