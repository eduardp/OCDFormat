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
      result = getAlignedText(result);

      document.replace(textSelection.getOffset(), textSelection.getLength(), result);

    }
    catch (BadLocationException e) {
      return null;
    }

    return null;
  }

  private class Thing {
    public String LHS = "";
    public String RHS = "";
    public String separator = "";
    public String leadingWS = "";
    public String trailer   = "";
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
    for (String line : lines) {
      Thing t = new Thing();
      Matcher m = pattern.matcher(line);
      if (m.find()) {
        String theLHS = m.group(2);
        if (theLHS.startsWith("//") || theLHS.contains("(") || theLHS.startsWith("/*") || theLHS.startsWith("*") || theLHS.endsWith("*\\\\")) {
          t.LHS    = line;
          t.ignore = true;
        }
        else if (theLHS.contains("//")){
          int idx = theLHS.indexOf("//");
          t.LHS = theLHS.substring(0, idx);
          t.trailer = theLHS.substring(idx);
          t.leadingWS = m.group(1);
          while (t.LHS.endsWith(" ")) {
            t.trailer = " " + t.trailer;
            t.LHS = t.LHS.substring(0, t.LHS.length()-1);
          }
        }
        else {
          t.leadingWS = m.group(1);
          t.LHS       = theLHS;
          t.separator = m.group(3);
          t.RHS       = t.separator.length()==0?"":m.group(4).trim();
        }
      }
      else {
        t.LHS    = line;
        t.ignore = true;
      }
      things.add(t);
    }

    alignLeftHandSides(things);

    alignEqualsSigns(things);

    return buildFinalString(text, things);
  }

  private String buildFinalString(String text, List<Thing> things) {
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
      if (!thing.ignore && thing.RHS.length() > 0) {
        if (thing.separator.length() > 0) {
          sb.append(" ");
          sb.append(thing.separator);
          sb.append(" ");
        }
        sb.append(thing.RHS);
      }
      sb.append(thing.trailer);
    }

    if (text.endsWith("\n")) {
      sb.append("\n");
    }

    return sb.toString();
  }

  private void alignEqualsSigns(List<Thing> things) {

    int maxLHS = 0;
    for (Thing thing : things) {
      if (thing.ignore || thing.RHS.length() == 0) {
        continue;
      }
      if (maxLHS < thing.LHS.length()) {
        maxLHS = thing.LHS.length();
      }
    }

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

  private void alignLeftHandSides(List<Thing> things) {

    List<List<String>> items = new ArrayList<List<String>>();
    List<Integer>      maxes = new ArrayList<Integer>();

    populateItemsAndMaxes(things, items, maxes);

    padLHSFields(things, items, maxes);

  }

  private void padLHSFields(List<Thing> things, List<List<String>> items, List<Integer> maxes) {
    for (int index = 0; index < items.size(); index++) {
      List<String> cols = items.get(index);
      if (!things.get(index).ignore) {
        StringBuilder newLHS = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
          String item = cols.get(i);
          if (i > 0) {
            newLHS.append(" ");
          }
          newLHS.append(pad(item,maxes.get(i)));
        }
        things.get(index).LHS = newLHS.toString();
      }
    }
  }

  private void populateItemsAndMaxes(List<Thing> things, List<List<String>> items, List<Integer> maxes) {
    Pattern pattern = Pattern.compile("\\s*(\\S+)\\s*");
    for (Thing t : things) {
      List<String> cols = new ArrayList<String>();
      if (!t.ignore) {
        Matcher matcher = pattern.matcher(t.LHS);
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
