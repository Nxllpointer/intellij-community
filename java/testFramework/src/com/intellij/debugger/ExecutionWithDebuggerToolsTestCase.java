// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.debugger;

import com.intellij.debugger.engine.*;
import com.intellij.debugger.engine.evaluation.CodeFragmentKind;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.debugger.impl.PositionUtil;
import com.intellij.debugger.impl.PrioritizedTask;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.debugger.ui.breakpoints.ExceptionBreakpoint;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionTestCase;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.ThreadTracker;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.util.SmartList;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.lang.CompoundRuntimeException;
import com.intellij.util.ui.UIUtil;
import com.sun.jdi.Method;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaMethodBreakpointProperties;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Runs the IDE with the debugger.
 * <p>
 * To set a breakpoint in the source code,
 * place a 'Breakpoint!' comment in the line directly above,
 * see {@link #createBreakpoints} for details.
 */
public abstract class ExecutionWithDebuggerToolsTestCase extends ExecutionTestCase {
  private @Nullable BreakpointProvider myBreakpointProvider;
  protected static final int RATHER_LATER_INVOKES_N = 10;
  public DebugProcessImpl myDebugProcess;
  private final List<Throwable> myException = new SmartList<>();

  private static class InvokeRatherLaterRequest {
    private final DebuggerCommandImpl myDebuggerCommand;
    private final DebugProcessImpl myDebugProcess;
    int invokesN;

    InvokeRatherLaterRequest(DebuggerCommandImpl debuggerCommand, DebugProcessImpl debugProcess) {
      myDebuggerCommand = debuggerCommand;
      myDebugProcess = debugProcess;
    }
  }

  public final List<InvokeRatherLaterRequest> myRatherLaterRequests = new ArrayList<>();

  protected DebugProcessImpl getDebugProcess() {
    return myDebugProcess;
  }

  protected String readValue(String comment, String valueName) {
    int valueStart = comment.indexOf(valueName);
    if (valueStart == -1) {
      return null;
    }

    valueStart += valueName.length();
    return comment.substring(valueStart + 1, findMatchingParenthesis(comment, valueStart));
  }

  private static int findMatchingParenthesis(String input, int startPos) {
    int depth = 0;
    while (startPos < input.length()) {
      switch (input.charAt(startPos)) {
        case '(' -> depth++;
        case ')' -> {
          if (depth == 1) {
            return startPos;
          }
          else {
            depth--;
          }
        }
      }
      startPos++;
    }
    return -1;
  }

  protected void resume(SuspendContextImpl context) {
    DebugProcessImpl debugProcess = context.getDebugProcess();
    debugProcess.getManagerThread().schedule(debugProcess.createResumeCommand(context, PrioritizedTask.Priority.LOWEST));
  }

  protected void stepInto(SuspendContextImpl context) {
    stepInto(context, false);
  }

  protected void stepInto(SuspendContextImpl context, boolean ignoreFilters) {
    DebugProcessImpl debugProcess = context.getDebugProcess();
    debugProcess.getManagerThread().schedule(debugProcess.createStepIntoCommand(context, ignoreFilters, null));
  }

  protected void stepOver(SuspendContextImpl context) {
    DebugProcessImpl debugProcess = context.getDebugProcess();
    debugProcess.getManagerThread().schedule(debugProcess.createStepOverCommand(context, false));
  }

  protected void stepOut(SuspendContextImpl context) {
    DebugProcessImpl debugProcess = context.getDebugProcess();
    debugProcess.getManagerThread().schedule(debugProcess.createStepOutCommand(context));
  }

  @Override
  protected void tearDown() throws Exception {
    ThreadTracker.awaitJDIThreadsTermination(100, TimeUnit.SECONDS);
    try {
      myDebugProcess = null;
      myBreakpointProvider = null;
      myRatherLaterRequests.clear();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
      throwExceptionsIfAny();
    }
  }

  protected void throwExceptionsIfAny() {
    synchronized (myException) {
      CompoundRuntimeException.throwIfNotEmpty(myException);
      myException.clear();
    }
  }

  private @NotNull BreakpointProvider getBreakpointProvider() {
    if (myBreakpointProvider == null) {
      final DebugProcessImpl debugProcess = getDebugProcess();
      assertNotNull("Debug process was not started", debugProcess);

      myBreakpointProvider = new BreakpointProvider(myDebugProcess);
      DebugProcessListener processListener = new DelayedEventsProcessListener(myBreakpointProvider);
      debugProcess.addDebugProcessListener(processListener, getTestRootDisposable());
    }
    return myBreakpointProvider;
  }

  protected void onBreakpoint(SuspendContextRunnable runnable) {
    getBreakpointProvider().onBreakpoint(runnable);
  }

  protected void onBreakpoints(SuspendContextRunnable runnable) {
    getBreakpointProvider().onBreakpoints(runnable);
  }

  protected void onStop(final SuspendContextRunnable runnable, final SuspendContextRunnable then) {
    onBreakpoint(new SuspendContextRunnable() {
      @Override
      public void run(SuspendContextImpl suspendContext) throws Exception {
        try {
          runnable.run(suspendContext);
        }
        finally {
          then.run(suspendContext);
        }
      }
    });
  }

  protected void doWhenPausedThenResume(final SuspendContextRunnable runnable) {
    onStop(runnable, this::resume);
  }

  protected void printFrameProxy(StackFrameProxyImpl frameProxy) throws EvaluateException {
    int frameIndex = frameProxy.getFrameIndex();
    Method method = frameProxy.location().method();

    println("frameProxy(" + frameIndex + ") = " + method, ProcessOutputTypes.SYSTEM);
  }

  protected static String toDisplayableString(SourcePosition sourcePosition) {
    int line = sourcePosition.getLine();
    if (line >= 0) {
      line++;
    }
    return sourcePosition.getFile().getVirtualFile().getName() + ":" + line;
  }

  protected void printContext(final StackFrameContext context) {
    ApplicationManager.getApplication().runReadAction(() -> {
      if (context.getFrameProxy() != null) {
        println(toDisplayableString(Objects.requireNonNull(PositionUtil.getSourcePosition(context))), ProcessOutputTypes.SYSTEM);
      }
      else {
        println("Context thread is null", ProcessOutputTypes.SYSTEM);
      }
    });
  }

  protected void printContextWithText(final StackFrameContext context) {
    ApplicationManager.getApplication().runReadAction(() -> {
      if (context.getFrameProxy() != null) {
        SourcePosition sourcePosition = PositionUtil.getSourcePosition(context);
        int offset = sourcePosition.getOffset();
        Document document = PsiDocumentManager.getInstance(myProject).getDocument(sourcePosition.getFile());
        CharSequence text = Objects.requireNonNull(document).getImmutableCharSequence();
        String positionText = "";
        if (offset > -1) {
          positionText = StringUtil.escapeLineBreak(" [" + text.subSequence(Math.max(0, offset - 20), offset) + "<*>"
          + text.subSequence(offset, Math.min(offset + 20, text.length())) + "]");
        }

        println(toDisplayableString(sourcePosition) + positionText, ProcessOutputTypes.SYSTEM);
      }
      else {
        println("Context thread is null", ProcessOutputTypes.SYSTEM);
      }
    });
  }

  protected void invokeRatherLater(SuspendContextImpl context, final Runnable runnable) {
    invokeRatherLater(new SuspendContextCommandImpl(context) {
      @Override
      public void contextAction(@NotNull SuspendContextImpl suspendContext) {
        DebuggerInvocationUtil.invokeLater(myProject, runnable);
      }
    });
  }

  protected void pumpSwingThread() {
    LOG.assertTrue(SwingUtilities.isEventDispatchThread());

    final InvokeRatherLaterRequest request = myRatherLaterRequests.get(0);
    request.invokesN++;

    if (request.invokesN == RATHER_LATER_INVOKES_N) {
      myRatherLaterRequests.remove(0);
      if (!myRatherLaterRequests.isEmpty()) pumpSwingThread();
    }

    if (request.myDebuggerCommand instanceof SuspendContextCommandImpl) {
      request.myDebugProcess.getManagerThread().schedule(new SuspendContextCommandImpl(
          ((SuspendContextCommandImpl)request.myDebuggerCommand).getSuspendContext()) {
          @Override
          public void contextAction(@NotNull SuspendContextImpl suspendContext) {
            pumpDebuggerThread(request);
          }

          @Override
          protected void commandCancelled() {
            pumpDebuggerThread(request);
          }
        });
    }
    else {
      request.myDebugProcess.getManagerThread().schedule(new DebuggerCommandImpl() {
          @Override
          protected void action() {
            pumpDebuggerThread(request);
          }

          @Override
          protected void commandCancelled() {
            pumpDebuggerThread(request);
          }
        });
    }
  }

  private void pumpDebuggerThread(final InvokeRatherLaterRequest request) {
    if (request.invokesN == RATHER_LATER_INVOKES_N) {
      request.myDebugProcess.getManagerThread().schedule(request.myDebuggerCommand);
    }
    else {
      if (!SwingUtilities.isEventDispatchThread()) {
        UIUtil.invokeAndWaitIfNeeded((Runnable)() -> pumpSwingThread());
      }
      else {
        SwingUtilities.invokeLater(() -> pumpSwingThread());
      }
    }
  }

  protected void invokeRatherLater(final DebuggerCommandImpl command) {
    invokeRatherLater(getDebugProcess(), command);
  }

  protected void invokeRatherLater(@NotNull DebugProcessImpl debugProcess, DebuggerCommandImpl command) {
    UIUtil.invokeLaterIfNeeded(() -> {
      InvokeRatherLaterRequest request = new InvokeRatherLaterRequest(command, debugProcess);
      myRatherLaterRequests.add(request);

      if (myRatherLaterRequests.size() == 1) pumpSwingThread();
    });
  }

  protected void addException(@NotNull Throwable e) {
    synchronized (myException) {
      myException.add(e);
    }
  }

  protected void error(@NotNull Throwable th) {
    fail(StringUtil.getThrowableText(th));
  }

  private static Pair<ClassFilter[], ClassFilter[]> readClassFilters(String filtersString) {
    ArrayList<ClassFilter> include = new ArrayList<>();
    ArrayList<ClassFilter> exclude = new ArrayList<>();
    for (String s : filtersString.split(",")) {
      ClassFilter filter = new ClassFilter();
      filter.setEnabled(true);
      if (s.startsWith("-")) {
        exclude.add(filter);
        s = s.substring(1);
      } else {
        include.add(filter);
      }
      filter.setPattern(s);
    }
    return Pair.create(include.toArray(ClassFilter.EMPTY_ARRAY), exclude.toArray(ClassFilter.EMPTY_ARRAY));
  }

  /**
   * A breakpoint comment has the form &#x201C;[<i>kind</i>] Breakpoint! [<i>property</i>...]&#x201D;.
   * <p>
   * Breakpoint kinds (none defaults to a line breakpoint):
   * <ul>
   * <li>Method
   * <li>Field(fieldName)
   * <li>Exception(java.lang.RuntimeException)
   * </ul>
   * Properties for all breakpoints:
   * <ul>
   * <li>Class filters(-MethodClassFilter$A)
   * <li>Condition(i >= 10 || (i % 5 == 0))
   * <li>LogExpression("i = " + i)
   * <li>Pass count(13)
   * <li>suspendPolicy(SuspendAll | SuspendThread | SuspendNone)
   * </ul>
   * Properties for method breakpoints:
   * <ul>
   * <li>Emulated(true)
   * <li>OnEntry(true)
   * <li>OnExit(true)
   * </ul>
   * Properties for exception breakpoints:
   * <ul>
   * <li>Catch class filters(-ExceptionTest,-com.intellij.rt.*)
   * </ul>
   */
  public void createBreakpoints(final PsiFile file) {
    Runnable runnable = () -> {
      BreakpointManager breakpointManager = DebuggerManagerEx.getInstanceEx(myProject).getBreakpointManager();
      Document document = PsiDocumentManager.getInstance(myProject).getDocument(file);
      String text = document.getText();
      int offset = -1;
      while (true) {
        offset = text.indexOf("Breakpoint!", offset + 1);
        if (offset == -1) break;

        int commentLine = document.getLineNumber(offset);

        String comment = text.substring(document.getLineStartOffset(commentLine), document.getLineEndOffset(commentLine));

        Breakpoint breakpoint;

        if (comment.contains("Method")) {
          breakpoint = breakpointManager.addMethodBreakpoint(document, commentLine + 1);
          if (breakpoint != null) {
            println("MethodBreakpoint created at " + file.getVirtualFile().getName() + ":" + (commentLine + 2),
                    ProcessOutputTypes.SYSTEM);

            String emulated = readValue(comment, "Emulated");
            if (emulated != null) {
              ((JavaMethodBreakpointProperties)breakpoint.getXBreakpoint().getProperties()).EMULATED = Boolean.parseBoolean(emulated);
              println("Emulated = " + emulated, ProcessOutputTypes.SYSTEM);
            }

            String entry = readValue(comment, "OnEntry");
            if (entry != null) {
              ((JavaMethodBreakpointProperties)breakpoint.getXBreakpoint().getProperties()).WATCH_ENTRY = Boolean.parseBoolean(entry);
              println("On Entry = " + entry, ProcessOutputTypes.SYSTEM);
            }

            String exit = readValue(comment, "OnExit");
            if (exit != null) {
              ((JavaMethodBreakpointProperties)breakpoint.getXBreakpoint().getProperties()).WATCH_EXIT = Boolean.parseBoolean(exit);
              println("On Exit = " + exit, ProcessOutputTypes.SYSTEM);
            }
          }
        }
        else if (comment.contains("Field")) {
          breakpoint = breakpointManager.addFieldBreakpoint(document, commentLine + 1, readValue(comment, "Field"));
          if (breakpoint != null) {
            println("FieldBreakpoint created at " + file.getVirtualFile().getName() + ":" + (commentLine + 2), ProcessOutputTypes.SYSTEM);
          }
        }
        else if (comment.contains("Exception")) {
          breakpoint = breakpointManager.addExceptionBreakpoint(readValue(comment, "Exception"), "");
          if (breakpoint != null) {
            println("ExceptionBreakpoint created at " + file.getVirtualFile().getName() + ":" + (commentLine + 2),
                    ProcessOutputTypes.SYSTEM);
          }
        }
        else {
          breakpoint = breakpointManager.addLineBreakpoint(document, commentLine + 1);
          if (breakpoint != null) {
            println("LineBreakpoint created at " + file.getVirtualFile().getName() + ":" + (commentLine + 2), ProcessOutputTypes.SYSTEM);
          }
        }

        if (breakpoint == null) {
          LOG.error("Unable to set a breakpoint at line " + (commentLine + 1));
          continue;
        }

        String suspendPolicy = readValue(comment, "suspendPolicy");
        if (suspendPolicy != null) {
          //breakpoint.setSuspend(!DebuggerSettings.SUSPEND_NONE.equals(suspendPolicy));
          breakpoint.setSuspendPolicy(suspendPolicy);
          println("SUSPEND_POLICY = " + suspendPolicy, ProcessOutputTypes.SYSTEM);
        }
        String condition = readValue(comment, "Condition");
        if (condition != null) {
          //breakpoint.CONDITION_ENABLED = true;
          breakpoint.setCondition(new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, condition));
          println("Condition = " + condition, ProcessOutputTypes.SYSTEM);
        }

        String logExpression = readValue(comment, "LogExpression");
        if (logExpression != null) {
          breakpoint.getXBreakpoint().setLogExpression(logExpression);
          println("LogExpression = " + logExpression, ProcessOutputTypes.SYSTEM);
        }

        String passCount = readValue(comment, "Pass count");
        if (passCount != null) {
          breakpoint.setCountFilterEnabled(true);
          breakpoint.setCountFilter(Integer.parseInt(passCount));
          println("Pass count = " + passCount, ProcessOutputTypes.SYSTEM);
        }

        String classFilters = readValue(comment, "Class filters");
        if (classFilters != null) {
          breakpoint.setClassFiltersEnabled(true);
          Pair<ClassFilter[], ClassFilter[]> filters = readClassFilters(classFilters);
          breakpoint.setClassFilters(filters.first);
          breakpoint.setClassExclusionFilters(filters.second);
          println("Class filters = " + classFilters, ProcessOutputTypes.SYSTEM);
        }

        String catchClassFilters = readValue(comment, "Catch class filters");
        if (catchClassFilters != null && breakpoint instanceof ExceptionBreakpoint) {
          ExceptionBreakpoint exceptionBreakpoint = (ExceptionBreakpoint)breakpoint;
          exceptionBreakpoint.setCatchFiltersEnabled(true);
          Pair<ClassFilter[], ClassFilter[]> filters = readClassFilters(catchClassFilters);
          exceptionBreakpoint.setCatchClassFilters(filters.first);
          exceptionBreakpoint.setCatchClassExclusionFilters(filters.second);
          println("Catch class filters = " + catchClassFilters, ProcessOutputTypes.SYSTEM);
        }
      }
    };
    if (!SwingUtilities.isEventDispatchThread()) {
      DebuggerInvocationUtil.invokeAndWait(myProject, runnable, ModalityState.defaultModalityState());
    }
    else {
      runnable.run();
    }
  }

  protected static class DelayedEventsProcessListener implements DebugProcessListener {
    private final DebugProcessAdapterImpl myTarget;

    public DelayedEventsProcessListener(DebugProcessAdapterImpl target) {
      myTarget = target;
    }

    @Override
    public void paused(@NotNull final SuspendContext suspendContext) {
      pauseExecution();
      myTarget.paused(suspendContext);
    }

    @Override
    public void resumed(final SuspendContext suspendContext) {
      pauseExecution();
      myTarget.resumed(suspendContext);
    }

    @Override
    public void processDetached(@NotNull final DebugProcess process, final boolean closedByUser) {
      myTarget.processDetached(process, closedByUser);
    }

    @Override
    public void processAttached(@NotNull final DebugProcess process) {
      myTarget.processAttached(process);
    }

    @Override
    public void connectorIsReady() {
      myTarget.connectorIsReady();
    }

    @Override
    public void attachException(final RunProfileState state, final ExecutionException exception, final RemoteConnection remoteConnection) {
      myTarget.attachException(state, exception, remoteConnection);
    }

    private static void pauseExecution() {
      TimeoutUtil.sleep(10);
    }
  }

  protected class BreakpointProvider extends DebugProcessAdapterImpl {
    private final DebugProcessImpl myDebugProcess;
    private final List<SuspendContextRunnable> myBreakpointListeners = new ArrayList<>();
    private final Queue<SuspendContextRunnable> myScriptRunnables = new ArrayDeque<>();

    public BreakpointProvider(DebugProcessImpl debugProcess) {
      myDebugProcess = debugProcess;
    }

    public void onBreakpoint(SuspendContextRunnable runnable) {
      myScriptRunnables.add(runnable);
    }

    public void onBreakpoints(SuspendContextRunnable runnable) {
      myBreakpointListeners.add(runnable);
    }

    @Override
    public void paused(SuspendContextImpl suspendContext) {
      try {
        if (myScriptRunnables.isEmpty() && myBreakpointListeners.isEmpty()) {
          print("resuming ", ProcessOutputTypes.SYSTEM);
          printContext(suspendContext);
          resume(suspendContext);
          return;
        }
        SuspendContextRunnable suspendContextRunnable = myScriptRunnables.poll();
        if (suspendContextRunnable != null) {
          suspendContextRunnable.run(suspendContext);
        }
        for (SuspendContextRunnable it : myBreakpointListeners) {
          it.run(suspendContext);
        }
      }
      catch (Exception e) {
        addException(e);
        error(e);
      }
      catch (AssertionError e) {
        addException(e);
        resume(suspendContext);
      }
    }

    //executed in manager thread
    @Override
    public void resumed(SuspendContextImpl suspendContext) {
      final SuspendContextImpl pausedContext = myDebugProcess.getSuspendManager().getPausedContext();
      if (pausedContext != null) {
        myDebugProcess.getManagerThread().schedule(new SuspendContextCommandImpl(pausedContext) {
          @Override
          public void contextAction(@NotNull SuspendContextImpl suspendContext) {
            paused(pausedContext);
          }
        });
      }
    }
  }
}
