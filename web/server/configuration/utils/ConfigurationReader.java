package web.server.configuration.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationReader {
    /**
     * Reads a file and returns a list of lines from the file, ignoring blank lines and lines starting with a '#'.
     * @param filepath Path of file to read.
     * @return A list of lines (strings) from the file, excluding blank lines and comments (lines starting with '#').
     * @throws IOException
     */
    public static List<String> readConfiguration(String filepath) throws IOException {
        List<String> config = new ArrayList<>();
        File file = new File(filepath);
        try {
            //try brackets closes reader when finished
            try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
                String nextLine;
                while ((nextLine = reader.readLine()) != null) {
                    nextLine = nextLine.trim();
                    // Ignore blank lines and comments
                    if (!nextLine.isBlank() && !nextLine.startsWith("#")) {
                        config.add(nextLine);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.printf("Could not open configuration file %s%n", file.getAbsolutePath());
            e.printStackTrace();
        }
        return config;
    }
}
