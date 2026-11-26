# Cero Desperdicio

Aplicación Android en Java y XML para que una panadería publique excedentes y un cliente los reserve y recoja. Esta versión funciona con SQLite en **un mismo dispositivo**. Las cuentas y publicaciones no se sincronizan entre teléfonos.

## Ejecutar

Abre esta carpeta en Android Studio. El proyecto declara SDK 37, Android Gradle Plugin 9.5.2, Gradle 9.6 y JVM 25 para Gradle. El código Java se compila con nivel 11 y el mínimo de Android es API 24.

Configura `ANDROID_HOME` o `sdk.dir` en `local.properties` con la ruta de tu SDK. Para compilar en PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Probar el recorrido completo

1. Registra una cuenta de **panadería**.
2. En Perfil, guarda la dirección y los días y horarios de recolección. Puedes guardar teléfono, descripción y coordenadas. Los datos de recolección son necesarios para recibir reservas.
3. Crea una publicación con precio y unidades disponibles. Una cantidad de cero la deja agotada. Cada publicación representa una unidad vendible, por ejemplo una caja o un paquete.
4. Cierra sesión desde Perfil y registra una cuenta de **cliente** en el mismo dispositivo.
5. Busca el producto, abre sus detalles y toca **Reservar y pagar al recoger**. Selecciona la cantidad y confirma.
6. En Mis compras puedes consultar el pedido, mostrar su QR, descargar el comprobante, abrir indicaciones o cancelar.
7. Copia el código de recolección. Cierra sesión e ingresa con la panadería. En Pedidos abre la reserva y confirma la entrega y el cobro introduciendo el código del cliente. También puedes leer un QR guardado como imagen.

## Reglas implementadas

- Pago en efectivo al recoger. Reservar no registra un pago; el vendedor confirma el cobro junto con la entrega.
- Las reservas duran dos horas desde su creación. El usuario debe recoger dentro del horario escrito por el negocio y antes del vencimiento.
- Reservar y descontar existencias es una transacción. Reintentar la misma solicitud no crea otro pedido.
- Cancelar o vencer devuelve las unidades una sola vez. Entregar requiere el código correcto y no permite repetir la entrega.
- La caducidad se comprueba al consultar datos y al intentar reservar, cancelar o entregar. Las pantallas de pedidos y catálogos se refrescan cada 30 segundos mientras están visibles; no hay un servicio permanente en segundo plano.
- Cada pedido conserva nombre, precio en centavos, cantidad, dirección, horario y coordenadas del momento de la reserva. Editar o retirar una publicación no borra el historial.
- Retirar una publicación la oculta del catálogo. Las imágenes utilizadas en pedidos se conservan.
- Los permisos de cliente y panadería se validan en las pantallas y en las operaciones de datos.
- SQLite migra de versiones anteriores a la versión 5 sin borrar usuarios, publicaciones o pedidos. La migración también añade `publicaciones.eliminado` si falta en una base marcada como versión 4.
- El comprobante descargable es texto; **no es una factura fiscal**.

## Google Maps

La clave se carga desde la variable de entorno `MAPS_API_KEY`, una propiedad de Gradle con el mismo nombre o `local.properties`:

```properties
MAPS_API_KEY=tu_clave
```

`local.properties` está ignorado por Git. La clave que ya existía en esta copia se trasladó a ese archivo. En Google Cloud debe estar habilitada Maps SDK for Android y configuradas las restricciones para el paquete y las firmas debug/release correspondientes. El mapa también necesita servicios de Google disponibles. Sin clave o servicios, el catálogo y las direcciones siguen utilizándose; las indicaciones se abren en una aplicación externa.

## Pruebas

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
# Con un emulador o dispositivo conectado:
.\gradlew.bat :app:connectedDebugAndroidTest
```

`DineroTest` verifica importes y redondeo. `ReservasTest` usa bases aisladas para probar inventario, concurrencia por la última unidad, idempotencia, permisos, cancelación, vencimiento, QR, historial y migración. `FlujoCompraTest` recorre las pantallas de compra y entrega con cuentas temporales y elimina únicamente sus datos al finalizar.

`ArranqueAndroid16Test` comprueba con Robolectric (API 36) el inicio sin sesión, el registro, los inicios por rol y la pantalla de diagnóstico. No sustituye la prueba física en un modelo concreto.

`MigracionSQLiteTest` reproduce el error `no such column: p.eliminado` usando una base v4 incompleta con cuentas, publicaciones y un pedido existentes. Comprueba la reparación, la conservación del precio, inventario, sesión y código de recolección, la reanudación del buscador y las migraciones desde versiones 1–4.

## Diagnóstico de cierres

El APK debug guarda un informe local si ocurre una excepción Java sin controlar. Muestra una pantalla nativa separada con el botón **Copiar diagnóstico**; pega ese texto en el chat para localizar la causa. Si Android no pudo abrir esa pantalla durante el cierre, el informe se presenta al abrir de nuevo. **Volver a intentar** descarta únicamente ese informe. Las cuentas y publicaciones se conservan. A partir de 1.0.2, un informe de una versión anterior se archiva al actualizar para permitir probar el arranque corregido.

Este mecanismo pertenece solo al APK debug. Los cierres nativos o una finalización directa del proceso por Android requieren Logcat del dispositivo. El cierre reportado en Motorola Android 16 se reprodujo con una base antigua sin la columna `eliminado`; la corrección está en la migración v5 incluida en la aplicación 1.0.2. La actualización se instala sobre la versión anterior y conserva los datos.

## Alcance pendiente para varios dispositivos

Para operar entre teléfonos hay que añadir un backend con autenticación, autorización e inventario transaccional en el servidor, almacenamiento compartido de imágenes y sincronización. No están integrados pagos electrónicos, notificaciones remotas, recuperación de contraseña por correo ni facturación fiscal. Los datos locales y la sesión no sustituyen la autorización de un servidor.
