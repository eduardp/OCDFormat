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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class FormatCommandHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {

    ISelection selection = HandlerUtil.getCurrentSelection(event);
    if (selection == null) {
      return null;
    }

    if (selection == null || !(selection instanceof TextSelection) ) {
      return null;
    }
    TextSelection textSelection = (TextSelection)selection;

    IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
    if (activeEditor == null || !(activeEditor instanceof ITextEditor) ) {
      return null;
    }

    IDocumentProvider documentProvider = ((ITextEditor)activeEditor).getDocumentProvider();
    if (documentProvider == null) {
      return null;
    }

    IDocument document = documentProvider.getDocument(activeEditor.getEditorInput());
    if (document == null) {
      return null;
    }

    try {
      document.replace(textSelection.getOffset(), textSelection.getLength(), getAlignedText(textSelection.getText()));
    }
    catch (BadLocationException e) {
      return null;
    }

    return null;
  }

  private String getAlignedText(String text) {

    List<Expression> expressions = new ArrayList<Expression>();

    for (String line : text.split("\n")) {
      expressions.add(getExpressionFromLine(line));
    }

    alignLeftHandSides(expressions);

    alignEqualsSigns(expressions);

    return buildFinalString(text, expressions);
  }

  private Expression getExpressionFromLine(String line) {
    Expression expression = new Expression();
    Pattern    pattern    = Pattern.compile("(\\s*)([^=]+)(={0,1})(.*)");
    Matcher    matcher    = pattern.matcher(line);
    if (matcher.find()) {
      String theLHS = matcher.group(2);
      if (theLHS.startsWith("//") || theLHS.contains("(") || theLHS.startsWith("/*") || theLHS.startsWith("*") || theLHS.endsWith("*\\\\")) {
        expression.LHS    = line;
        expression.ignore = true;
      }
      else if (theLHS.contains("//")){
        int idx              = theLHS.indexOf("//");
        expression.LHS       = theLHS.substring(0, idx);
        expression.trailer   = theLHS.substring(idx);
        expression.leadingWS = matcher.group(1);
        //add any whitespace before the comment to the trailer
        while (expression.LHS.endsWith(" ")) {
          expression.trailer = " " + expression.trailer;
          expression.LHS     = expression.LHS.substring(0, expression.LHS.length()-1);
        }
      }
      else {
        expression.leadingWS = matcher.group(1);
        expression.LHS       = theLHS;
        expression.separator = matcher.group(3);
        expression.RHS       = expression.separator.length()==0?"":matcher.group(4).trim();
      }
    }
    else {
      expression.LHS    = line;
      expression.ignore = true;
    }
    return expression;
  }

  private String buildFinalString(String text, List<Expression> expressions) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < expressions.size(); i++) {
      Expression expression = expressions.get(i);
      if (i > 0) {
        sb.append("\n");
      }
      append(sb,expression.leadingWS,expression.LHS);
      if (!expression.ignore && expression.RHS.length() > 0) {
        if (expression.separator.length() > 0) {
          append(sb," ",expression.separator," ");
        }
        sb.append(expression.RHS);
      }
      sb.append(expression.trailer);
      while (sb.charAt(sb.length()-1) == ' ') {
        sb.deleteCharAt(sb.length()-1);
      }
    }

    if (text.endsWith("\n")) {
      sb.append("\n");
    }

    return sb.toString();
  }

  private void append(StringBuilder sb, String... string) {
    for (String str : string) {
      sb.append(str);
    }
  }

  /**
   * after all the cols on the LHS has been aligned, we need to align all the LHSs as a whole to ensure the equals signs are aligned
   */
  private void alignEqualsSigns(List<Expression> expressions) {

    int maxLHS = getMaxLHS(expressions);

    for (Expression expression : expressions) {
      if (expression.RHS.length() == 0) {
        continue;
      }
      expression.LHS = pad(expression.LHS, maxLHS);
    }

  }

  /**
   * returns the length of the longest LHS
   */
  private int getMaxLHS(List<Expression> expressions) {
    int maxLHS = 0;
    for (Expression expression : expressions) {
      if (expression.ignore || expression.RHS.length() == 0) {
        continue;
      }
      if (maxLHS < expression.LHS.length()) {
        maxLHS = expression.LHS.length();
      }
    }
    return maxLHS;
  }

  private void alignLeftHandSides(List<Expression> expressions) {
    List<List<String>> items = new ArrayList<List<String>>();
    List<Integer>      maxes = new ArrayList<Integer>();
    populateItemsAndMaxes(expressions, items, maxes);
    padLHSFields(expressions, items, maxes);
  }

  /**
   * for each line, space it's columns on the LHS according to the max size of the column
   */
  private void padLHSFields(List<Expression> expressions, List<List<String>> items, List<Integer> maxes) {
    for (int index = 0; index < items.size(); index++) {
      List<String> cols = items.get(index);
      if (!expressions.get(index).ignore) {
        StringBuilder newLHS = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
          String item = cols.get(i);
          if (i > 0) {
            newLHS.append(" ");
          }
          newLHS.append(pad(item,maxes.get(i)));
        }
        expressions.get(index).LHS = newLHS.toString();
      }
    }
  }

  /**
   * populate a 2 dim array of items to be put into columns on the LHS. For each column, determine the maximum width
   */
  private void populateItemsAndMaxes(List<Expression> expressions, List<List<String>> items, List<Integer> maxes) {
    Pattern pattern = Pattern.compile("\\s*(\\S+)\\s*");
    for (Expression expression : expressions) {
      List<String> cols = new ArrayList<String>();
      if (!expression.ignore) {
        Matcher matcher = pattern.matcher(expression.LHS);
        for(int i = 0; matcher.find(); i++) {
          String item = matcher.group(1);
          cols.add(item);
          if (i == maxes.size()) {
            maxes.add(0);
          }
          maxes.set(i, Math.max(item.length(), maxes.get(i)));
        }
      }
      items.add(cols);
    }
  }


  private String pad(String field, int length) {
    StringBuilder sb = new StringBuilder(field);
    for (int i = 0; i < length - field.length(); i++) {
      sb.append(" ");
    }
    return sb.toString();
  }

  private class Expression {
    public String  LHS       = "";
    public String  RHS       = "";
    public String  separator = "";
    public String  leadingWS = "";
    public String  trailer   = "";
    public boolean ignore    = false;

    @Override
    public String toString() {
      return "Thing: [" + leadingWS + "] " + LHS + "," + RHS;
    }

  }

  //test cases
  public static void main(String[] args) {

    String txt = "";
    txt="x\na b c d=b\n";
    txt = "  a b c d=b";
    txt="x\na b c d=b\n=c\nc ggg =\naaa\n";
    System.out.println(new FormatCommandHandler().getAlignedText(txt));
    System.out.println();

    txt ="ass=ass;\na = a";
    System.out.println(new FormatCommandHandler().getAlignedText(txt));
    System.out.println();

    txt = "PeriodicUpdateWindow.parent = parent;\n" +
        "PeriodicUpdateWindow.as = new AnalyticsSuite();";
    System.out.println(new FormatCommandHandler().getAlignedText(txt));
    System.out.println();

    txt = "  aa bb cc dd;\n" +
        "  aaa b    cccc;\n" +
        "  a bbbb c d;";
    System.out.println(new FormatCommandHandler().getAlignedText(txt));
    System.out.println();

    txt = "    updateMode             = \"\";\n" +
        "    lastProcessedMessage   = -1;\n" +
        "//sdkfljslkjdflsdjfkjsdkfjsldfkdfjlskdjfldk\n" +
        "    int puServerIsUp        = false;\n" +
        "    String puSuccessSinceEntering = false;\n";
    System.out.println(new FormatCommandHandler().getAlignedText(txt));
    System.out.println();

    txt = "Component component = this.lblProgressMain;\n" +
        "Componentsss co  aaaa;\n" +
        "nstraints.gridx = 0;";
    System.out.println(new FormatCommandHandler().getAlignedText(txt));
    System.out.println();

    txt = "  aa bb cc dd;\n" +
        "  aaa b    cccc;\n" +
        "  a bbbb c d;";
    System.out.println(new FormatCommandHandler().getAlignedText(txt));
    System.out.println();

    //--removes private\n"
    txt = "private IQPanel pnlRoot = null;\n" +
        "private JTabbedPane tpValidation = null;\n" +
        "//wkejwkjfjkd jskdjfk lskdfjlksdjf\n" +
        "private ValidateInputTables validateInputTables = null;\n" +
        "private ManageProfiles manageProfiles = null;";
    System.out.println(new FormatCommandHandler().getAlignedText(txt));
    System.out.println();

    txt = "private Timestamp initTimestamp; // When the update process was initiated.\n" +
        "private Time endTime; // Whens the update process finished.";
    System.out.println(new FormatCommandHandler().getAlignedText(txt));
    System.out.println();

    /*
public static final String STATUS_FINALIZING = "Finalizing";
public static final String STATUS_FINALIZED  = "Finalized";
public static final String STATUS_CANCELLED  = "Cancelled";
public static final String STATUS_ERROR      = "Error";
public static final String STATUS_FINISHED   = "Finished";

     */
  }

}
