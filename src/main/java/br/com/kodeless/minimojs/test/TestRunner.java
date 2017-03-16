package br.com.kodeless.minimojs.test;

/**
 * Created by eduardo on 06/03/17.
 */

import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.util.*;

public class TestRunner {
    private static final long serialVersionUID = 1L;
    private WebDriver driver;

    private Map<String, List<String>> getTests(String sourceFolder) {
        String basePath = sourceFolder;
        File baseDir = new File(basePath);
        int cutoff = baseDir.getAbsolutePath().length();
        Map<String, List<String>> testsMap = new HashMap<String, List<String>>();
        fillTests(baseDir, cutoff, testsMap);
        return testsMap;
    }

    private void fillTests(File baseDir, int cutoff, Map<String, List<String>> testsMap) {
        List<String> tests = new ArrayList<String>();
        for (File file : baseDir.listFiles()) {
            if (file.isDirectory()) {
                fillTests(file, cutoff, testsMap);
            } else {
                if (file.getName().endsWith(".htmx") && !file.getName().endsWith(".modal.htmx")) {
                    tests.add(file.getName().substring(0, file.getName().length() - ".htmx".length()));
                }
            }
        }
        if (!tests.isEmpty()) {
            testsMap.put(baseDir.getAbsolutePath().substring(cutoff), tests);
        }
    }

    public void execTest(String testToRun, Map<String, List<String>> tests, String basePath) {
        int[] errorCount = {0};
        StringBuffer sb = new StringBuffer();
        try {
            startDriver();

            if (testToRun == null || testToRun.trim().equals("/") || testToRun.trim().equals("")) {
                for (Map.Entry<String, List<String>> e : tests.entrySet()) {
                    exec(sb, errorCount, e.getKey(), e.getValue(), basePath);
                }
            } else {
                List<String> testList = tests.get(testToRun);
                if (testList == null) {
                    errorCount[0]++;
                    sb.append("Test group not found");
                } else {
                    exec(sb, errorCount, testToRun, testList, basePath);
                }
            }
        } finally {
            System.out.println(driver);
            System.out.println(driver != null);
            if (driver != null) {
                System.out.println(driver);
                try {
                    driver.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println((errorCount[0] > 0 ? "============\nERROR: " + errorCount[0] + "\n" : "============\nSUCCESS\n") + sb.toString());
    }

    private void startDriver() {
        driver = new ChromeDriver();
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
    }

    private void exec(StringBuffer sb, int[] errorCount, String path, List<String> testList, String basePath) {
        sb.append(path);
        for (String test : testList) {
            sb.append("\n    ").append(path).append("/").append(test).append(": ");
            String result = runTest(path, test, basePath);
            if (result == null) {
                sb.append(" not configured");
            } else if (result.equals("Success")) {
                sb.append(" Success");
            } else {
                errorCount[0]++;
                sb.append(" Error: ").append(result);
            }
        }
        sb.append("\n");
    }

    private String runTest(String path, String test, String basePath) {
        basePath = basePath == null || basePath.trim().equals("") ? "http://localhost:8080" : basePath;

        System.out.println("Running test: " + basePath + path + "/" + test);
        try {
            String url = (basePath + path + "/" + test + ".html").replace("//", "/");
            if (url.startsWith("/")) {
                //local url
                url = "file://" + url;
            }
            driver.get(url);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error in test " + basePath + path + "/" + test + ": " + e.getMessage();
        }
        String title = driver.getTitle();
        if (title.endsWith("Server Error")) {
            return title;
        }
        WebElement instructions;
        try {
            instructions = driver.findElement(By.id("instructions"));
        } catch (NoSuchElementException e) {
            System.out.println("Test is not configured");
            return null;
        }
        int step = 0;
        List<Instruction> instructionList = parse(instructions.getText());

        int count = 0;
        try {
            String text = driver.findElement(By.id("load")).getText();
            while (!text.equals("Loaded:true")) {
                if (count > 20) {
                    return "Timeout error. X didn't load";
                }
                try {
                    Thread.sleep(100);
                    count++;
                } catch (Exception e) {
                }
                text = driver.findElement(By.id("load")).getText();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Load Exception: " + e.getMessage();
        }
        try {
            for (Instruction i : instructionList) {
                if (i.command.equals("wait")) {
                    try {
                        Thread.sleep(Integer.parseInt(i.elementId));
                    } catch (Exception e) {
                    }
                } else if (i.command.equals("type")) {
                    String id = i.elementId;
                    WebElement el = getElements(id);
                    el.sendKeys(i.paramList.get(0));
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                } else if (i.command.equals("text")) {
                    String id = i.elementId;
                    WebElement el = getElements(id);
                    String text = el.getText().replace(" ", "");
                    if (!text.equals(i.paramList.get(0))) {
                        return step + ": InnerText of element " + id + " = " + el.getText() + " and should be "
                                + i.paramList.get(0);
                    }
                } else if (i.command.equals("value")) {
                    String id = i.elementId;
                    WebElement el = getElements(id);
                    String value = el.getAttribute("value");
                    String paramValue = i.paramList.get(0);
                    if (!paramValue.equals(value) && !(paramValue.equals("") && value == null)) {
                        return step + ": Value of element " + id + " = " + el.getAttribute("value") + " and should be "
                                + paramValue;
                    }
                } else if (i.command.equals("notfound")) {
                    String id = i.elementId;
                    boolean notfound = false;
                    try {
                        driver.findElement(By.id(id));
                    } catch (NoSuchElementException e) {
                        notfound = true;
                    }
                    if (!notfound) {
                        return step + ": Should be notfound " + id;
                    }
                } else if (i.command.equals("found")) {
                    String id = i.elementId;
                    boolean found = true;
                    try {
                        driver.findElement(By.id(id));
                    } catch (NoSuchElementException e) {
                        found = false;
                    }
                    if (!found) {
                        return step + ": Should be found " + id;
                    }
                } else if (i.command.equals("click")) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                    String id = i.elementId;
                    WebElement el = getElements(id);
                    el.click();
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                } else if (i.command.equals("select")) {
                    String id = i.elementId;
                    WebElement el = getElements(id);
                    Select dropdown = new Select(el);
                    dropdown.selectByIndex(Integer.parseInt(i.paramList.get(0)));
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                } else if (i.command.equals("attr")) {
                    String id = i.elementId;
                    WebElement el = getElements(id);
                    String attr = el.getAttribute(i.paramList.get(0));
                    String value = i.paramList.size() > 1 ? i.paramList.get(1) : "";
                    if (!value.equals(attr) && !(value.equals("") && attr == null)
                            && !(value.equals("false") && attr == null)) {
                        return step + ": Attribute " + i.paramList.get(0) + " of element " + id + " not equal to "
                                + value;
                    }
                } else if (i.command.equals("del")) {
                    String id = i.elementId;
                    WebElement el = getElements(id);
                    int times = Integer.parseInt(i.paramList.get(0));
                    for (int j = 0; j < times; j++) {
                        el.sendKeys(Keys.BACK_SPACE);
                    }
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                } else if (i.command.equals("down")) {
                    String id = i.elementId;
                    WebElement el = getElements(id);
                    int times = Integer.parseInt(i.paramList.get(0));
                    for (int j = 0; j < times; j++) {
                        el.sendKeys(Keys.ARROW_DOWN);
                    }
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                } else if (i.command.equals("enter")) {
                    String id = i.elementId;
                    WebElement el = getElements(id);
                    el.sendKeys(Keys.ENTER);
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                } else if (i.command.equals("eq")) {
                    String id = i.elementId;
                    WebElement el = getElements(id);
                    String id2 = i.paramList.get(0);
                    WebElement el2 = getElements(id2);

                    String text = el.getText().replace(" ", "");
                    String text2 = el2.getText().replace(" ", "");

                    if (!text.equals(text2)) {
                        return step + ": InnerText of element " + id + " = " + el.getText() + " is different of innerText of element " + id2 + " = " + el2.getText();
                    }
                } else if (i.command.equals("neq")) {
                    String id = i.elementId;
                    WebElement el = getElements(id);
                    String id2 = i.paramList.get(0);
                    WebElement el2 = getElements(id2);

                    String text = el.getText().replace(" ", "");
                    String text2 = el2.getText().replace(" ", "");

                    if (text.equals(text2)) {
                        return step + ": InnerText of element " + id + " = " + el.getText() + " is equals to innerText of element " + id2 + " = " + el2.getText();
                    }
                }
                step++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return step + ": Exception: " + e.getMessage();
        }
        System.out.println("Test successful");
        return "Success";
    }

    private WebElement getElements(String idOrName) {
        WebElement el = driver.findElement(By.id(idOrName));
        if (el == null) {
            el = driver.findElement(By.className(idOrName));
        }
        return el;
    }

    private List<Instruction> parse(String text) {
        List<Instruction> result = new ArrayList<TestRunner.Instruction>();
        String[] lines = text.trim().split(";");
        for (String line : lines) {
            if (line.trim().equals("")) {
                continue;
            }
            String[] split = line.trim().split(" ");
            Instruction i = new Instruction();
            i.command = split[0];
            if (split.length > 1) {
                i.elementId = split[1];
            }
            if (split.length > 2) {
                for (int j = 2; j < split.length; j++) {
                    i.paramList.add(split[j]);
                }
            } else {
                i.paramList.add("");
            }
            result.add(i);
        }
        return result;
    }

    private static class Instruction {
        String command;
        String elementId;
        List<String> paramList = new ArrayList<String>();

        private String join(List<String> list) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i));
                if (i < list.size() - 1) {
                    sb.append(",");
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return "Command: " + command + ", element: " + elementId + ", paramList: " + join(paramList);
        }
    }

    public static void main(String[] a) {
        Scanner sc = new Scanner(System.in);
        String sourcePath = null;
        boolean exists;
        do {
            System.out.println("What is the source (uncompiled) path? ");
            sourcePath = sc.nextLine();
            exists = new File(sourcePath).exists();
            if (!exists) {
                System.out.println("Invalid path");
            }
        } while (!exists);

        System.out.println("What is the base path of the running project (ex: http://localhost:9090/ctx or /Users/Vader/project) ? ");
        String basePath = sc.nextLine();


        TestRunner runner = new TestRunner();

        Map<String, List<String>> tests = runner.getTests(sourcePath + "/pages");
        Map<Integer, String> testByIndex = new HashMap<>();
        while (true) {
            int count = 1;
            System.out.println("==========================================");
            for (String test : tests.keySet()) {
                testByIndex.put(count, test);
                System.out.println(count++ + ") " + test);
            }

            System.out.println("==========================================");

            System.out.println("Type the number of the test to run or type 0 to run all or a q to quit");
            String testNum = sc.nextLine();
            int index = 0;
            if (testNum.equals("q")) {
                break;
            } else {
                try {
                    index = Integer.parseInt(testNum);
                } catch (Exception e) {
                    System.out.println("Invalid test");
                    continue;
                }
            }
            runner.execTest(testByIndex.get(index), tests, basePath);
            System.out.println("Hit enter to continue or q to quit");
            String next = sc.nextLine();
            if (next.equals("q")) {
                break;
            }
        }

    }
}
