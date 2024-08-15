import java.io.*;
import java.util.*;

public class FileSimilarityConcorrent {

    static Map<String, List<Long>> fileFingerprints = new HashMap<>();

    public synchronized static void addNewFileMap(String path, List<Long> listCount) {
        fileFingerprints.put(path, listCount);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Sum filepath1 filepath2 filepathN");
            System.exit(1);
        }
        long timetemp = System.currentTimeMillis();

        // Create a map to store the fingerprint for each file
        //Map<String, List<Long>> fileFingerprints = new HashMap<>();

        List<SumThread> sumThreadList = new ArrayList<>();
        List<SimilarityThread> similarityThreadsList = new ArrayList<>();

        // Calculate the fingerprint for each file
        for (String path : args) {
            SumThread sumThread = new SumThread(path);
            sumThread.start();
            sumThreadList.add(sumThread);
        }
        for(SumThread sumThread : sumThreadList) {
            sumThread.join();
        }

        System.out.println("Tempo 1: " + (System.currentTimeMillis() - (timetemp)));

        // Compare each pair of files
        for (int i = 0; i < args.length; i++) {
            for (int j = i + 1; j < args.length; j++) {

                String file1 = args[i];
                String file2 = args[j];
                List<Long> fingerprint1 = fileFingerprints.get(file1);
                List<Long> fingerprint2 = fileFingerprints.get(file2);
                SimilarityThread similarityThread = new SimilarityThread(fingerprint1, fingerprint2, file1, file2);
                similarityThread.start();
                similarityThreadsList.add(similarityThread);
                //float similarityScore = similarity(fingerprint1, fingerprint2);

            }
        }
        for (SimilarityThread similarityThread : similarityThreadsList)
            similarityThread.join();

        System.out.println("Tempo 2: " + (System.currentTimeMillis() - (timetemp)));
    }

    static class SimilarityThread extends Thread {

        private final String fileName1;

        private final String fileName2;

        private final List<Long> fingerprint1;

        private final List<Long> fingerprint2;


        SimilarityThread(List<Long> fingerprint1, List<Long> fingerprint2, String fileName1, String fileName2) {
            this.fingerprint1 = fingerprint1;
            this.fingerprint2 = fingerprint2;
            this.fileName1 = fileName1;
            this.fileName2 = fileName2;
        }

        @Override
        public void run(){
            float similarityScore = similarity(this.fingerprint1, this.fingerprint2);
            System.out.println("Similarity between " + fileName1 + " and " + fileName2 + ": " + (similarityScore * 100) + "%");
        }
    }

    static class SumThread extends Thread {

        private final String filePath;

        public SumThread(String filePath){
            this.filePath = filePath;
        }

        @Override
        public void run(){
            try {
                addNewFileMap(this.filePath, fileSum(filePath));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static List<Long> fileSum(String filePath) throws IOException {
        File file = new File(filePath);
        List<Long> chunks = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[100];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                long sum = sum(buffer, bytesRead);
                chunks.add(sum);
            }
        }
        return chunks;
    }

    private static long sum(byte[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Byte.toUnsignedInt(buffer[i]);
        }
        return sum;
    }

    private static float similarity(List<Long> base, List<Long> target) {
        int counter = 0;
        List<Long> targetCopy = new ArrayList<>(target);

        for (Long value : base) {
            if (targetCopy.contains(value)) {
                counter++;
                targetCopy.remove(value);
            }
        }

        return (float) counter / base.size();
    }
}
