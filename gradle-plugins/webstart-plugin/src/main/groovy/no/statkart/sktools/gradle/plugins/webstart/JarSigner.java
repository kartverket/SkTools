package no.statkart.sktools.gradle.plugins.webstart;

import no.statkart.sktools.gradle.plugins.webstart.util.FileHashIdent;
import org.gradle.api.Action;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Steg for signering av alle jar avhengigheter.
 *
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
public class JarSigner extends ConventionTask {
    private File cacheDir;
    /**
     * Holder cache over alle signerte filer, et map per sertifikat.
     * <p/>
     * String er her en representasjon av jarfilen som typiskt inneholder navn, group og version.
     */
    private Map<FileHashIdent, Map<String, FileHashIdent>> signedArtifactsForCertificates;

    private File certificateFile;
    private String password;
    private String alias;
    private String digestAlgorithm;
    /**
     * Keystore type. Standard verdi avhenger av JDK og security properties.
     * <br> For JDK 8 er det {@literal JKS med PKCS12 i kompatibilitetsmodus} som er standard.
     * <br> For JDK 7 er det {@literal JKS} som er standard.
     * <p>Standard verdi fra jdk fås fra {@link java.security.KeyStore#getDefaultType()}.
     * Denne property verdien leses ifra {@literal JAVA_HOME/jre/lib/security/java.security} filen på windows.
     * <ul>
     * <li>{@literal keystore.type=jks} bestemmer standard format til keytore dersom ikke definert</li>
     * <li>{@literal keystore.type.compat=true} When set to 'true', the JKS keystore type supports loading keystore files in either JKS or PKCS12 format.</li>
     * </ul>
     */
    private String storetype;

    private final Map<String, String> manifestAttributes = new LinkedHashMap<>();

    private final ConfigurableFileCollection jarFilesToSign = getProject().files();
    private final FileTree signedJarFiles;
    private final File signedJarFilesDir;

    public Map<FileHashIdent, Map<String, FileHashIdent>> getSignedArtifactsForCertificates() {
        if (signedArtifactsForCertificates == null) {
            try {
                initCache();
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize cache of signed jars", e);
            }
        }

        return signedArtifactsForCertificates;
    }

    public JarSigner() {
        super();
        signedJarFilesDir = new File(new File(getProject().getBuildDir(), "signedJars"), getName());
        signedJarFiles = getProject().files(signedJarFilesDir).getAsFileTree();
    }

    /**
     * Signerer jar filer, eller henter cachede filer basert på lagrede hashverdier.
     * Filer blir kopiert til {@link #getCacheDir()} med undermappe lik hashverdi til sertifikatet
     * </br>
     */
    @TaskAction
    public void signJars() throws Exception {
        if (getCertificateFile() != null) {
            //sertifikat ident er streng representasjon av hash verdi + alias  - dette danner da mappenavn i cache dir
            FileHashIdent certificateFileIdent = getCertificateHashIdent();

            File certDirectory = getCertificateCacheDir(certificateFileIdent);
            certDirectory.mkdirs();

            Map<String, FileHashIdent> signedArtifacts = getSignedArtifactsForCertificates().get(certificateFileIdent);
            if (signedArtifacts == null) {
                signedArtifacts = new HashMap<>();
                getSignedArtifactsForCertificates().put(certificateFileIdent, signedArtifacts);
            }

            File manifestAddendum = null; //temp-file for additional manifest attributes (this is passed on to the jar tool)
            if (!getManifestAttributes().isEmpty()) {
                manifestAddendum = new File(getTemporaryDir(), "addendum.mf");

                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(manifestAddendum, false), "UTF-8"))) {
                    for (Map.Entry<String, String> entry : getManifestAttributes().entrySet()) {
                        writer.format("%s: %s\n", entry.getKey(), entry.getValue());
                    }
                }
            }

            Set<File> filesToSign = getJarFilesToSign().getFiles();
            for (File unsignedJar : filesToSign) {
                FileHashIdent jarFileIdent = new FileHashIdent(unsignedJar);

                File signedJarFile = null;

                //forsøker cache
                FileHashIdent cachedFileIdent = signedArtifacts.get(unsignedJar.getName());
                if (cachedFileIdent != null) {
                    if (cachedFileIdent.equals(jarFileIdent)) {
                        signedJarFile = cachedFileIdent.getFile();
                        getLogger().info("...using cached jar " + signedJarFile.getAbsolutePath());
                    }
                }

                //dersom fortsatt ikke funnet - signer filen
                if (signedJarFile == null) {

                    File tempFile = new File(certDirectory, unsignedJar.getName() + ".tmp");
                    signedJarFile = new File(certDirectory, unsignedJar.getName());


                    //kopiere via URL for å overkomme evt symlink filer.
                    Files.copy(unsignedJar.toPath(), tempFile.toPath());

                    //signing jar
                    try {
                        signJar(tempFile, manifestAddendum);

                        Files.move(tempFile.toPath(), signedJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    } finally {
                        if (tempFile.exists()) {
                            tempFile.delete();
                        }
                    }

                    //updating cache...
                    if (cachedFileIdent != null) {
                        getLogger().debug("updating cache-entry for " + unsignedJar);
                    }
                    cachedFileIdent = new FileHashIdent(signedJarFile, jarFileIdent.hash());
                    cachedFileIdent.writeChecksumToFile(new File(certDirectory, signedJarFile.getName() + ".md5"));
                    signedArtifacts.put(unsignedJar.getName(), cachedFileIdent);
                }

                //legger til signert fil i eksekvering av task - blir også lagt til ved beregning av output-filer
                signedJarFilesDir.mkdirs();
                Files.copy(signedJarFile.toPath(), new File(signedJarFilesDir, signedJarFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);

                //kan ikke bruke symlink pga kartverkets UAC policy -- kan evt kjøres med admin privilegier
                // se også https://stackoverflow.com/questions/23217460/how-to-create-soft-symbolic-link-using-java-nio-files
//                Files.createSymbolicLink(new File(signedJarFilesDir, signedJarFile.getName()).toPath(), signedJarFile.toPath());
            }
        } else {
            getLogger().warn("Signing of resources disabled - no certificate!");
        }
    }

    private void signJar(final File jarFile, final File manifestAddendum) {

        if (manifestAddendum != null) {
            getLogger().info(String.format("Updating manifest for %s ...", jarFile.getPath()));

            getProject().exec(new Action<ExecSpec>() {
                @Override
                public void execute(ExecSpec execSpec) {

                    execSpec.setWorkingDir(jarFile.getParent());
                    execSpec.setIgnoreExitValue(false);
                    execSpec.setExecutable("jar");
                    execSpec.args("ufm", jarFile.getName(), manifestAddendum.getAbsolutePath());   //updating existing archive, specify archive file name, include manifest information from specified manifest file
                }
            });
        } else {
            getLogger().debug("No additional manifest attributes specified. Using original manifest...");
        }

        getLogger().lifecycle(String.format("Signing file %s ...", getProject().relativePath(jarFile)));

        getProject().exec(new Action<ExecSpec>() {
            @Override
            public void execute(ExecSpec execSpec) {
                execSpec.setWorkingDir(jarFile.getParentFile());
                execSpec.setIgnoreExitValue(false);
                execSpec.setExecutable("jarsigner");
                execSpec.args(
                        "-keystore", getCertificateFile().getAbsolutePath(),
                        "-storepass", getPassword(),
                        jarFile.getName(),    //file to sign in same dir - see ...setDir(File)
                        getAlias()
                );

                if (getDigestAlgorithm() != null) {
                    execSpec.args("-digestalg", getDigestAlgorithm());
                }

                if (getStoretype() != null) {
                    execSpec.args("-storetype", getStoretype());
                }

                if (getLogger().isInfoEnabled()) {
                    execSpec.args("-verbose");
                }
            }
        });

    }

    @InputFile
    @Optional //signeringssteg er optional, derfor optional her
    @SkipWhenEmpty
    public File getCertificateFile() {
        return certificateFile;
    }

    public void setCertificateFile(File certificateFile) {
        this.certificateFile = certificateFile;
    }


    @Internal
    public FileHashIdent getCertificateHashIdent() throws Exception {
        return new FileHashIdent(getCertificateFile(), FileHashIdent.createChecksum(getCertificateFile(), getAlias()));
    }

    @Internal
    public File getCertificateCacheDir(FileHashIdent certificateFileIdent) throws Exception {
        return new File(getCacheDir(), certificateFileIdent.hash());
    }

    @Internal
    public File getCacheDir() {
        if (cacheDir == null) {
            cacheDir = new File(getProject().getBuildDir(), "sign-cache");
        }
        return cacheDir;
    }

    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    @Input
    @Optional //signeringssteg er optional, derfor optional her
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Input
    @Optional
    public String getStoretype() {
        return storetype;
    }

    public void setStoretype(String storetype) {
        this.storetype = storetype;
    }

    @Input
    @Optional //signeringssteg er optional, derfor optional her
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Input
    @Optional
    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    @Input
    public Map<String, String> getManifestAttributes() {
        return manifestAttributes;
    }

    public void setManifestAttributes(Map<String, String> attributes) {
        manifestAttributes.clear();
        manifestAttributes.putAll(attributes);
    }

    public void manifestAttributes(Map<String, String> attributes) {
        manifestAttributes.putAll(attributes);
    }

    public void manifestAttribute(String name, String value) {
        manifestAttributes.put(name, value);
    }

    void initCache() throws IOException {
        signedArtifactsForCertificates = new HashMap<>();
        getLogger().info("Initializing cache...");

        if (getCacheDir().exists()) {
            for (File certDirectory : cacheDir.listFiles()) {
                if (certDirectory.isDirectory()) {
                    FileHashIdent certFileIdent = new FileHashIdent(certDirectory, certDirectory.getName());

                    getLogger().debug(String.format("Found cache-dir for certificate with hash = %s", certDirectory.getName()));

                    Map<String, FileHashIdent> signedArtifacts = signedArtifactsForCertificates.get(certFileIdent);
                    if (signedArtifacts == null) {
                        signedArtifacts = new HashMap<>();
                        signedArtifactsForCertificates.put(certFileIdent, signedArtifacts);
                    }

                    for (File md5File : certDirectory.listFiles()) {
                        if (md5File.isFile() && md5File.getName().endsWith(".md5")) {
                            FileHashIdent signedArtifactFileIdent = FileHashIdent.fileHashIdentFromChecksumFile(md5File);
                            if (signedArtifactFileIdent != null) {
                                String unsignedFileName = signedArtifactFileIdent.getFile().getName();
                                signedArtifacts.put(unsignedFileName, signedArtifactFileIdent);
                                getLogger().debug(String.format("   found cached file %s", signedArtifactFileIdent.getFile().getAbsolutePath()));
                            }
                        }
                    }
                }
            }
        } else {
            getLogger().info("  Cachedir does not exist!");
        }
        getLogger().info("...cache initialized!");
    }

    @InputFiles
    @SkipWhenEmpty
    public FileCollection getJarFilesToSign() {
        return jarFilesToSign;
    }

    public void setJarFilesToSign(Object... jarFiles) {
        jarFilesToSign.from(jarFiles);
    }

    /**
     * Dersom signering returneres ferdig signerte filer ifra cache-katalog. <br>
     * Dersom signering ikke er satt opp returneres {@link #jarFilesToSign}
     */
    @OutputFiles
    public FileCollection getJarFiles() {
        return signedJarFiles;
    }


}
