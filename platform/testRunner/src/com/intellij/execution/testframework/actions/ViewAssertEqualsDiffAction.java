/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.execution.testframework.actions;

import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.TestFrameworkRunningModel;
import com.intellij.execution.testframework.TestTreeView;
import com.intellij.execution.testframework.TestTreeViewAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ViewAssertEqualsDiffAction extends AnAction implements TestTreeViewAction {
  @NonNls public static final String ACTION_ID = "openAssertEqualsDiff";

  public void actionPerformed(final AnActionEvent e) {
    if (!openDiff(e.getDataContext())) {
      final Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
      Messages.showInfoMessage(component, "Comparison error was not found", "No Comparison Data Found");
    }
  }

  public static boolean openDiff(DataContext context) {
    final AbstractTestProxy testProxy = AbstractTestProxy.DATA_KEY.getData(context);
    if (testProxy != null) {
      final AbstractTestProxy.AssertEqualsDiffViewerProvider diffViewerProvider = testProxy.getDiffViewerProvider();
      if (diffViewerProvider != null) {
        final Project project = CommonDataKeys.PROJECT.getData(context);
        if (diffViewerProvider instanceof AbstractTestProxy.AssertEqualsMultiDiffViewProvider) {
          final TestFrameworkRunningModel runningModel = TestTreeView.MODEL_DATA_KEY.getData(context);
          final List<AbstractTestProxy.AssertEqualsMultiDiffViewProvider> providers = collectAvailableProviders(runningModel);
          final MyAssertEqualsDiffChain diffChain =
            providers.size() > 1 ? new MyAssertEqualsDiffChain(providers, (AbstractTestProxy.AssertEqualsMultiDiffViewProvider)diffViewerProvider) : null;
          ((AbstractTestProxy.AssertEqualsMultiDiffViewProvider)diffViewerProvider).openMultiDiff(project, diffChain);
        } else {
          diffViewerProvider.openDiff(project);
        }
        return true;
      }
    }
    return false;
  }

  private static List<AbstractTestProxy.AssertEqualsMultiDiffViewProvider> collectAvailableProviders(TestFrameworkRunningModel model) {
    final List<AbstractTestProxy.AssertEqualsMultiDiffViewProvider> providers = new ArrayList<AbstractTestProxy.AssertEqualsMultiDiffViewProvider>();
    if (model != null) {
      final AbstractTestProxy root = model.getRoot();
      final List<? extends AbstractTestProxy> allTests = root.getAllTests();
      for (AbstractTestProxy test : allTests) {
        if (test.isLeaf()) {
          final AbstractTestProxy.AssertEqualsDiffViewerProvider provider = test.getDiffViewerProvider();
          if (provider instanceof AbstractTestProxy.AssertEqualsMultiDiffViewProvider) {
            providers.add((AbstractTestProxy.AssertEqualsMultiDiffViewProvider)provider);
          }
        }
      }
    }
    return providers;
  }

  public void update(final AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final boolean enabled;
    final DataContext dataContext = e.getDataContext();
    if (CommonDataKeys.PROJECT.getData(dataContext) == null) {
      enabled = false;
    }
    else {
      final AbstractTestProxy test = AbstractTestProxy.DATA_KEY.getData(dataContext);
      if (test != null) {
        if (test.isLeaf()) {
          enabled = test.getDiffViewerProvider() != null;
        }
        else if (test.isDefect()) {
          enabled = true;
        }
        else {
          enabled = false;
        }
      }
      else {
        enabled = false;
      }
    }
    presentation.setEnabled(enabled);
    presentation.setVisible(enabled);
  }

  private static class MyAssertEqualsDiffChain implements AbstractTestProxy.AssertEqualsDiffChain {


    private final List<AbstractTestProxy.AssertEqualsMultiDiffViewProvider> myProviders;
    private AbstractTestProxy.AssertEqualsMultiDiffViewProvider myProvider;

    public MyAssertEqualsDiffChain(List<AbstractTestProxy.AssertEqualsMultiDiffViewProvider> providers,
                                   AbstractTestProxy.AssertEqualsMultiDiffViewProvider provider) {
      myProviders = providers;
      myProvider = provider;
    }

    @Override
    public AbstractTestProxy.AssertEqualsMultiDiffViewProvider getPrevious() {
      final int prevIdx = (myProviders.size() + myProviders.indexOf(myProvider) - 1) % myProviders.size();
      return myProviders.get(prevIdx);
    }

    @Override
    public AbstractTestProxy.AssertEqualsMultiDiffViewProvider getCurrent() {
      return myProvider;
    }

    @Override
    public AbstractTestProxy.AssertEqualsMultiDiffViewProvider getNext() {
      final int nextIdx = (myProviders.indexOf(myProvider) + 1) % myProviders.size();
      return myProviders.get(nextIdx);
    }

    @Override
    public void setCurrent(AbstractTestProxy.AssertEqualsMultiDiffViewProvider provider) {
      myProvider = provider;
    }
  }
}
