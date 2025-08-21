package dji.sampleV5.aircraft.utils

// Stubs mínimos y únicos para compilación exitosa - EVITAR DUPLICADOS

// === OBJETOS SINGLETON ===
object mission
object keyvalue  
object common
object manager
object JsonUtil
object SimulatorManager
object GpsUtils
object DialogUtil
object Helper
object WaypointMissionManager

// === CLASES DE DATOS ===
class MediaDataCenter
class DecoderState
class VideoPlayState
class VideoChannelType
class DecoderOutputMode
class VirtualStickFlightControlParam
class Stick
class FlightMode
class LocationCoordinate2D
class WaypointMissionExecuteState
class IDJIError
class BreakPointInfo

// === INTERFACES ===
interface IVideoDecoder
interface IVideoFrame
interface WaylineExecutingInfoListener
interface WaylineExecutingInterruptReasonUpdate

// === CLASES IMPLEMENTACIÓN ===
class VideoDecoder

// === OBJETOS CON PROPIEDADES ===
object LogPath {
    const val SAMPLE = "SAMPLE"
    const val WAYPOINT = "WAYPOINT"
}

object RecoverActionType {
    const val GoBackToRecordPoint = "GoBackToRecordPoint"
    const val GoBackToNextPoint = "GoBackToNextPoint" 
    const val GoBackToNextNextPoint = "GoBackToNextNextPoint"
}

object WPMZParserManager {
    const val TEMPLATE_FILE = "template.kmz"
    const val WAYLINE_FILE = "wayline.kmz"
    fun unZipFolder(context: Any?, src: String, dest: String, overwrite: Boolean) {}
    fun zipFiles(context: Any?, files: List<String>, dest: String) {}
}

object CommonCallbacks {
    open class CompletionCallback {
        open fun onSuccess() {}
        open fun onFailure(error: IDJIError) {}
    }
}

// === FUNCIONES GLOBALES ===
fun getPackageName(): String = "com.dronescan.msdksample"
