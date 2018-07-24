package no.statkart.sktools.gradle.plugins.webstart.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;

/**
 * Helper class for identification and maintaining hash codes of files.
 */
public class FileHashIdent {
    private transient File file;
    private String hash;

    public FileHashIdent(File file) {
        this.file = file;
    }
    public FileHashIdent(File file, String hash) {
        this(file);
        this.hash = hash;
    }

    public static String hexEncoded(byte[] digest) {
        StringBuilder hexString = new StringBuilder();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0xFF & digest[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String createChecksum(File file, String... beacon) throws Exception {
        byte[] digest = createDigest(file, beacon);
        return hexEncoded(digest);
    }

    public static byte[] createDigest(File file, String[] beacon) throws Exception {
        InputStream fis = new FileInputStream(file.getCanonicalFile());

        byte[] buffer = new byte[1024];

        MessageDigest complete = MessageDigest.getInstance("MD5");

        // leser inn eventuelle beacons
        if (beacon != null) {
            for (String s : beacon) {
                complete.update(s.getBytes());
            }
        }

        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();


        return complete.digest();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileHashIdent) {
            FileHashIdent that = (FileHashIdent) obj;
            try {
                return this.hash().equals(that.hash());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        try {
            return hash().hashCode();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String hash() throws Exception {
        if (hash == null) {
            hash = createChecksum(file);
        }
        return hash;
    }

    public File getFile() {
        return file;
    }

    /**
     * Updates (or creates) a checksum file for jar.
     * File can later be used to re-establish a cache of jar files.
     *
     * @see #fileHashIdentFromChecksumFile(java.io.File)
     */
    public FileHashIdent writeChecksumToFile() throws IOException {
        File md5File = new File(file.getParent(), file.getName() + ".md5");
        Files.write(md5File.toPath(), hash.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * Creates a fileIdent based on stored cache data
     * @see FileHashIdent#writeChecksumToFile()
     *
     * @return {@code null} if corresponding files are not found
     */
    public static FileHashIdent fileHashIdentFromChecksumFile(File md5File) throws IOException {
        FileHashIdent signedArtifactFileIdent = null;
        String md5 = null;
        try {
            md5 = new String(Files.readAllBytes(md5File.toPath()), StandardCharsets.UTF_8);

            String jarFilename = md5File.getName().substring(0, md5File.getName().length()-4);
            File jarFile = new File(md5File.getParentFile(), jarFilename);
            if (jarFile.exists() && jarFile.isFile()) {
                signedArtifactFileIdent = new FileHashIdent(jarFile, md5);
            }
        } catch (Exception e) {
            //file not exists
        }
        return signedArtifactFileIdent;
    }

}
