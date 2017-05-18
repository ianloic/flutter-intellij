/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.fuchsia;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import io.flutter.dart.DartPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class FuchsiaModuleUtils {
  public static final String DIR_KEY = "Fuchsia.Dir";

  private FuchsiaModuleUtils() {
  }

  public static boolean isFuchsiaProject(Project project) {
    return project.getBaseDir().findChild(".jiri_manifest") != null;
  }

  public static void configureDartSdk(Project project) {
    final String dartSdkDir = project.getBasePath() + "/out/debug-x86-64/host_x64/dart-sdk";
    DartPlugin.ensureDartSdkConfigured(project, dartSdkDir);
  }

  /**
   * Find directories containing Dart or Flutter modules that aren't yet configured in the project.
   * @param project the IntelliJ project
   * @param callback a callback that takes a set of Dart directories and a set of Flutter directories
   */
  public static void findNewDartAndFlutterDirs(Project project, BiConsumer<Set<String>, Set<String>> callback) {
    findDartAndFlutterDirs(project, (dartDirs, flutterDirs) -> {
      for(Module module : ModuleManager.getInstance(project).getModules()) {
        final String dir = module.getOptionValue(DIR_KEY);
        if (dir != null) {
          dartDirs.remove(dir);
          flutterDirs.remove(dir);
        }
      }
      callback.accept(dartDirs, flutterDirs);
    });
  }

  public static void findDartAndFlutterDirs(Project project, BiConsumer<Set<String>, Set<String>> callback) {
    final Gn gn = new Gn(project);
    gn.refs("//dart:create_sdk(//build/toolchain:host_x64)", dartLabels -> {
      final Set<String> dartDirs = labelsToDirs(dartLabels);

      gn.refs("//flutter/lib/snapshot:generate_snapshot_bin", flutterLabels -> {
        final Set<String> flutterDirs = labelsToDirs(flutterLabels);

        if (!dartDirs.containsAll(flutterDirs)) {
          System.err.println("All Flutter directories should be Dart directories but these are not:");
          final Set<String> tmp = new HashSet<>(flutterDirs);
          tmp.removeAll(dartDirs);
          System.err.println(tmp.toString());
        }

        dartDirs.removeAll(flutterDirs);

        callback.accept(dartDirs, flutterDirs);
      });
    });
  }

  private static Set<String> labelsToDirs(List<String> labels) {
    return labels.stream()
      .filter(label -> !label.equals("//packages/gn:mkbootfs"))
      .filter(label -> !label.startsWith("//flutter"))
      .map(label -> label.replaceFirst(":.*", "").substring(2))
      .distinct()
      .collect(Collectors.toSet());
  }
}
