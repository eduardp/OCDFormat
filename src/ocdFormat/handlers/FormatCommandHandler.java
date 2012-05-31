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

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class FormatCommandHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {

    ISelection selection = HandlerUtil.getCurrentSelection(event);
    if (selection == null) {
      return null;
    }

    TextSelection textSelection = (TextSelection)selection;
    if (textSelection == null || !(textSelection instanceof TextSelection) ) {
      return null;
    }

    IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
    if (activeEditor == null || !(activeEditor instanceof ITextEditor) ) {
      return null;
    }

    IDocumentProvider p = ((ITextEditor) activeEditor).getDocumentProvider();
    if (p == null) {
      return null;
    }

    IDocument document = p.getDocument(activeEditor.getEditorInput());
    if (document == null) {
      return null;
    }

    try {
      String result = textSelection.getText();
      //      if (result.indexOf('\n') == -1) {
      //        return null;
      //      }
      //      if (result.indexOf('=') == -1) {
      //        result = getTextWithColumnsAligned(result);
      //      }
      //      else {
      //        result = getTextWithEqualsAligned(result);
      //      }
      //
      //      document.replace(textSelection.getOffset(), textSelection.getLength(), result);
      result = getAlignedText(result);

      document.replace(textSelection.getOffset(), textSelection.getLength(), result);

    }
    catch (BadLocationException e) {
      return null;
    }

    return null;
  }

  private class Thing {
    public String LHS;
    public String RHS;
    public String leadingWS;
    public boolean ignore = false;

    @Override
    public String toString() {
      return "Thing: [" + leadingWS + "] " + LHS + "," + RHS;
    }

  }

  private String getAlignedText(String text) {
    String[] lines  = text.split("\n");
    List<Thing> things = new ArrayList<Thing>();
    Pattern pattern = Pattern.compile("(\\s*)([^=]+)(={0,1})(.*)");
    //    Pattern patternWS = Pattern.compile("(\\s*)\\S+");
    for (String line : lines) {
      Thing t = new Thing();
      Matcher m = pattern.matcher(line);
      if (m.find()) {
        //        for (int i = 1; i <= m.groupCount(); i++) {
        //          System.out.println("-" + m.group(i) + "-");
        //        }
        //        System.out.println("-----------------------------------------------");
        t.leadingWS = m.group(1);
        String theLHS = m.group(2);
        if (theLHS.startsWith("//")) {
          t.LHS = line;
          t.RHS = "";
          t.ignore  = true;
        }
        t.LHS = theLHS;
        t.RHS = m.group(3).length()==0?"":m.group(4).trim();
      }
      //      else {
      //        Matcher m2 = patternWS.matcher(line);
      //        if (m2.find()) {
      //          t.leadingWS = m2.group(1);
      //          t.LHS = line.trim();
      //          t.RHS = "";
      //        }
      else {
        t.leadingWS = "";
        t.LHS = line;
        t.RHS = "";
      }
      //      }
      things.add(t);
    }
    int maxLHS = alignLeftHandSides(things);
    //    for (Thing thing : things) {
    //      System.out.println(thing);
    //    }
    maxLHS = 0;
    for (Thing thing : things) {
      if (thing.RHS.length()==0) {
        continue;
      }
      if (maxLHS < thing.LHS.length()) {
        maxLHS = thing.LHS.length();
      }
    }
    alignEqualsSigns(things,maxLHS);

    StringBuilder sb = new StringBuilder();

    boolean first = true;
    for (Thing thing : things) {
      if (first) {
        first = false;
      }
      else {
        sb.append("\n");
      }
      sb.append(thing.leadingWS);
      sb.append(thing.LHS);
      if (thing.RHS.length() > 0) {
        sb.append("= ");
        sb.append(thing.RHS);
      }
    }

    if (text.endsWith("\n")) {
      sb.append("\n");
    }

    return sb.toString();
  }

  private void alignEqualsSigns(List<Thing> things, int maxLHS) {
    for (Thing thing : things) {
      if (thing.RHS.length() == 0) {
        continue;
      }
      int target = maxLHS - thing.LHS.length();
      for (int i = 0; i < target; i++) {
        thing.LHS += " ";
      }

    }
  }

  private int alignLeftHandSides(List<Thing> things) {
    List<List<String>> items = new ArrayList<List<String>>();
    List<Integer> maxes = new ArrayList<Integer>();

    Pattern pattern = Pattern.compile("\\s*(\\S+)\\s*");

    for (Thing t : things) {
      List<String> cols = new ArrayList<String>();
      Matcher matcher = pattern.matcher(t.LHS);
      int i = 0;
      while (matcher.find()) {
        String item = matcher.group(1);
        cols.add(item);
        if (i == maxes.size()) {
          maxes.add(0);
        }
        int length = item.length();
        if (length > maxes.get(i)) {
          maxes.set(i, length);
        }
        i++;
      }
      items.add(cols);
    }

    int index = 0;
    for (List<String> cols : items) {
      int i = 0;
      StringBuilder newLHS = new StringBuilder();
      for (String item : cols) {
        newLHS.append(item);
        for (int j = item.length(); j < maxes.get(i); j++) {
          newLHS.append(" ");
        }
        newLHS.append(" ");
        i++;
      }
      things.get(index).LHS = newLHS.toString();
      index++;
    }

    int maxLHS = maxes.size();
    for (Integer i : maxes) {
      maxLHS += i;
    }
    return maxLHS;

  }


  public static void main(String[] args) {

    String txt = "";
    txt="x\na b c d=b\n";
    txt = "  a b c d=b";
    txt="x\na b c d=b\n=c\nc ggg =\naaa\n";
    //new FormatCommandHandler().getAlignedText(txt);
    System.out.println(new FormatCommandHandler().getAlignedText(txt));
    System.out.println();
    //System.exit(0);
    txt ="ass=ass;\na = a";
    System.out.println(new FormatCommandHandler().getAlignedText(txt));
    System.out.println();

    txt = "PeriodicUpdateWindow.parent = parent;\n" +
        "PeriodicUpdateWindow.as = new AnalyticsSuite();";
    System.out.println(new FormatCommandHandler().getAlignedText(txt));
    System.out.println();    txt = "  aa bb cc dd;\n" +
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
        "private ValidateInputTables validateInputTables = null;\n" +
        "private ManageProfiles manageProfiles = null;";
    System.out.print(new FormatCommandHandler().getAlignedText(txt));
    System.out.println("<>");


    /* --adds extra newline
	 private IQPanel pnlRoot;
private JTabbedPane tpValidation;
private ValidateInputTables validateInputTables;
private ManageProfiles manageProfiles;
     */

    /* -- comments/stuff after
	 private Timestamp             initTimestamp; // When the update process was initiated.
private Timestamp             endTimestamp;  // When the update process finished.
     */

    /*
public static final String STATUS_FINALIZING = "Finalizing";
public static final String STATUS_FINALIZED  = "Finalized";
public static final String STATUS_CANCELLED  = "Cancelled";
public static final String STATUS_ERROR      = "Error";
public static final String STATUS_FINISHED   = "Finished";

     */
  }

}
