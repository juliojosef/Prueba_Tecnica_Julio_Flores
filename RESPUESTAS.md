\# Parte I — Teoría, diseño y criterio técnico





\## 6. Descomposición del monolito



Definiría los límites por responsabilidades de negocio y reglas de consistencia.

Mantendría inicialmente cuentas, saldos y movimientos en un mismo servicio:

registrar el movimiento y actualizar el saldo requieren una transacción atómica.

Separaría notificaciones y reportería, porque pueden trabajar con consistencia

eventual y tienen necesidades diferentes de disponibilidad y escalamiento.

El servicio financiero sería dueño de las cuentas y movimientos; notificaciones

de sus entregas y reintentos; reportería de sus proyecciones de consulta.

Usaría REST para comandos y consultas que requieren respuesta inmediata,

y eventos para actualizar reportes y enviar notificaciones sin bloquear la operación.

Migraría gradualmente, evitando que varios servicios escriban en las mismas tablas.



\## 7. Gateway, Config Server y service discovery



Spring Cloud Gateway centraliza el ingreso: enrutamiento, autenticación,

límites de solicitudes y propagación de identificadores de correlación.



Config Server centraliza configuración por ambiente y facilita su administración;

introduce una dependencia operativa, por lo que definiría recuperación y respaldo.



Service discovery permite encontrar instancias sin fijar sus direcciones,

pero agrega registro, comprobaciones de salud y posibles datos desactualizados.



\## 8. Consistencia entre movimiento y evento



Usaría transactional outbox: en la misma transacción guardaría el movimiento,

actualizaría el saldo e insertaría un evento pendiente en la tabla outbox.

Así, ambos registros se confirman juntos o ninguno se confirma.

Un publicador enviaría los eventos al broker y marcaría su entrega después

de recibir confirmación; si falla, conservaría el evento para reintentar.

Puede haber publicaciones duplicadas si ocurre una caída entre enviar y marcar.

Por eso cada evento tendría un ID único y los consumidores registrarían los

IDs procesados de forma atómica con sus efectos locales.

Una clave de idempotencia impediría duplicar el movimiento ante reintentos del cliente.

Reservaría una saga para operaciones entre servicios con transacciones locales

y compensaciones; no es necesaria para el movimiento y outbox en una misma BD.



\## 9. Resiliencia ante un tercero inestable



\- Timeout: limita conexión y respuesta. Muy corto provoca rechazos innecesarios;

&#x20; muy largo retiene recursos. Definiría también un presupuesto total de tiempo.

\- Retry: pocos intentos ante fallas transitorias, con espera exponencial y jitter.

&#x20; No reintentaría rechazos de negocio. Sin límites amplifica una caída;

&#x20; una operación con efectos exige idempotencia también en el tercero.

\- Circuit breaker: deja de llamar temporalmente cuando acumula fallas

&#x20; y permite llamadas de prueba para detectar recuperación.

&#x20; Umbrales inadecuados pueden bloquear una dependencia que ya funciona.

\- Bulkhead: limita concurrencia y recursos destinados a la dependencia.

&#x20; Una cola grande vuelve a introducir latencia; definiría rechazo controlado.

\- Fallback: para una validación financiera obligatoria rechazaría de forma

&#x20; controlada con 503 y permitiría reintentar con la misma clave.

&#x20; No confirmaría movimientos sin validar, porque cambia la regla de negocio.



En el ejercicio implementé timeout, reintentos acotados, circuit breaker

y rechazo controlado. El bulkhead sería una mejora adicional.



\## 10. Diagnóstico de latencia intermitente



1\. Confirmaría el periodo, endpoints afectados, volumen y percentiles p50/p95/p99;

&#x20;  revisaría despliegues o cambios recientes.

2\. Seguiría una petición lenta con correlation-id y trazas para separar

&#x20;  tiempo de aplicación, espera de conexiones, BD y llamada externa.

3\. Revisaría SQL Server: consultas lentas, bloqueos, esperas, plan de ejecución

&#x20;  y ocupación del pool de conexiones.

4\. Revisaría duración y reintentos del tercero; comprobaría DNS,

&#x20;  establecimiento TCP/TLS y conectividad desde el entorno de la aplicación.

5\. Revisaría CPU, memoria, pausas de GC, disco, hilos y colas;

&#x20;  buscaría saturación mediante métricas y thread dumps.

6\. Reproduciría con carga comparable, cambiaría una causa a la vez

&#x20;  y mediría nuevamente. La ausencia de errores no descarta esperas o saturación.



\## 11. Diagnóstico y optimización de SQL Server



Compararía el comportamiento con Query Store, el plan real de ejecución

y estadísticas de lecturas y tiempo; revisaría bloqueos y tipos de espera.

Como acciones concretas: ajustaría índices a los filtros y ordenamiento,

actualizaría estadísticas cuando estuvieran desactualizadas y eliminaría

conversiones o funciones sobre columnas filtradas que impidan búsquedas eficientes.

En movimientos usaría un índice por cuenta\_id, fecha e id, con columnas

incluidas para reducir accesos adicionales a la tabla.

También eliminaría N+1 mediante consultas por lote o proyecciones.

La paginación tendría orden estable por fecha e id; para páginas profundas

evaluaría paginación por cursor frente al costo creciente de OFFSET.

El saldo acumulado debe considerar movimientos anteriores al inicio del rango;

filtrar primero ese historial produciría un resultado incorrecto.

Verificaría las mejoras bajo carga, considerando el costo de mantener los índices.



\## 12. Mínimo privilegio y secretos



Cada servicio tendría una identidad propia y únicamente los permisos necesarios.

La aplicación no usaría sa, sysadmin ni db\_owner; las migraciones tendrían

una identidad separada con permisos de creación o modificación del esquema.

Para el tercero usaría credenciales restringidas a sus operaciones necesarias.

En desarrollo inyectaría secretos locales fuera del repositorio;

en UAT y producción usaría un gestor de secretos e identidad de la carga,

con inyección mediante archivos protegidos o variables de entorno.

Mantendría credenciales distintas por ambiente, rotación y auditoría de accesos.

No incluiría secretos en Git, Dockerfile, imágenes, ejemplos ni logs.

Las variables de entorno son un mecanismo de entrega, no un gestor de secretos.



\## 13. Datos sensibles, PCI DSS y PLD/LA



Aplicaría minimización de datos, TLS con validación de certificados y cifrado

de BD y respaldos, con llaves administradas por separado y acceso restringido.

Para tarjetas preferiría tokens de un proveedor especializado y evitaría

almacenar el PAN cuando no fuera necesario para el negocio.

Enmascararía datos sensibles en respuestas y logs; nunca registraría

credenciales ni cuerpos completos de solicitudes con información de clientes.

La auditoría conservaría actor, operación, resultado y correlación,

con controles de acceso, integridad y retención.

PCI DSS establece controles técnicos y operativos para proteger datos de pago;

usar tokens no permite asumir automáticamente que todo queda fuera de alcance.

Para PLD/LA facilitaría identificación del cliente, monitoreo y trazabilidad

de operaciones, alertas y evidencia para el equipo de cumplimiento,

conforme a las obligaciones aplicables. El cifrado por sí solo no cumple PLD/LA.





\## 14. Fallas y riesgos del fragmento original



| Líneas | Falla o riesgo | Condición y consecuencia |

|---|---|---|

| 10–11 | Optional.get() sin validar existencia | Si una cuenta no existe, produce NoSuchElementException en lugar de una excepción de negocio. |

| 9–13 | Entradas sin validar | IDs nulos o vacíos y monto nulo o inválido generan errores técnicos sin respuesta controlada. |

| 13–17 | Conversión monetaria a double | Importes decimales pueden perder precisión; montos extremos pueden convertirse en infinito. |

| 13–17 | No valida monto positivo ni escala y límite | Un monto negativo invierte la transferencia; cero permite una operación sin sentido y exceso de decimales incumple el formato monetario. |

| 15 | Usa > en lugar de >= | Rechaza transferir exactamente el saldo disponible. |

| 15–20 | Saldo insuficiente se ignora | El método termina sin informar que la operación fue rechazada. |

| 9, 18–19 | Falta una transacción que abarque ambas cuentas | Si falla el segundo guardado, el débito puede quedar confirmado sin el crédito. |

| 10–19 | Falta control de concurrencia | Dos operaciones leen el mismo saldo y sobrescriben cambios o autorizan débitos incompatibles. |

| 9–17 | No rechaza origen igual a destino | Manipula dos referencias de la misma cuenta y admite una transferencia inválida. |

| 7, 22 | HashMap mutable compartido en un servicio singleton | Solicitudes simultáneas producen acceso inseguro; además, la caché crece sin límite. |

| 22 | Caché sin coherencia transaccional ni entre instancias | Puede conservar saldos obsoletos o distintos de la BD; cambiar HashMap por ConcurrentHashMap no resuelve esta inconsistencia. |

| 26 | Compara String mediante == | Compara referencias, por lo que un valor PREMIUM puede clasificarse como estándar. |

| 25–26 | No valida Cuenta nula | estado(null) produce NullPointerException. |

| 4–5 | Inyección por campo | Dificulta expresar dependencias obligatorias y construir el servicio en pruebas; usaría constructor. |



Además, el método no ofrece idempotencia: si el cliente reintenta después

de perder la respuesta, puede ejecutar otra transferencia.



\## 15. Transferencia y clasificación corregidas



El siguiente ejemplo es una propuesta para el fragmento teórico.

Supone una entidad Cuenta con id String y saldo BigDecimal mapeado a DECIMAL(19,2).

Todas las operaciones que modifican saldos deben respetar el mismo esquema de bloqueo.



\### Repositorio



```java

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;



import java.util.Optional;



public interface CuentaRepository extends JpaRepository<Cuenta, String> {



&#x20;   @Lock(LockModeType.PESSIMISTIC\_WRITE)

&#x20;   @Query("select c from Cuenta c where c.id = :id")

&#x20;   Optional<Cuenta> buscarParaActualizar(@Param("id") String id);

}

```



\### Servicio



```java

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.math.BigDecimal;

import java.math.RoundingMode;



@Service

public class TransferenciaService {



&#x20;   private static final BigDecimal MAX\_SALDO =

&#x20;           new BigDecimal("99999999999999999.99");



&#x20;   private final CuentaRepository cuentaRepo;



&#x20;   public TransferenciaService(CuentaRepository cuentaRepo) {

&#x20;       this.cuentaRepo = cuentaRepo;

&#x20;   }



&#x20;   @Transactional

&#x20;   public void transferir(String origenId, String destinoId, String monto) {

&#x20;       validarId(origenId);

&#x20;       validarId(destinoId);



&#x20;       if (origenId.equals(destinoId)) {

&#x20;           throw new NegocioException(

&#x20;                   "Las cuentas de origen y destino deben ser diferentes");

&#x20;       }



&#x20;       BigDecimal importe = convertirMonto(monto);



&#x20;       // Todas las transferencias bloquean en el mismo orden.

&#x20;       String primeroId = origenId.compareTo(destinoId) < 0

&#x20;               ? origenId : destinoId;

&#x20;       String segundoId = origenId.compareTo(destinoId) < 0

&#x20;               ? destinoId : origenId;



&#x20;       Cuenta primera = bloquear(primeroId);

&#x20;       Cuenta segunda = bloquear(segundoId);



&#x20;       Cuenta origen = origenId.equals(primeroId) ? primera : segunda;

&#x20;       Cuenta destino = destinoId.equals(primeroId) ? primera : segunda;



&#x20;       BigDecimal saldoOrigen = validarSaldo(origen.getSaldo());

&#x20;       BigDecimal saldoDestino = validarSaldo(destino.getSaldo());



&#x20;       if (saldoOrigen.compareTo(importe) < 0) {

&#x20;           throw new NegocioException("Saldo insuficiente");

&#x20;       }



&#x20;       BigDecimal nuevoSaldoDestino = saldoDestino.add(importe);



&#x20;       if (nuevoSaldoDestino.compareTo(MAX\_SALDO) > 0) {

&#x20;           throw new NegocioException(

&#x20;                   "El saldo de destino excede el límite permitido");

&#x20;       }



&#x20;       origen.setSaldo(saldoOrigen.subtract(importe));

&#x20;       destino.setSaldo(nuevoSaldoDestino);



&#x20;       // Las entidades están administradas por JPA.

&#x20;       // Los cambios se guardan juntos al confirmar la transacción.

&#x20;       cuentaRepo.flush();

&#x20;   }



&#x20;   public String estado(Cuenta cuenta) {

&#x20;       if (cuenta == null) {

&#x20;           throw new NegocioException("La cuenta es obligatoria");

&#x20;       }

&#x20;       return "PREMIUM".equals(cuenta.getTipo())

&#x20;               ? "prioritario" : "estandar";

&#x20;   }



&#x20;   private Cuenta bloquear(String id) {

&#x20;       return cuentaRepo.buscarParaActualizar(id)

&#x20;               .orElseThrow(() ->

&#x20;                       new CuentaNoEncontradaException(id));

&#x20;   }



&#x20;   private void validarId(String id) {

&#x20;       if (id == null || id.isBlank()) {

&#x20;           throw new NegocioException("El identificador es obligatorio");

&#x20;       }

&#x20;   }



&#x20;   private BigDecimal convertirMonto(String texto) {

&#x20;       if (texto == null || texto.isBlank()) {

&#x20;           throw new NegocioException("El monto es obligatorio");

&#x20;       }



&#x20;       BigDecimal importe;

&#x20;       try {

&#x20;           importe = new BigDecimal(texto)

&#x20;                   .setScale(2, RoundingMode.UNNECESSARY);

&#x20;       } catch (NumberFormatException | ArithmeticException ex) {

&#x20;           throw new NegocioException(

&#x20;                   "El monto debe ser numérico y tener hasta dos decimales");

&#x20;       }



&#x20;       if (importe.signum() <= 0

&#x20;               || importe.compareTo(MAX\_SALDO) > 0) {

&#x20;           throw new NegocioException("El monto está fuera del rango permitido");

&#x20;       }

&#x20;       return importe;

&#x20;   }



&#x20;   private BigDecimal validarSaldo(BigDecimal saldo) {

&#x20;       if (saldo == null || saldo.signum() < 0

&#x20;               || saldo.compareTo(MAX\_SALDO) > 0) {

&#x20;           throw new NegocioException("La cuenta tiene un saldo inválido");

&#x20;       }

&#x20;       try {

&#x20;           return saldo.setScale(2, RoundingMode.UNNECESSARY);

&#x20;       } catch (ArithmeticException ex) {

&#x20;           throw new NegocioException("La cuenta tiene una escala monetaria inválida");

&#x20;       }

&#x20;   }

}

```



\### Excepciones de negocio



Cada clase pública se ubicaría en su propio archivo.



```java

public class NegocioException extends RuntimeException {

&#x20;   public NegocioException(String mensaje) {

&#x20;       super(mensaje);

&#x20;   }

}

```



```java

public class CuentaNoEncontradaException extends RuntimeException {

&#x20;   public CuentaNoEncontradaException(String id) {

&#x20;       super("Cuenta no encontrada: " + id);

&#x20;   }

}

```



\### Justificación



\- @Transactional hace atómicos el débito y el crédito; las excepciones

&#x20; RuntimeException provocan rollback. El método debe invocarse mediante

&#x20; el proxy de Spring, desde otro componente.

\- BigDecimal y DECIMAL(19,2) evitan aritmética binaria para dinero.

&#x20; Se rechazan fracciones incompatibles en lugar de redondearlas silenciosamente.

\- PESSIMISTIC\_WRITE protege los saldos durante la operación.

&#x20; El orden fijo de adquisición reduce el riesgo de interbloqueos.

&#x20; Los IDs deben ser canónicos y la BD debe tratarlos como cuentas distintas.

\- Las validaciones rechazan cuentas iguales, entradas inválidas,

&#x20; saldo insuficiente y desbordamiento del saldo de destino.

\- compareTo permite transferir exactamente el saldo disponible.

\- Se elimina la caché de saldos: la BD es la fuente de verdad.

\- La comparación "PREMIUM".equals(...) compara contenido y tolera tipo nulo,

&#x20; que en este ejemplo se clasifica como estándar.

\- Un ControllerAdvice distinguiría entradas inválidas (400), cuenta

&#x20; inexistente (404) y conflictos de negocio (409), sin exponer errores internos.

&#x20; Los timeouts de bloqueo se tratarían como fallas transitorias controladas.

\- Para una API de transferencias agregaría una clave de idempotencia,

&#x20; un registro único de la operación y, si publica eventos, una outbox

&#x20; dentro de esta misma transacción. No usaría reintentos ciegos.

