# Benito Ventas

Backend de ventas desarrollado con Java 21 y Quarkus. Permite registrar clientes, administrar productos, acreditar saldo y crear órdenes de compra.

El proyecto está dividido en dos servicios: `orders-service` recibe las órdenes y `commerce-service` valida la compra, descuenta el saldo y actualiza el inventario. Se comunican por Kafka, por lo que una orden puede quedar pendiente mientras se procesa.

## Contenido

- [Preparar el entorno](#preparar-el-entorno)
- [Levantar los servicios](#levantar-los-servicios)
- [Endpoints](#endpoints)
- [Probar una compra](#probar-una-compra)
- [Administración adicional](#administración-adicional)
- [Compilación](#compilación)
- [Apagado](#apagado)

## Arquitectura

| Componente | Responsabilidad | Dirección local |
|---|---|---|
| `commerce-service` | Autenticación, clientes, depósitos, productos y procesamiento de compras | `https://localhost:8443` |
| `orders-service` | Creación y consulta de órdenes, estados y notas administrativas | `https://localhost:8444` |
| MongoDB 8.0 | Persistencia; replica set `rs0` para transacciones | `localhost:27017`, TLS |
| Kafka 4.1.2 | Intercambio de eventos entre los servicios | `localhost:9092`, mTLS |

Los microservicios se ejecutan en la JVM local. Docker Compose levanta MongoDB y Kafka. Las bases `benito_commerce` y `benito_orders` están separadas lógicamente y comparten la instancia local de MongoDB.

El `201` confirma la creación de la orden; el resultado de negocio se consulta después. El precio se obtiene del catálogo al procesar la compra. El cliente envía productos y cantidades, y el propietario se obtiene del JWT.

### Qué pasa si una operación falla

- El débito, la reducción del inventario y el registro del resultado se ejecutan en una transacción MongoDB.
- Las escrituras incluyen condiciones de saldo e inventario suficientes.
- Una compra procesada se identifica por `orderId`, evitando un segundo cobro al recibir de nuevo el evento.
- Los depósitos son idempotentes por cliente y referencia. Repetir la referencia con otro monto devuelve `409`.
- Ambos servicios conservan eventos pendientes en MongoDB y reintentan su publicación mediante outboxes, cada cinco segundos. Una publicación puede repetirse si el proceso falla después del ACK; los consumidores manejan duplicados.

### Organización del código

```text
benito-ventas/
├── README.md
├── compose.yaml
├── tools/
│   ├── GenerateJwtKeys.java
│   └── GenerateTlsCertificates.ps1
├── commerce-service/
├── orders-service/
└── secrets/                       # Generado localmente; excluido de Git
```

Dentro de `src/main/java/com/benitoventas`: `resources` contiene endpoints; `services`, lógica de negocio; `repositories`, acceso a datos; `entities`, modelos persistidos; `dto`, peticiones y respuestas; `security`, controles del certificado administrativo; y `messaging`, eventos, consumidores, publicadores, deserializadores y outboxes. Comercio incluye además `bootstrap` y `exceptions`.

## Requisitos

Guía preparada para **Windows y PowerShell**:

- JDK **21**, con `java` y `keytool` disponibles en `PATH`.
- Git.
- Docker Desktop iniciado, usando contenedores Linux y Docker Compose.
- Internet para descargar imágenes Docker y dependencias Maven la primera vez.
- Puertos `27017`, `9092`, `8443` y `8444` disponibles.

Se utiliza Quarkus `3.40.1.1`. No hace falta instalar Maven: cada servicio incluye Maven Wrapper.

```powershell
java -version
keytool -help
git --version
docker version
docker compose version
```

## Preparar el entorno

Estos pasos corresponden a una instalación nueva. Si ya existen claves, certificados y datos, conservarlos y continuar con el arranque. Los generadores rechazan sobrescribir archivos existentes.

### 1. Clonar el repositorio

```powershell
git clone https://github.com/JaimeTerrero/benito-ventas-backend.git benito-ventas
Set-Location benito-ventas
```

Ejecutar los pasos siguientes desde esta raíz, que contiene `compose.yaml`.

### 2. Generar claves JWT y certificados locales

```powershell
java tools/GenerateJwtKeys.java
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\GenerateTlsCertificates.ps1
```

Se generan claves JWT RSA y una autoridad local con certificados para los dos servicios, Kafka, MongoDB y el administrador. Los almacenes locales usan la contraseña `local-dev-only`, ya configurada en los servicios y Compose; no es la contraseña de la cuenta administrativa.

Para que PowerShell confíe en los servidores HTTPS, importar **la autoridad generada por esta copia del proyecto** en el almacén del usuario actual:

```powershell
Import-Certificate -FilePath .\secrets\tls\ca.crt -CertStoreLocation Cert:\CurrentUser\Root
```

Esto agrega confianza a la CA local en Windows. No se omite la validación TLS. Las claves privadas y los certificados generados permanecen en `secrets/`, excluido de Git.

### 3. Preparar y levantar la infraestructura

Con Docker Desktop iniciado, ajustar los permisos del volumen nuevo de Kafka y levantar los contenedores:

```powershell
docker compose run --rm --no-deps --user 0 --entrypoint chown kafka -R 1000:1000 /tmp/kraft-combined-logs
docker compose up -d
docker compose ps -a
```

Esperar a que ambos contenedores permanezcan en estado `Up`. Si acaban de arrancar, puede ser necesario esperar unos segundos antes de los comandos siguientes.

### 4. Inicializar el replica set de MongoDB

```powershell
$mongoInit = @'
try {
    printjson(rs.status());
} catch (error) {
    if (error.code === 94) {
        printjson(rs.initiate({ _id: "rs0", members: [{ _id: 0, host: "mongodb:27017" }] }));
    } else {
        throw error;
    }
}
'@

$mongoInit | docker compose exec -T mongodb mongosh --tls --host localhost --tlsCAFile /certs/ca.crt --tlsCertificateKeyFile /certs/mongodb.pem --quiet
```

La inicialización se hace una vez por volumen. El bloque también permite consultar un replica set existente. Esperar unos segundos y verificar que el nodo sea primario:

```powershell
docker compose exec mongodb mongosh --tls --host localhost --tlsCAFile /certs/ca.crt --tlsCertificateKeyFile /certs/mongodb.pem --quiet --eval 'db.hello().isWritablePrimary'
```

Debe devolver `true`. Si devuelve `false`, esperar y repetir esta consulta antes de iniciar los servicios.

### 5. Crear los temas Kafka

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:19092 --command-config /certs/kafka-client.properties --create --if-not-exists --topic orders-created --partitions 1 --replication-factor 1
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:19092 --command-config /certs/kafka-client.properties --create --if-not-exists --topic orders-processed --partitions 1 --replication-factor 1
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:19092 --command-config /certs/kafka-client.properties --list
```

## Levantar los servicios

Usar **tres terminales**: una para comercio, otra para órdenes y una para probar la API. Las variables PowerShell de una terminal no se comparten con las demás.

### Terminal 1: commerce-service

Abrir PowerShell en la raíz del repositorio. En el primer arranque, elegir una contraseña administrativa de al menos 12 caracteres; usar caracteres ASCII evita exceder el límite de 72 bytes de bcrypt.

```powershell
$env:APP_BOOTSTRAP_ADMIN_ENABLED = 'true'
$env:APP_BOOTSTRAP_ADMIN_EMAIL = 'admin@example.com'
$adminSecret = Read-Host 'Elige la contraseña del administrador' -AsSecureString
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $adminSecret).Password

Set-Location .\commerce-service
.\mvnw.cmd quarkus:dev
```

Esperar `Listening on: https://localhost:8443`. La primera ejecución puede tardar por la descarga de dependencias.

El bootstrap crea una cuenta `ADMIN` si no existe. No cambia la contraseña de una cuenta existente ni convierte una cuenta `CLIENTE` en administradora. En arranques posteriores basta con ejecutar, dentro de `commerce-service`:

```powershell
.\mvnw.cmd quarkus:dev
```

Después de detener el primer arranque con `Ctrl+C`, retirar sus variables de esa terminal:

```powershell
Remove-Item Env:APP_BOOTSTRAP_ADMIN_ENABLED -ErrorAction SilentlyContinue
Remove-Item Env:APP_BOOTSTRAP_ADMIN_EMAIL -ErrorAction SilentlyContinue
Remove-Item Env:APP_BOOTSTRAP_ADMIN_PASSWORD -ErrorAction SilentlyContinue
```

### Terminal 2: orders-service

Abrir otra terminal en la raíz del repositorio:

```powershell
Set-Location .\orders-service
.\mvnw.cmd quarkus:dev
```

Esperar `Listening on: https://localhost:8444`.

### Terminal 3: probar la API

Abrir otra terminal en la raíz. Comprobar el catálogo:

```powershell
Invoke-RestMethod -Uri 'https://localhost:8443/products'
```

En una instalación nueva el catálogo estará vacío. Continuar con la [sección de prueba](#probar-una-compra), que crea un cliente, un producto, un depósito y una orden sin depender de datos previos.

## Seguridad

- Registro y catálogo públicos; el registro crea cuentas `CLIENTE` con saldo cero.
- Login con correo y contraseña; contraseñas almacenadas con bcrypt.
- JWT Bearer con vigencia de **900 segundos**, emisor `https://benito-ventas.local` y audiencia `benito-ventas-api`.
- El cliente consulta sus propios datos y órdenes. La identidad de una orden se toma del token.
- Una cuenta `ADMIN` debe presentar también el certificado `admin.p12`, tanto al iniciar sesión como al realizar peticiones autenticadas.
- El servidor HTTPS solicita certificado de cliente; el control administrativo exige la identidad `CN=admin,O=Benito Ventas`.
- Las conexiones de los microservicios a Kafka y MongoDB utilizan TLS con certificados de cliente.

## Endpoints

`Comercio`: `https://localhost:8443`. `Órdenes`: `https://localhost:8444`.

| Servicio | Método | Ruta | Acceso / propósito |
|---|---|---|---|
| Comercio | POST | `/auth/login` | Público; ADMIN requiere certificado |
| Comercio | POST | `/customers` | Público; registrar CLIENTE |
| Comercio | GET | `/customers/{id}` | Propietario o ADMIN |
| Comercio | GET | `/customers?page=0&size=20` | ADMIN; listado |
| Comercio | PUT | `/customers/{id}` | ADMIN; editar nombre y correo |
| Comercio | POST | `/customers/{id}/deposits` | ADMIN; acreditar saldo |
| Comercio | GET | `/products` | Público; productos activos |
| Comercio | GET | `/products/{id}` | Público; producto activo |
| Comercio | POST | `/products` | ADMIN; crear |
| Comercio | PUT | `/products/{id}` | ADMIN; editar nombre, precio y estado |
| Comercio | DELETE | `/products/{id}` | ADMIN; desactivar, devuelve 204 |
| Órdenes | POST | `/orders` | CLIENTE; crear orden propia |
| Órdenes | GET | `/orders/{id}` | Propietario o ADMIN |
| Órdenes | GET | `/orders` | ADMIN; listado con filtros |
| Órdenes | PUT | `/orders/{id}/admin-note` | ADMIN; nota interna |

Todas las llamadas con identidad ADMIN requieren JWT y certificado. Los cuerpos de petición son JSON. Los listados administrativos aceptan `page` desde cero y `size` entre 1 y 100. Órdenes admite además `customerId` y `status` (`PENDIENTE`, `COMPLETADA`, `RECHAZADA`).

## Probar una compra

Ejecutar los bloques en orden, en una misma terminal PowerShell situada en la raíz del repositorio. Mantener ambos microservicios y la infraestructura encendidos. Los ejemplos crean datos nuevos y usan los IDs de las respuestas.

### 1. Iniciar sesión como administrador

Usar la contraseña elegida durante el bootstrap:

```powershell
$commerceUrl = 'https://localhost:8443'
$ordersUrl = 'https://localhost:8444'
$adminCertificate = [System.Security.Cryptography.X509Certificates.X509Certificate2]::new(
    (Join-Path (Get-Location).Path 'secrets\tls\admin.p12'),
    'local-dev-only'
)
$adminSecret = Read-Host 'Contraseña de admin@example.com' -AsSecureString
$adminCredentials = @{
    email = 'admin@example.com'
    password = [System.Net.NetworkCredential]::new('', $adminSecret).Password
} | ConvertTo-Json

$adminLogin = Invoke-RestMethod -Uri "$commerceUrl/auth/login" -Method Post -ContentType 'application/json' -Body $adminCredentials -Certificate $adminCertificate -ErrorAction Stop
$adminHeaders = @{ Authorization = "Bearer $($adminLogin.accessToken)" }
```

Si pasan 15 minutos, repetir las dos últimas asignaciones para obtener un token nuevo. El certificado se carga desde el archivo local; no se comparte ni se sube al repositorio.

### 2. Registrar un cliente

```powershell
$demoEmail = "demo-$([guid]::NewGuid().ToString('N').Substring(0,8))@example.com"
$clientSecret = Read-Host 'Elige la contraseña del cliente (minimo 12 caracteres)' -AsSecureString
$clientPassword = [System.Net.NetworkCredential]::new('', $clientSecret).Password
$registration = @{
    name = 'Cliente de demostracion'
    email = $demoEmail
    password = $clientPassword
} | ConvertTo-Json

$customer = Invoke-RestMethod -Uri "$commerceUrl/customers" -Method Post -ContentType 'application/json' -Body $registration -ErrorAction Stop
$customer
```

Respuesta: `201`, con `id`, `name`, `email`, `balance` igual a cero y `createdAt`. Un correo ya registrado produce `409`.

### 3. Iniciar sesión como cliente

```powershell
$clientCredentials = @{ email = $demoEmail; password = $clientPassword } | ConvertTo-Json
$clientLogin = Invoke-RestMethod -Uri "$commerceUrl/auth/login" -Method Post -ContentType 'application/json' -Body $clientCredentials -ErrorAction Stop
$clientHeaders = @{ Authorization = "Bearer $($clientLogin.accessToken)" }
```

Para renovar el token, repetir las asignaciones de `$clientLogin` y `$clientHeaders`. `accessToken`, `tokenType` y `expiresIn` forman la respuesta del login.

### 4. Crear un producto y acreditar saldo

```powershell
$productBody = @{ name = 'Cafe de demostracion'; price = 150.00; stock = 10 } | ConvertTo-Json
$product = Invoke-RestMethod -Uri "$commerceUrl/products" -Method Post -ContentType 'application/json' -Body $productBody -Headers $adminHeaders -Certificate $adminCertificate -ErrorAction Stop
$product

$depositReference = "demo-$([guid]::NewGuid().ToString('N'))"
$depositBody = @{ reference = $depositReference; amount = 500.00 } | ConvertTo-Json
Invoke-RestMethod -Uri "$commerceUrl/customers/$($customer.id)/deposits" -Method Post -ContentType 'application/json' -Body $depositBody -Headers $adminHeaders -Certificate $adminCertificate -ErrorAction Stop
```

El producto devuelve `id`, `name` y `price`. El depósito devuelve el cliente con saldo `500.00`.

### 5. Crear la orden como cliente

```powershell
$orderBody = @{
    items = @(@{ productId = $product.id; quantity = 1 })
} | ConvertTo-Json -Depth 5

$order = Invoke-RestMethod -Uri "$ordersUrl/orders" -Method Post -ContentType 'application/json' -Body $orderBody -Headers $clientHeaders -ErrorAction Stop
$order
```

No se envía `customerId`; se obtiene del JWT. La respuesta `201` normalmente mostrará `PENDIENTE`.

### 6. Consultar hasta recibir el resultado

```powershell
$finished = $null
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    $finished = Invoke-RestMethod -Uri "$ordersUrl/orders/$($order.id)" -Headers $clientHeaders -ErrorAction Stop
    if ($finished.status -ne 'PENDIENTE') { break }
    Start-Sleep -Seconds 2
}
$finished | ConvertTo-Json -Depth 6

Invoke-RestMethod -Uri "$commerceUrl/customers/$($customer.id)" -Headers $clientHeaders -ErrorAction Stop
```

Sin otras operaciones simultáneas, se espera `COMPLETADA`, total `150.00`, `rejectionReason` nulo y saldo `350.00`. La respuesta incluye `processedAt`. Si sigue pendiente tras el sondeo, revisar ambos servicios y Kafka; no se considera una compra completada.

Un rechazo de negocio se consulta en la orden: `status` igual a `RECHAZADA` y `rejectionReason` con el motivo. La creación puede haber devuelto `201` aunque la compra termine rechazada.

## Administración adicional

Ejecutar estos ejemplos después del recorrido principal. Modifican los datos de demostración.

### Consultar y editar clientes

```powershell
Invoke-RestMethod -Uri "$commerceUrl/customers?page=0&size=20" -Headers $adminHeaders -Certificate $adminCertificate -ErrorAction Stop

$customerUpdate = @{ name = 'Cliente actualizado'; email = $demoEmail } | ConvertTo-Json
Invoke-RestMethod -Uri "$commerceUrl/customers/$($customer.id)" -Method Put -ContentType 'application/json' -Body $customerUpdate -Headers $adminHeaders -Certificate $adminCertificate -ErrorAction Stop
```

La edición no permite cambiar saldo, contraseña ni rol. Si se cambia el correo, usar el correo nuevo en el siguiente login.

### Editar y desactivar el producto

```powershell
$productUpdate = @{ name = 'Cafe actualizado'; price = 155.00; active = $true } | ConvertTo-Json
Invoke-RestMethod -Uri "$commerceUrl/products/$($product.id)" -Method Put -ContentType 'application/json' -Body $productUpdate -Headers $adminHeaders -Certificate $adminCertificate -ErrorAction Stop
```

Para desactivarlo al terminar las pruebas:

```powershell
Invoke-WebRequest -UseBasicParsing -Uri "$commerceUrl/products/$($product.id)" -Method Delete -Headers $adminHeaders -Certificate $adminCertificate -ErrorAction Stop | Select-Object StatusCode
```

Se espera `204`. Después, la consulta pública por ese ID devuelve `404` y las compras nuevas de ese producto se rechazan. No se elimina el documento histórico. `PUT` con `active = $true` permite reactivarlo.

### Listar órdenes y agregar una nota

```powershell
$adminOrders = Invoke-RestMethod -Uri "$ordersUrl/orders?status=COMPLETADA&page=0&size=20" -Headers $adminHeaders -Certificate $adminCertificate -ErrorAction Stop
$adminOrders | ConvertTo-Json -Depth 8

$noteBody = @{ note = 'Compra revisada por administracion.' } | ConvertTo-Json
Invoke-RestMethod -Uri "$ordersUrl/orders/$($order.id)/admin-note" -Method Put -ContentType 'application/json' -Body $noteBody -Headers $adminHeaders -Certificate $adminCertificate -ErrorAction Stop | ConvertTo-Json -Depth 8
```

Las respuestas administrativas contienen `order`, `adminNote`, `updatedBy` y `updatedAt`. La consulta del cliente no expone estos datos internos.

## Compilación

Para compilar y empaquetar, desde la raíz:

```powershell
Push-Location .\commerce-service
.\mvnw.cmd -DskipTests package
Pop-Location

Push-Location .\orders-service
.\mvnw.cmd -DskipTests package
Pop-Location
```

El comando compila y empaqueta, pero no ejecuta tests. Los tests de `/hello` incluidos en el repositorio son los ejemplos iniciales de Quarkus y no cubren el flujo de ventas. El recorrido para probar registro, login, depósitos y compras está en la [sección de prueba](#probar-una-compra).

El procedimiento de ejecución documentado usa el perfil `dev`. La configuración de conexiones y claves contiene valores `%dev`; para ejecutar el JAR en otro perfil se necesita configurar explícitamente esos valores.

## Apagado

Presionar `Ctrl+C` en cada terminal de Quarkus. Desde la raíz:

```powershell
docker compose stop
```

Los volúmenes conservan los datos. Para volver a levantar la infraestructura, usar `docker compose up -d`; no es necesario regenerar claves, reinicializar el replica set ni crear otra vez los temas.

## Tener en consideración

El inventario inicial se define al crear un producto; la edición permite cambiar nombre, precio y estado activo. La API pública de productos devuelve `id`, `name` y `price`. Los saldos se modifican por depósitos administrativos o compras, y los estados finales de órdenes proceden del procesamiento de eventos. Las notas administrativas no modifican ese resultado.
