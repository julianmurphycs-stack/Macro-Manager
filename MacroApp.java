import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class MacroApp {
    private static JFrame mainWindow;

    private static final boolean USE_ROBOT = true;

    public static void main(String[] args){
        SwingUtilities.invokeLater(() ->{
        JFrame frame = new JFrame("InventoryX Macro Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //close app if user closes window
        frame.setContentPane(buildHomeScreen(frame));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        mainWindow = frame;
        });
    }

    public static void showMsg(String msg){
        JOptionPane.showMessageDialog(mainWindow, msg, " ", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String ask(String prompt){
        return JOptionPane.showInputDialog(mainWindow,prompt);
    }

    private static JPanel buildHomeScreen(JFrame window){
        JPanel panel = new JPanel(); 
        panel.setLayout(new GridLayout(5,1,16,16)); //5 rows 1 colum 16 pixel wide gaps
        panel.setBorder(BorderFactory.createEmptyBorder(16,16,16,16)); //move buttons a little away from edges

        JButton createButton = new JButton("Create Macro");
        JButton editButton = new JButton("Edit Macro");
        JButton runButton = new JButton("Run Macro");
        JButton deleteButton = new JButton("Delete Macro");
        JButton exitButton = new JButton("Exit");

        //if button gets clicked call respective function
        createButton.addActionListener(e -> {
            try {
                createMacro();} 
            catch (Exception ex){
                showError(window, ex);
            }
        });

        editButton.addActionListener(e -> {
            try {
                editMacro();} 
            catch (Exception ex){
                showError(window, ex);
            }
        });

        runButton.addActionListener(e -> {
            try {
                runMacro();} 
            catch (Exception ex){
                showError(window, ex);
            }
        });

        deleteButton.addActionListener(e -> {
            try {
                deleteMacro();} 
            catch (Exception ex){
                showError(window, ex);
            }
        });

        exitButton.addActionListener(e -> {
           System.exit(0);
        });

    panel.add(createButton);
    panel.add(editButton);
    panel.add(runButton);
    panel.add(deleteButton);
    panel.add(exitButton);
    return panel;
    }



    private static void showError(JFrame window, Exception ex){
        JOptionPane.showMessageDialog(window, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void createMacro() throws IOException{
        Macro macro = new Macro();
        showMsg("Creating new Macro please enter steps:");
        while (true){
            StepCode code = PromptStepOfCode();
            if (code == null) {
                break;
            }
            String param = PromptParam(code);
            macro.steps.add(new Step(code, param));
            String s = ask("Amount of time to wait before next step (in ms,0 for none): ").trim();
            try {
                int intAmountOfTime = Integer.parseInt(s);
                if (intAmountOfTime > 0){
                    macro.steps.add(new Step(StepCode.WT, Integer.toString(intAmountOfTime)));
                }
            } catch (NumberFormatException e ){
                showMsg("Enter an integer: ");
            }
        }
        String s = ask("Save macro to a file path (ex: C:/Users/(your user)/Desktop)"); 
        Path path = Paths.get(s.trim());
        CsvIO.write(macro,path);
        showMsg("File saved to " + path.toAbsolutePath());
    }
   
    private static void editMacro() throws IOException{
        String editedMacro =  ask("Path of Macro to edit");
            if (editedMacro == null){
                showMsg("Edit canceled");
                return;
            }
            Path path = Paths.get(editedMacro.trim());
            if (!Files.exists(path)){
                showMsg("File not found");
                return;
            }
            Macro macro = CsvIO.read(path);
            while (true){
                String steps = "";
                for (int i = 0; i < macro.steps.size(); i++){
                    steps += "(" + i + ")" + macro.steps.get(i) + "\n";
                }
            String editingMenu = "a) add step\n" + "r) remove step\n" + "s) save and quit\n" + "q) quit without saving\n" + steps;
            String choice = ask(editingMenu);
            if (choice == null){
                showMsg("Edit canceled");
                return;
            }
            choice = choice.trim().toLowerCase();
            if (choice.equals("a")){
                StepCode codeToAdd = PromptStepOfCode();
                if (codeToAdd == null){
                    showMsg("No step added");
                } else{
                    String paramForCode = PromptParam(codeToAdd);
                    String index = ask("Index to insert at from 0 to " + macro.steps.size() + "\n" + steps);
                    int insertIndex = macro.steps.size();
                    if (index != null){
                        index = index.trim();
                        try{
                            insertIndex = Integer.parseInt(index);
                        } catch(Exception e){

                        }
                    }
                    if (insertIndex < 0){
                        insertIndex = 0;
                    }
                    if (insertIndex > macro.steps.size()){
                        insertIndex = macro.steps.size();
                    }
                    macro.steps.add(insertIndex, new Step(codeToAdd,paramForCode)); //overload with index
                    String s = ask("Amount of time to wait before next step (in ms,0 for none): ");
                    if (s != null){
                        s = s.trim();
                        try {
                            int intAmountOfTime = Integer.parseInt(s);
                            if (intAmountOfTime > 0){
                                macro.steps.add(insertIndex + 1,new Step(StepCode.WT, Integer.toString(intAmountOfTime)));
                            }
                        }catch (NumberFormatException e ){
                        showMsg("Enter an integer: ");
                        }
                    }
                    showMsg("Step added");
                }
            } else if(choice.equals("r")){
                String indexOfStep = ask("Index of step you want to remove 0 to " + macro.steps.size() + "\n" + steps);
                if (indexOfStep == null){
                    showMsg("Remove Canceled");
                    continue;
                }
                indexOfStep = indexOfStep.trim();
                int indexToRemove = -1;

                try {
                    indexToRemove = Integer.parseInt(indexOfStep);}catch(Exception e){
                    showMsg("Please enter a number");
                    continue;
                }
                if (indexToRemove < 0 || indexToRemove >= macro.steps.size()){
                showMsg("Please enter a number equal to or greater than 0 and less the number of steps - 1");
                continue;
                }
                Step removedStep = macro.steps.remove(indexToRemove);
                showMsg("Removed" + removedStep);


            } else if(choice.equals("s")){
                CsvIO.write(macro,path);
                showMsg("Saved to " + path);
                return;
            } else if(choice.equals("q")){
                showMsg("Exited without saving");
                return;
            } else{
                showMsg("No valid choice selected");
            }
            }
            
    }

    private static void deleteMacro() {
        String macropath =  ask("Path of Macro to delete");
            if (macropath == null){
                showMsg("Delete canceled");
                return;
            }
            Path path = Paths.get(macropath.trim());
            if (!Files.exists(path)){
                showMsg("File not found");
                return;
            }
            try {
                Files.delete(path);
                showMsg("Deleted " + path);
            } catch (Exception  e) {
                showError(mainWindow, e);
            }
    }

   private static void runMacro() throws Exception{
    String s = ask("Run macro from a file path");
    if (s == null) return;

    Path path = Paths.get(s.trim());
    Macro macro = CsvIO.read(path);
    Executor exec = new Executor(USE_ROBOT);

    String loopsText = ask("How many times? Enter -1 for infinite loop");
    if (loopsText == null) return;

    int loops = Integer.parseInt(loopsText.trim());

        if (loops == -1){
            while (true){
                exec.execute(macro);
            }
        } else {
            for (int i = 0; i < loops; i++){
                exec.execute(macro);
            }
        }
    }

    private static StepCode PromptStepOfCode() {
        while (true){
            String s = ask("Step? TY(type) / CL(click) / DG(drag) / EV(event) / DONE");
            s = s.trim().toUpperCase();
            if (s.equals("DONE")){
                return null;
            }
            try {
                return StepCode.valueOf(s);
            } catch (IllegalArgumentException e){
                showMsg("Please Enter: TY(type) / CL(click) / DG(drag) / EV(event) / DONE");
            }
        }
    }

     private static String PromptParam(StepCode code) {
        while (true){
            if (code == StepCode.TY){
                String s = ask("Please enter text to type: ");
                return s;
            }else if (code == StepCode.CL){
                showMsg("Move the mouse over desired location and hit enter");
                Point p = MouseInfo.getPointerInfo().getLocation();
                return (p.x + " " + p.y);
            } else if (code == StepCode.DG){
            showMsg("Move mouse to START of drag, then press OK");
            Point start = MouseInfo.getPointerInfo().getLocation();

            showMsg("Now move mouse to END of drag, then press OK");
            Point end = MouseInfo.getPointerInfo().getLocation();

            return start.x + " " + start.y + " " + end.x + " " + end.y;
            }else if (code == StepCode.EV){
                String s = ask("Please enter path of desired image to wait until");
                return s.trim();
            } else {
                throw new IllegalStateException("Wait is added after this step, should never get here");
            }
    
        }
    }
}
    enum StepCode {TY,CL,DG,EV,WT}
    class Step{
        final StepCode code;
        final String param;
        Step(StepCode code, String param){
            this.code = code;
            this.param = param;
        }
        public String toString(){
            return code + "," + param;
        }
    }

    class Macro{
        final List<Step> steps = new ArrayList<>();
    }

class CsvIO{
    static Macro read(Path path) throws IOException{ //reads and does light error cheking on a file given a path, returns a macro based on what was read
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found");
        }
        List<String> lines = Files.readAllLines(path);
        Macro m = new Macro();
        int lineNum = 0;
        for (String origLine : lines){
            lineNum++;
            String line = origLine.strip();
            if (line.isEmpty()){
                continue;
            }
            String[] parts = splitCsvLine(line);
            StepCode code;
            code = StepCode.valueOf(parts[0].trim().toUpperCase());
            String param = parts[1].trim();
            if (code == StepCode.CL){
                String[] xy = param.split("\\s+");
                Integer.parseInt(xy[0]);
                Integer.parseInt(xy[1]);
                if (xy.length != 2){
                    throw new IOException("Line " + line + "incorrect, not x y");
                }
            }
            if (code == StepCode.WT){
                Integer.parseInt(param);
            }
            m.steps.add(new Step(code, param));
            }
        return m;
    }

    private static String[] splitCsvLine(String line){ //splits a line into an array of 2 strings and returns it 
        String[] parts = line.split(",",2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Expected Code,Param on " + line);
        }
        parts[0] = parts[0].trim();
        parts[1] = parts[1].trim();
        return parts;
    }

    public static void write(Macro macro, Path path) throws IOException{ //writes macro to path
        List<String> lines = new ArrayList<>();
        for (Step s : macro.steps){
            lines.add(s.toString());
        }
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null){
            Files.createDirectories(parent); //create all missing directories will not overite
        }
        Files.write(path,lines); 
    
    }

}
interface Actions{
    void typeText(String text);
    void Click(int x, int y);
    void Drag(int x1, int y1, int x2, int y2);
    void Event(String pathToImg);
    void sleepMs(int ms);
}

class TestRunActions implements Actions{
    public void typeText(String text){}
    public void Click(int x, int y){}
    public void Drag(int x1, int y1, int x2, int y2){}
    public void Event(String pathToImg){}
    public void sleepMs(int ms){}
}

class Executor{
    private final Actions actions;

    public Executor(boolean USE_ROBOT) throws AWTException {
        if (USE_ROBOT){
            this.actions = new RobotsActions();
        } else{
            this.actions = new TestRunActions();
        }
    }

    void execute(Macro macro){
        for (Step s : macro.steps){
            //MacroApp.showMsg("Executing" + s.code + " " + s.param);
            if (s.code == StepCode.TY){
                actions.typeText(s.param);
            } else if (s.code == StepCode.CL){
                String[] xy = s.param.split("\\s+");
                int x = Integer.parseInt(xy[0]);
                int y = Integer.parseInt(xy[1]);
                actions.Click(x, y);
            }else if (s.code == StepCode.DG){
                String[] xy = s.param.split("\\s+");
                int x1 = Integer.parseInt(xy[0]);
                int y1 = Integer.parseInt(xy[1]);
                int x2 = Integer.parseInt(xy[2]);
                int y2 = Integer.parseInt(xy[3]);
                actions.Drag(x1, y1, x2, y2); 
            }else if(s.code == StepCode.EV){
                actions.Event(s.param);
            } else if (s.code == StepCode.WT){
                try{
                int ms = Integer.parseInt(s.param);
                actions.sleepMs(ms);
                } catch (NumberFormatException e){
                int ms = 0;
                actions.sleepMs(ms);
                }
            }
        }
    }
    
}

class RobotsActions implements Actions{
    private final Robot robot;

    RobotsActions() throws AWTException{
        this.robot = new Robot();
    }

    @Override public void typeText(String text){
        //MacroApp.showMsg("Typing " + text);
        for (char ch : text.toCharArray()){
            if (Character.isUpperCase(ch)) {
                keyWithShift(Character.toUpperCase(ch)); 
            }else if (Character.isLowerCase(ch)) {
                pressAndRelease(KeyEvent.getExtendedKeyCodeForChar(Character.toUpperCase(ch)));
            } else if (ch == ' ') {
                pressAndRelease(KeyEvent.VK_SPACE);
            }else if (ch == '.') { 
                pressAndRelease(KeyEvent.VK_PERIOD);
            }else {
               MacroApp.showMsg("Unsupported character" + ch); 
            }
            sleepMs(5);
        }
    }

    @Override public void Drag(int x1, int y1, int x2, int y2){
    robot.mouseMove(x1, y1);
    robot.delay(50);

    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.delay(50);

    int steps = 20;
    for (int i = 1; i <= steps; i++){
        int x = x1 + (x2 - x1) * i / steps;
        int y = y1 + (y2 - y1) * i / steps;
        robot.mouseMove(x, y);
        robot.delay(15);
    }

    robot.delay(50);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    @Override public void Click(int x, int y){
        //MacroApp.showMsg("Clicking at " + x + "," + y);
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(20);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    @Override public void Event(String pathToImg){
        
    }

    @Override public void sleepMs(int ms){
        //MacroApp.showMsg("Waiting " + ms);
        robot.delay(ms);
    }

    public void keyWithShift(char upCase){
        robot.keyPress(KeyEvent.VK_SHIFT);
        pressAndRelease(KeyEvent.getExtendedKeyCodeForChar(upCase));
        robot.keyRelease(KeyEvent.VK_SHIFT);
    }

    public void pressAndRelease(int key){
        robot.keyPress(key);
        robot.delay(5);
        robot.keyRelease(key);
    }
}