/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.testFramework;

import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public abstract class ResolveTestCase extends PsiTestCase {
  protected static final String MARKER = "<ref>";

  private Document myDocument;

  @Override
  protected void tearDown() throws Exception {
    if (myDocument != null) {
      FileDocumentManager.getInstance().reloadFromDisk(myDocument);
    }

    super.tearDown();
  }

  protected PsiReference configureByFile(@NotNull String filePath) throws Exception {
    return configureByFile(filePath, null);
  }

  protected PsiReference configureByFile(@TestDataFile @NotNull String filePath, @Nullable VirtualFile parentDir) throws Exception {
    final String fullPath = getTestDataPath() + filePath;
    final VirtualFile vFile = LocalFileSystem.getInstance().findFileByPath(fullPath.replace(File.separatorChar, '/'));
    assertNotNull("file " + filePath + " not found", vFile);

    String fileText = StringUtil.convertLineSeparators(VfsUtilCore.loadText(vFile));
    return configureByFileText(fileText, vFile.getName(), parentDir);
  }

  protected PsiReference configureByFileText(String fileText, String fileName) throws Exception {
    return configureByFileText(fileText, fileName, null);
  }

  protected PsiReference configureByFileText(String fileText, String fileName, @Nullable VirtualFile parentDir) throws Exception {
    int offset = fileText.indexOf(MARKER);
    assertTrue(offset >= 0);
    fileText = fileText.substring(0, offset) + fileText.substring(offset + MARKER.length());

    if (parentDir == null) {
      myFile = createFile(myModule, fileName, fileText);
    }
    else {
      VirtualFile existing = parentDir.findChild(fileName);
      if (existing != null) {
        myDocument = FileDocumentManager.getInstance().getDocument(existing);
        assertNotNull(myDocument);
        myDocument.setText(fileText);
        myFile = PsiManager.getInstance(getProject()).findFile(existing);
        assertNotNull(myFile);
        assertEquals(fileText, myFile.getText());
      }
      else {
        myFile = createFile(myModule, parentDir, fileName, fileText);
      }
    }

    PsiReference ref = myFile.findReferenceAt(offset);
    assertNotNull(ref);
    return ref;
  }

  @Override
  protected String getTestDataPath() {
    return PathManagerEx.getTestDataPath() + "/psi/resolve/";
  }
}