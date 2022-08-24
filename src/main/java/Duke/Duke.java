package Duke;

import java.io.FileWriter;
import java.sql.SQLSyntaxErrorException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.ListIterator;

import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.format.DateTimeFormatter;


public class Duke {
    private ArrayList<Task> all = new ArrayList<>();
    enum TaskType {
        TODO,
        EVENT,
        DEADLINE
    }
    private TaskList taskList;
    private Storage storage;
    private Parser parser;
    private Ui ui;

    /**
     * Initializes Duke instance. Initializes the ui, taskList, parser and Storage instance.
     */
    public Duke() {
        ui = new Ui();
        taskList = new TaskList();
        parser = new Parser();
        storage = new Storage("data/Duke.txt");
    }

    // abstractions

    private class Ui {
        private Scanner sc;

        public Ui() {
            sc = new Scanner(System.in);
        }

        public void welcome() {
            System.out.println("Hello! I am Duke");
            System.out.println("What can I do for you?");
        }

        public String readCommand() {
            if (sc.hasNext()) {
                return sc.nextLine();
            } else {
                return "";
            }
        }

        public void invalidInput() {
            System.out.println("I'm sorry, but I don't know what that means :-(");
        }

        public void showError(String s) {
            System.out.println(s);
        }

        public void marked(Task task) {
            System.out.println("Nice! I've marked this task as done:");
            System.out.println(task.toString());
        }

        public void unmarked(Task task) {
            System.out.println("OK, I've marked this task as not done yet:");
            System.out.println(task.toString());
        }

        public void added(Task task) {
            System.out.println("Got it. I've added this task:");
            System.out.printf("  added: %s\n", task.toString());
            System.out.printf("Now you have %s tasks in the list.\n", taskList.count());
        }

        public void deleted(Task task) {
            System.out.println("Got it. I've deleted this task:");
            System.out.printf("  added: %s\n", task.toString());
            System.out.printf("Now you have %s tasks in the list.\n", taskList.count());
        }

        public void exit() {
            System.out.println("Bye!");
        }

        public void showLine() {
            System.out.println("_______________________________");
        }
    }

    private class TaskList {
        private ArrayList<Task> all;

        public TaskList() {
            all = new ArrayList<>();
        }

        public void displayList() {
            System.out.println("Here are the tasks in your list:");
            for (int i = 0; i < count(); i++) {
                System.out.printf("%s. %s\n", i + 1, all.get(i).toString());
            }
        }

        public void findList(String target) {
            System.out.println("Here are the tasks in your list with the keyword:");
            for (int i = 0; i < count(); i++) {
                if (all.get(i).getDescription().indexOf(target) != -1) {
                    System.out.printf("%s. %s\n", i + 1, all.get(i).toString());
                }
            }
        }

        public void addTask(Task task) {
            all.add(task);
            ui.added(task);
        }

        public void deleteTask(int index) {
            Task deleted = all.get(index - 1);
            all.remove(index - 1);
            ui.deleted(deleted);
        }

        public Task getTask(int index) {
            return all.get(index);
        }

        public int count() {
            return all.size();
        }
    }

    private class Storage {
        private String filePath;

        public Storage(String path) {
            filePath = path;
        }

        /**
         * Loads data from the text file "Duke.txt". Store the data in the taskList instance
         *
         * @throws FileNotFoundException
         */
        public void loadData() throws FileNotFoundException {
            File f = new File(filePath);
            Scanner sc = new Scanner(f);

            while (sc.hasNext()) {
                String s = sc.nextLine();
                char type = s.charAt(1);
                boolean isDone = s.charAt(4) == 'X';
                String des = s.substring(7);
                Task newTask;

                if (type == 'T') {
                    newTask = new Todo(des);
                } else if (type == 'D') {
                    int i = des.indexOf('(');
                    newTask = new Deadline(des.substring(0, i - 1), parser.parseStringToDate(des.substring(i + 5, i + 15)));
                } else if (type == 'E') {
                    int i = des.indexOf('(');
                    newTask = new Event(des.substring(0, i - 1), parser.parseStringToDate(des.substring(i + 5, i + 15)));
                } else {
                    newTask = null;
                }

                taskList.addTask(newTask);
            }
        }

        /**
         * Write data to the text file "Duke.txt".
         */
        public void saveData() {
            try {
                FileWriter fw = new FileWriter(filePath);
                String s = "";
                for (int i = 0; i < all.size(); i++) {
                    s += all.get(i).toString() + "\n";
                }
                fw.write(s);
                fw.close();
            } catch (IOException e) {
                System.out.println("Something went wrong: " + e.getMessage());
            }
        }
    }

    private abstract class Command {
        /**
         * Execute the command based on its type.
         */
        public abstract void execute();

        /**
         * Returns whether it is an exit command.
         *
         * @return a boolean of whether it is an exit command.
         */
        public abstract boolean isExit();
    }

    private class AddCommand extends Command {
        private TaskType type;
        private String description;
        private LocalDate date;

        public AddCommand(TaskType type, String description ) {
            super();
            this.type = type;
            this.description = description;
        }


        /**
         * Execute the add command. Determine the type of task added then add it to taskList.
         */
        @Override
        public void execute() {
            if (type == TaskType.TODO) {
                taskList.addTask(new Todo(description));
            } else if (type == TaskType.DEADLINE) {
                try {
                    taskList.addTask(new Deadline(parser.extractDescription(description),
                                                  parser.extractDate(description)));
                } catch (DukeException e) {
                    ui.showError("format incorrect!");
                }
            } else if (type == TaskType.EVENT) {
                try {
                    taskList.addTask(new Event(parser.extractDescription(description),
                            parser.extractDate(description)));
                } catch (DukeException e) {
                    ui.showError("format incorrect!");
                }
            } else {
                ui.showError("type not supported");
            }
        }

        @Override
        public boolean isExit () {
            return false;
        }
    }

    private class ListCommand extends Command {
        public ListCommand() { super(); }

        /**
         * Execute the List command. List all tasks in taskList.
         */
        @Override
        public void execute()  {
            taskList.displayList();
        }

        @Override
        public boolean isExit () {
            return false;
        }
    }

    private class FindCommand extends Command {
        private String target;

        public FindCommand(String target) {
            super();
            this.target = target;
        }

        /**
         * Execute the find command. Display all tasks containing the input text.
         */
        @Override
        public void execute()  {
            taskList.findList(target);
        }

        @Override
        public boolean isExit () {
            return false;
        }
    }

    private class MarkCommand extends Command {
        private boolean isMark;
        private int index;

        public MarkCommand(boolean isMark, int index) {
            super();
            this.isMark = isMark;
            this.index = index;
        }

        /**
         * Execute the mark command.
         * Mark the task as done or undone.
         */
        @Override
        public void execute() {
            Task task = taskList.getTask(index - 1);

            if (isMark) {
                task.markAsDone();
                ui.marked(task);
            } else {
                task.markAsUndone();
                ui.unmarked(task);
            }
        }

        @Override
        public boolean isExit () {
            return false;
        }
    }

    private class DeleteCommand extends Command {
        private int index;

        public DeleteCommand(int index) {
            super();
            this.index = index;
        }

        /**
         * Execute the delete command.
         * Delete the specified task in taskList.
         * */
        @Override
        public void execute()  {
            if (index < 1 || index > taskList.count()) {
                ui.showError("index out of range");
            } else {
                taskList.deleteTask(index);
            }
        }

        @Override
        public boolean isExit () {
            return false;
        }
    }

    private class ExitCommand extends Command {
        public ExitCommand() { super(); }

        @Override
        public void execute()  {
            ui.exit();
        }

        @Override
        public boolean isExit () {
            return true;
        }
    }

    private class Parser {
        private String list, todo, ddl, event, mark, unmark, exit, delete, find;

        public Parser() {
            list = "list";
            todo = "todo";
            event = "event";
            ddl = "deadline";
            unmark = "unmark";
            mark = "mark";
            exit = "bye";
            delete = "delete";
            find = "find";
        }

        public Command parse(String s) throws DukeException {
            int space = s.indexOf(" ");
            if (s.equals(list)) {
                return new ListCommand();
            } else if (s.equals(exit)) {
                return new ExitCommand();
            } else if (space == -1 || s.substring(0, space) == "") {
                throw new DukeException("Invalid input!");
            } else {
                String identifier = s.substring(0, space);
                String description = s.substring(space + 1);

                if (identifier.equals(todo)) {
                    return new AddCommand(TaskType.TODO, description);
                } else if (identifier.equals(event)) {
                    return new AddCommand(TaskType.EVENT, description);
                }  else if (identifier.equals(ddl)) {
                    return new AddCommand(TaskType.DEADLINE, description);
                } else if (identifier.equals(delete)) {
                    return new DeleteCommand(extractIndex(s));
                } else if (identifier.equals(mark)) {
                    return new MarkCommand(true, extractIndex(s));
                } else if (identifier.equals(unmark)) {
                    return new MarkCommand(false, extractIndex(s));
                } else if (identifier.equals(find)) {
                    return new FindCommand(description);
                } else {
                    throw new DukeException("Cannot find matching key word!");
                }
            }
        }

        public int extractIndex(String s) throws DukeException {
            int pos = s.indexOf(" ");
            if (pos == -1) throw new DukeException("Invalid structure, include the index after mark/unmark");

            int index;
            try {
                index = Integer.parseInt(s.substring(pos + 1));
            } catch (Exception e) {
                throw new DukeException("Invalid argument. only integers are accepted!");
            }
            return index;
        }

        public LocalDate extractDate(String s) throws DukeException {
            int pos = s.indexOf("/by");
            if (pos == -1) pos = s.indexOf("/at");
            if (pos == -1) throw new DukeException("Invalid structure, include /by or /at before the date");

//            String date = ;

            return parseStringToDate(s.substring(pos + 4));
        }

        public String extractDescription(String s) throws DukeException {
            int pos = s.indexOf("/by");
            if (pos == -1) pos = s.indexOf("/at");
            if (pos == -1) throw new DukeException("Invalid structure, include /by or /at before the date");
            return s.substring(0, pos - 1);
        }

        public LocalDate parseStringToDate(String s) {
            return LocalDate.parse(s.replace('/', '-'));
        }

        public String parseDateToString(LocalDate date) {
            return date.format(DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH));

        }

    }

    public void run() {
        try {
            storage.loadData();
        } catch (FileNotFoundException e) {
            ui.showError("file duke.txt not found under the directory!");
        }
        ui.welcome();
        boolean isExit = false;
        while (!isExit) {
            try {
                String fullCommand = ui.readCommand();
                ui.showLine(); // show the divider line ("_______")
                Command c = parser.parse(fullCommand);
                c.execute();
                isExit = c.isExit();
                storage.saveData();
            } catch (Exception e) {
                ui.showError(e.getMessage());
            } finally {
                ui.showLine();
            }
        }
    }

    /**
     * Encapsulates all types of tasks.
     * <p>
     * This class contain the description and type of the task
     */
    public class Task {
        private String todo = "todo";
        private String ddl = "deadline";
        private String event = "event";
        protected String description;
        protected boolean isDone;

        /**
         * Constructor for Task class.
         *
         * @param description description string of the task
         */
        public Task(String description) {
            this.description = description;
            this.isDone = false;
        }

        /**
         * Returns the status icon of a task, representing whether it is done.
         *
         * @return a string containing the status icon
         */
        public String getStatusIcon() {
            return (isDone ? "X" : " "); // mark done task with X
        }

        @Override
        public String toString() {
            return String.format("[%s] %s", getStatusIcon(), description);
        }

        public void markAsDone() {
            isDone = true;
        }

        public void markAsUndone() {
            isDone = false;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Encapsulates deadline tasks.
     * <p>
     * This class contain the description and the due date of the deadline instance.
     */
    public class Deadline extends Task {
        protected LocalDate by;

        /**
         * Constructor for Deadline class.
         *
         * @param description description string of the task
         * @param by a LocalDate instance representing the due date
         */
        public Deadline(String description, LocalDate by) {
            super(description);
            this.by = by;
        }

        @Override
        public String toString() {
            return "[D]" + super.toString() + " (by: " + parser.parseDateToString(by) + ")";
        }
    }

    /**
     * Encapsulates todo tasks.
     * <p>
     * This class contain the description of a todo instance.
     */
    public class Todo extends Task {
        /**
         * Constructor for Todo class.
         *
         * @param description description string of the task
         */
        public Todo(String description) {
            super(description);
        }

        @Override
        public String toString() {
            return "[T]" + super.toString();
        }
    }

    /**
     * Encapsulates event tasks.
     * <p>
     * This class contain the description and the date of the event instance.
     */
    public class Event extends Task {
        private LocalDate at;

        /**
         * Constructor for Event class.
         *
         * @param description description string of the task
         * @parama at a LocalDate instance representing the date of the event
         */
        public Event(String description, LocalDate at) {
            super(description);
            this.at = at;
        }

        @Override
        public String toString() {
            return "[E]" + super.toString() + " (at: " + parser.parseDateToString(at) + ")";
        }
    }

    private class DukeException extends Exception {
        public DukeException(String message) {
            super("OOPS!!" + message);
        }
    }

    public static void main(String[] args) {
        new Duke().run();
    }

}
