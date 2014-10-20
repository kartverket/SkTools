package no.statkart.sktools.gradle.plugins.webstart.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.security.MessageDigest;

/**
 * Helper class for identification and maintaining hash codes of files.
 */
public class FileHashIdent implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient File file;
    private String hash;

    public FileHashIdent(File file) {
        this.file = file;
    }
    public FileHashIdent(File file, String hash) {
        this(file);
        this.hash = hash;
    }

    public static String createChecksum(File file, String... beacon) throws Exception {
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


        return String.valueOf(Hex.encodeHex(complete.digest()));
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
        FileUtils.writeStringToFile(md5File, hash);
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
            md5 = FileUtils.readFileToString(md5File);

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
