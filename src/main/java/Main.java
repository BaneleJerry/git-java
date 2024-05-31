import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Main {
  public static void main(String[] args) {
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

      case "hash-object" -> {
        boolean write = false;
        String filepath = "";
        if (args.length == 3 && args[1].equals("-w")) {
          write = true;
          filepath = args[2];
        } else if (args.length == 2) {
          filepath = args[1];
        } else {
          System.out.println("Usage: hash-object [-w] <file>");
          return;
        }
        String sha = computeSha(filepath);
        if (write) {
          writeObject(filepath, sha);
        }
        System.out.println(sha);
      }
      default -> System.out.println("Unknown command: " + command);
    }
  }

  private static void catFile(String blobSha) {
    String fileName = String.format(".git/objects/%s/%s", blobSha.substring(0, 2), blobSha.substring(2));
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(fileName))))) {
      String line = reader.readLine();
      System.out.print(line.substring(line.indexOf('\0') + 1));
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String computeSha(String filepath) {
    File file = new File(filepath);
    if (!file.exists()) {
      System.out.println("ERR: File " + filepath + " does not exist");
      return "";
    }
    try (FileInputStream fileInputStream = new FileInputStream(file)) {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
      byte[] content = Files.readAllBytes(file.toPath());
      String header = "blob " + content.length + "\0";
      messageDigest.update(header.getBytes());
      messageDigest.update(content);

      // Get the hash's bytes
      byte[] hashBytes = messageDigest.digest();

      // Convert hash bytes to hexadecimal string
      StringBuilder sb = new StringBuilder();
      for (byte b : hashBytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void writeObject(String filepath, String sha) {
    File file = new File(filepath);
    try (FileInputStream fileInputStream = new FileInputStream(file)) {
      byte[] content = Files.readAllBytes(file.toPath());
      String header = "blob " + content.length + "\0";
      byte[] headerBytes = header.getBytes();
      byte[] allBytes = new byte[headerBytes.length + content.length];
      System.arraycopy(headerBytes, 0, allBytes, 0, headerBytes.length);
      System.arraycopy(content, 0, allBytes, headerBytes.length, content.length);

      // Compress the data
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      try (DeflaterOutputStream deflater = new DeflaterOutputStream(byteStream)) {
        deflater.write(allBytes);
      }

      // Write to .git/objects directory
      String dirName = ".git/objects/" + sha.substring(0, 2);
      String fileName = sha.substring(2);
      File dir = new File(dirName);
      if (!dir.exists()) {
        dir.mkdirs();
      }
      File objectFile = new File(dir, fileName);
      try (FileOutputStream fileOutputStream = new FileOutputStream(objectFile)) {
        fileOutputStream.write(byteStream.toByteArray());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
