package com.example.sanctions;

import net.openhft.chronicle.map.ChronicleMap;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final int CHUNK_SIZE = 100_000; // number of entries per ChronicleMap

    public static void main(String[] args) {
        String csvFile = "sample_data.csv";       // CSV input file
        String outputDir = "chronicle_maps";      // folder to store ChronicleMap files

        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        List<String[]> csvChunk = new ArrayList<>();
        int maxKeyLength = 0;
        int maxValueLength = 0;
        int fileIndex = 1;
        int totalEntries = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 2); // assuming CSV format: key,value
                if (parts.length != 2) continue;

                csvChunk.add(parts);
                maxKeyLength = Math.max(maxKeyLength, parts[0].getBytes().length);
                maxValueLength = Math.max(maxValueLength, parts[1].getBytes().length);
                totalEntries++;

                // When chunk reaches CHUNK_SIZE, save it to a ChronicleMap
                if (csvChunk.size() >= CHUNK_SIZE) {
                    createChronicleMap(csvChunk, maxKeyLength, maxValueLength, outputDir, fileIndex);
                    fileIndex++;
                    csvChunk.clear();
                    maxKeyLength = 0;
                    maxValueLength = 0;
                }
            }

            // Save remaining entries
            if (!csvChunk.isEmpty()) {
                createChronicleMap(csvChunk, maxKeyLength, maxValueLength, outputDir, fileIndex);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Finished processing " + totalEntries + " CSV entries into ChronicleMaps.");
    }

    private static void createChronicleMap(List<String[]> data, int maxKeyLength, int maxValueLength,
                                           String outputDir, int index) {
        String fileName = outputDir + File.separator + "sample_data_part" + index + ".dat";
        try (ChronicleMap<String, String> map = ChronicleMap
                .of(String.class, String.class)
                .name("sample-map-" + index)
                .entries(data.size())
                .averageKeySize(maxKeyLength)
                .averageValueSize(maxValueLength)
                .maxBloatFactor(4.0)
                .createPersistedTo(new File(fileName))) {

            for (String[] parts : data) {
                map.put(parts[0], parts[1]);
            }
            System.out.println("Created ChronicleMap file: " + fileName + " with " + data.size() + " entries");
        } catch (IOException e) {
            System.err.println("Error creating ChronicleMap file: " + fileName);
            e.printStackTrace();
        }
    }
}
