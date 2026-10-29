param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$StorePassword = 'local-dev-only'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
if (-not (Test-Path -LiteralPath (Join-Path $ProjectRoot 'compose.yaml'))) {
    throw 'ProjectRoot debe ser la carpeta benito-ventas que contiene compose.yaml.'
}
if ($StorePassword.Length -lt 6) { throw 'La clave del almacen requiere al menos 6 caracteres.' }
$Keytool = (Get-Command keytool -ErrorAction Stop).Source
$Java = (Get-Command java -ErrorAction Stop).Source
$TlsRoot = Join-Path $ProjectRoot 'secrets/tls'
if (Test-Path -LiteralPath $TlsRoot) {
    throw 'secrets/tls ya existe. No se sobrescribiran certificados existentes.'
}
New-Item -ItemType Directory -Path $TlsRoot -Force | Out-Null

function Invoke-Keytool {
    param([string[]]$ToolArguments)
    & $Keytool @ToolArguments
    if ($LASTEXITCODE -ne 0) { throw 'keytool fallo; revise el mensaje anterior.' }
}

$CaStore = Join-Path $TlsRoot 'ca.p12'
$CaCertificate = Join-Path $TlsRoot 'ca.crt'
$TrustStore = Join-Path $TlsRoot 'truststore.p12'

Invoke-Keytool -ToolArguments @(
    '-genkeypair', '-alias', 'ca', '-keyalg', 'RSA', '-keysize', '3072',
    '-dname', 'CN=Benito Ventas Local CA,O=Benito Ventas', '-validity', '3650',
    '-ext', 'BC=ca:true,pathlen:0', '-ext', 'KU=keyCertSign,cRLSign',
    '-storetype', 'PKCS12', '-keystore', $CaStore,
    '-storepass', $StorePassword, '-keypass', $StorePassword, '-noprompt'
)
Invoke-Keytool -ToolArguments @(
    '-exportcert', '-rfc', '-alias', 'ca', '-keystore', $CaStore,
    '-storepass', $StorePassword, '-file', $CaCertificate
)
Invoke-Keytool -ToolArguments @(
    '-importcert', '-noprompt', '-alias', 'ca', '-file', $CaCertificate,
    '-storetype', 'PKCS12', '-keystore', $TrustStore, '-storepass', $StorePassword
)

$Identities = @('commerce-service', 'orders-service', 'kafka', 'mongodb', 'admin')
foreach ($Identity in $Identities) {
    $Store = Join-Path $TlsRoot "$Identity.p12"
    $Csr = Join-Path $TlsRoot "$Identity.csr"
    $Certificate = Join-Path $TlsRoot "$Identity.crt"
    $San = "SAN=dns:localhost,ip:127.0.0.1,dns:$Identity"
    $Eku = if ($Identity -eq 'admin') { 'EKU=clientAuth' } else { 'EKU=serverAuth,clientAuth' }

    Invoke-Keytool -ToolArguments @(
        '-genkeypair', '-alias', $Identity, '-keyalg', 'RSA', '-keysize', '2048',
        '-dname', "CN=$Identity,O=Benito Ventas", '-validity', '365',
        '-storetype', 'PKCS12', '-keystore', $Store,
        '-storepass', $StorePassword, '-keypass', $StorePassword, '-noprompt'
    )
    Invoke-Keytool -ToolArguments @(
        '-certreq', '-alias', $Identity, '-keystore', $Store,
        '-storepass', $StorePassword, '-file', $Csr
    )
    Invoke-Keytool -ToolArguments @(
        '-gencert', '-rfc', '-alias', 'ca', '-keystore', $CaStore,
        '-storepass', $StorePassword, '-infile', $Csr, '-outfile', $Certificate,
        '-validity', '365', '-ext', 'BC=ca:false',
        '-ext', 'KU=digitalSignature,keyEncipherment', '-ext', $Eku, '-ext', $San
    )
    Invoke-Keytool -ToolArguments @(
        '-importcert', '-noprompt', '-alias', 'ca', '-file', $CaCertificate,
        '-keystore', $Store, '-storepass', $StorePassword
    )
    Invoke-Keytool -ToolArguments @(
        '-importcert', '-noprompt', '-alias', $Identity, '-file', $Certificate,
        '-keystore', $Store, '-storepass', $StorePassword
    )
}

# Exporta PEM para MongoDB y herramientas que no leen PKCS12 directamente.
$Exporter = Join-Path $TlsRoot 'ExportTlsPem.java'
$ExporterSource = @'
import java.nio.file.*;
import java.security.KeyStore;
import java.util.Base64;

public class ExportTlsPem {
    private static String pem(String type, byte[] data) {
        return "-----BEGIN " + type + "-----\n"
            + Base64.getMimeEncoder(64, new byte[]{10}).encodeToString(data)
            + "\n-----END " + type + "-----\n";
    }
    public static void main(String[] args) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        char[] password = args[1].toCharArray();
        try (var input = Files.newInputStream(Path.of(args[0]))) {
            store.load(input, password);
        }
        var privateKey = store.getKey(args[2], password);
        StringBuilder text = new StringBuilder(pem("PRIVATE KEY", privateKey.getEncoded()));
        for (var certificate : store.getCertificateChain(args[2])) {
            text.append(pem("CERTIFICATE", certificate.getEncoded()));
        }
        Files.writeString(Path.of(args[3]), text, StandardOpenOption.CREATE_NEW);
    }
}
'@
[IO.File]::WriteAllText($Exporter, $ExporterSource)
foreach ($Identity in $Identities) {
    & $Java $Exporter (Join-Path $TlsRoot "$Identity.p12") $StorePassword $Identity (Join-Path $TlsRoot "$Identity.pem")
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la exportacion PEM.' }
}

$ClientProperties = @"
security.protocol=SSL
ssl.truststore.type=PKCS12
ssl.truststore.location=/certs/truststore.p12
ssl.truststore.password=$StorePassword
ssl.keystore.type=PKCS12
ssl.keystore.location=/certs/kafka.p12
ssl.keystore.password=$StorePassword
ssl.key.password=$StorePassword
ssl.endpoint.identification.algorithm=https
"@
[IO.File]::WriteAllText((Join-Path $TlsRoot 'kafka-client.properties'), $ClientProperties)

Write-Host 'Certificados creados en secrets/tls. No se modificaron JWT, servicios ni contenedores.'
Write-Host 'La clave predeterminada local-dev-only es exclusivamente para estos almacenes de desarrollo.'
