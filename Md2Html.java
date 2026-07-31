package md2html;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Md2Html {

    private static final HashMap<String, Integer> tagsHashMap = new HashMap<>(Map.of(
            "*", -1,
            "_", -1,
            "**", -1,
            "__", -1,
            "--", -1,
            "`", -1,
            "!!", -1
    ));

    private static final Map<Character, String> specialCharMap = (Map.of(
            '<', "&lt;",
            '>', "&gt;",
            '&', "&amp;"
    ));

    public static void main(String[] args) {

        if (args.length < 2) {
            throw new IllegalArgumentException("Not enough values " + args.length + "/2");
        }
        String markdownFile = args[0];
        String htmlFile = args[1];

        parse(markdownFile, htmlFile);

    }

    public static void parse(String markdownFile, String htmlFile) {
        try (BufferedReader reader = openFileReader(markdownFile)) {

            String readedLine;
            char markedchar;
            StringBuilder lineBuilder = new StringBuilder();

            while (reader.ready()) {
                readedLine = reader.readLine();
                // Note isEmpty
                if (readedLine.isEmpty()) {
                    continue;

                }
                markedchar = readedLine.charAt(0);
                if (markedchar == '#') {
                    writeHeader(readedLine, lineBuilder, reader);
                } else {
                    writeParagraph(readedLine, lineBuilder, reader);
                }

                lineBuilder.append(System.lineSeparator());
            }
            try (BufferedWriter writer = openFileWriter(htmlFile)) {
                writer.write(lineBuilder.toString());
            } catch (IOException e) {
                System.err.println("Something wrong with .html file");
            }

        } catch (FileNotFoundException e) {
            System.err.println(".md file not found");
        } catch (IOException e) {
            System.err.println("Something wrong with .md file");
        }

    }

    private static int checkHeaderLevel(String line) {
        int level = 1;

        while (line.charAt(level) != ' ') {

            if (line.charAt(level) != '#') {
                return -1;
            }
            level++;
        }
        return level;
    }

    private static void writeParagraph(String line, StringBuilder lineBuilder, BufferedReader reader) throws IOException {

        lineBuilder.append(getTag("", false));
        writeTextFromContainer(line, lineBuilder, reader);
        lineBuilder.append(getTag("", true));
    }

    private static void writeHeader(String line, StringBuilder lineBuilder, BufferedReader reader) throws IOException {

        int headerLevel = checkHeaderLevel(line);
        if (headerLevel == -1) {
            writeParagraph(line, lineBuilder, reader);
        } else {
            writeHeaderTag(headerLevel, lineBuilder, false);

            writeTextFromContainer(line.substring(headerLevel + 1), lineBuilder, reader);
            writeHeaderTag(headerLevel, lineBuilder, true);
        }
    }

    private static void writeTextFromContainer(String line, StringBuilder lineBuilder, BufferedReader reader) throws IOException {
        String highlighter;
        while (true) {
            for (int i = 0; i < line.length(); i++) {
                if (!isReflected(line, i) && isHighlighter(line, i)) {
                    if (isDoubleTag(line, i)) {
                        highlighter = line.substring(i, i + 2);
                        i++;
                    } else {
                        highlighter = line.substring(i, i + 1);
                    }
                    updateHashMap(highlighter, lineBuilder);
                } else if (isSpecialChar(line.charAt(i))) {
                    lineBuilder.append(specialCharMap.get(line.charAt(i)));
                } else if (!isReflected(line, i + 1)) {
                    lineBuilder.append(line.charAt(i));

                }
            }
            if (checkEndContainer(reader)) {
                resetHashMap();
                break;
            }
            lineBuilder.append(System.lineSeparator());
            line = reNewString(reader);
        }
    }

    private static String getTag(String highlighter, boolean endTag) {
        return switch (highlighter) {
            case "*" -> endTag ? "</em>" : "<em>";
            case "_" -> endTag ? "</em>" : "<em>";
            case "**" -> endTag ? "</strong>" : "<strong>";
            case "__" -> endTag ? "</strong>" : "<strong>";
            case "--" -> endTag ? "</s>" : "<s>";
            case "`" -> endTag ? "</code>" : "<code>";
            case "!!" -> endTag ? "</samp>" : "<samp>";
            default -> endTag ? "</p>" : "<p>";
        };
    }

    private static String reNewString(BufferedReader reader) throws IOException {
        return reader.readLine();
    }

    private static BufferedReader openFileReader(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8)
        );
        return reader;
    }

    private static BufferedWriter openFileWriter(String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8)
        );
        return writer;
    }

    private static boolean isSpecialChar(char c) {
        return c == '<' || c == '>' || c == '&';
    }

    private static void resetHashMap() {
        tagsHashMap.replaceAll((_, _) -> -1);
    }

    private static boolean checkEndContainer(BufferedReader reader) throws IOException {
        if (reader.ready()) {
            reader.mark(System.lineSeparator().length());
            int currentchar;
            for (int i = 0; i < System.lineSeparator().length(); i++) {
                currentchar = reader.read();
                if (currentchar == -1 || (char) currentchar != System.lineSeparator().charAt(i)) {
                    reader.reset();
                    return false;
                }
            }
        }
        return true;

    }

    private static boolean isReflected(String line, int index) {
        return (index != 0 && line.charAt(index - 1) == '\\')
                && ((line.charAt(index) == '*' || line.charAt(index) == '_' || line.charAt(index) == '!'));
    }

    private static boolean isDoubleTag(String line, int index) {
        return (index < line.length() - 1) && (line.charAt(index) == line.charAt(index + 1));
    }

    private static void updateHashMap(String highlighter, StringBuilder lineBuilder) {
        if (tagsHashMap.get(highlighter) == -1) {
            tagsHashMap.put(highlighter, lineBuilder.length());
            lineBuilder.append(highlighter);
        } else {
            lineBuilder.replace(tagsHashMap.get(highlighter), tagsHashMap.get(highlighter) + highlighter.length(),
                    getTag(highlighter, false));
            tagsHashMap.put(highlighter, -1);
            lineBuilder.append(getTag(highlighter, true));
        }
    }

    private static boolean isHighlighter(String line, int index) {
        return (tagsHashMap.containsKey(line.substring(index, index + 1))
                || (index < line.length() - 1 && tagsHashMap.containsKey(line.substring(index, index + 2))));
    }

    private static void writeHeaderTag(int headerLevel, StringBuilder lineBuilder, boolean endTag) {
        lineBuilder.append("<");
        if (endTag) {
            lineBuilder.append("/");
        }
        lineBuilder.append("h");
        lineBuilder.append(headerLevel);
        lineBuilder.append(">");
    }

}
