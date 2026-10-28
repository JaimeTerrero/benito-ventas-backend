import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class GenerateJwtKeys {

    public static void main(String[] args) throws Exception{
        Path directory = Path.of("secrets");
        Path privateFile = directory.resolve("privateKey.pem");
        Path publicFile = directory.resolve("publicKey.pem");
        
        if (Files.exists(privateFile) || Files.exists(publicFile)) {
            throw new IllegalStateException("Ya existen claves. No se sobrescribiran.");
        }

        Files.createDirectories(directory);

        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);

        var pair = generator.generateKeyPair();

        Files.writeString(privateFile, toPem("PRIVATE KEY", pair.getPrivate().getEncoded()), StandardOpenOption.CREATE_NEW);

        Files.writeString(publicFile, toPem("PUBLIC KEY", pair.getPublic().getEncoded()), StandardOpenOption.CREATE_NEW);

        System.out.println("Claves creadas dentro de secrets.");
    }

    private static String toPem(String type, byte[] encoded) {
        String content = Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(encoded);

        return "-----BEGIN " + type + "-----\n"
                + content
                + "\n-----END " + type + "-----\n";
    }
}
