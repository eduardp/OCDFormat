/**
 *   Copyright 2012 Eduard Penzhorn
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ocdFormat.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class BracketFinderCommandHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {

    IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
    if (activeEditor == null || !(activeEditor instanceof ITextEditor) ) {
      return null;
    }

    ITextEditor textEditor = (ITextEditor) activeEditor;

    ISelectionProvider selectionProvider = textEditor.getSelectionProvider();

    if (selectionProvider == null) {
      return null;
    }

    ISelection selection = selectionProvider.getSelection();
    if (selection == null || !(selection instanceof ITextSelection) ) {
      return null;
    }

    IDocumentProvider documentProvider = textEditor.getDocumentProvider();
    if (documentProvider == null) {
      return null;
    }

    IDocument document = documentProvider.getDocument(activeEditor.getEditorInput());
    if (document == null ) {
      return null;
    }

    int offset = ((ITextSelection)selection).getOffset();
    offset -= 2;
    int nesting = 0;

    while (offset > 0) {
      try {
        char char1 = document.getChar(offset);
        if (char1 == '(' || char1 == '{') {
          nesting--;
          if (nesting < 0) {
            selectionProvider.setSelection(new TextSelection(offset+1, 0));
            break;
          }
        }
        else if (char1 == ')' || char1 == '}') {
          nesting++;
        }
      }
      catch (BadLocationException e) {
        return null;
      }
      offset--;
    }

    return null;
  }

}
