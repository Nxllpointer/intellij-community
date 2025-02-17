// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.execution

import com.intellij.openapi.util.NlsSafe
import com.intellij.util.io.IdeUtilIoBundle

class WorkingDirectoryNotFoundException(val workDirectory: @NlsSafe String)
  : ExecutionException(IdeUtilIoBundle.message("run.configuration.error.working.directory.does.not.exist", workDirectory))