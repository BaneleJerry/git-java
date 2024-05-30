import java.io.*;
import java.nio.file.Files;
import java.util.zip.InflaterInputStream;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible
    // when running tests.

    // Uncomment this block to pass the first stage
    final String command = args[0];

    switch (command) {
      case "init" -> {
        final File root = new File(".git");
        new File(root, "objects").mkdirs();
        new File(root, "refs").mkdirs();
        final File head = new File(root, "HEAD");

        try {
          head.createNewFile();
          Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
          System.out.println("Initialized git directory");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      case "cat-file" -> {
        String option = args[1];
        if (option.equalsIgnoreCase("-p")) {
          final String blobSha = args[2];
          catFile(blobSha);
        }
      }
      default -> System.out.println("Unknown command: " + command);
    }
  }

  private static void catFile(String blobSha) {
    String fileName = String.format(".git/objects/%s/%s", blobSha.substring(0, 2), blobSha.substring(2));
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(fileName))))){
      String line = reader.readLine();
      System.out.print(line.substring(line.indexOf('\0') + 1));
      while ((line = reader.readLine()) != null){
        System.out.println(line);
      }
    }catch (IOException e){
      e.printStackTrace();
    }
  }
}
