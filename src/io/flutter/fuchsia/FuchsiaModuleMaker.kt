/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package io.flutter.fuchsia

import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.WebModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import io.flutter.dart.DartPlugin
import io.flutter.utils.FlutterModuleUtils

class FuchsiaModuleMaker(private val project: Project) {
  private val model: ModifiableModuleModel = ModuleManager.getInstance(project).modifiableModel

  private fun makeModule(dir: String): Module? {
    val moduleName = dir.replace("/+".toRegex(), "_")
    val moduleFile = "${project.basePath}/.idea/_$moduleName.iml"

    val module = model.newModule(moduleFile, WebModuleType.WEB_MODULE)
    module.setOption(FuchsiaModuleUtils.DIR_KEY, dir)
    DartPlugin.enableDartSdk(module)
    model.setModuleGroupPath(module, dir.split("/+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    val root = ModuleRootManager.getInstance(module).modifiableModel
    val vdir = project.baseDir.findFileByRelativePath(dir)
    if (vdir == null) {
      println("failed to find: $dir")
      return null
    }
    root.addContentEntry(vdir)
    root.commit()

    DartPlugin.enableDartSdk(module)

    return module
  }

  fun makeDartModule(dir: String) {
    makeModule(dir)
  }

  fun makeFlutterModule(dir: String) {
    val module = makeModule(dir)
    if (module != null) {
      FlutterModuleUtils.setFlutterModuleType(module)
    }
  }

  fun commit() = model.commit()
}
