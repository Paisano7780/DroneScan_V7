package dji.sampleV5.aircraft.utils

// Stubs mínimos para avanzar en la compilación
// Stubs únicos para clases y objetos que no existen como archivos individuales
object mission
object keyvalue
object common
object manager
object JsonUtil
class MediaDataCenter
class DecoderState
class VideoPlayState
interface IVideoDecoder
class VideoDecoder
class VideoChannelType
class DecoderOutputMode
interface IVideoFrame
class VirtualStickFlightControlParam
class Stick
class FlightMode
class LocationCoordinate2D
object SimulatorManager
object GpsUtils
interface WaylineExecutingInfoListener
interface WaylineExecutingInterruptReasonUpdate
class RecoverActionType
object DialogUtil
object Helper
object WaypointMissionManager
class WaypointMissionExecuteState
object CommonCallbacks {
	open class CompletionCallback
}
class IDJIError
class BreakPointInfo
object LogPath
object WPMZParserManager
fun getPackageName(): String = "stub"

object LogPath {
	const val SAMPLE = "SAMPLE"
	const val WAYPOINT = "WAYPOINT"
}

object RecoverActionType {
	val GoBackToRecordPoint = "GoBackToRecordPoint"
	val GoBackToNextPoint = "GoBackToNextPoint"
	val GoBackToNextNextPoint = "GoBackToNextNextPoint"
}

object WPMZParserManager {
	const val TEMPLATE_FILE = "template.kmz"
	const val WAYLINE_FILE = "wayline.kmz"
	fun unZipFolder(context: Any?, src: String, dest: String, overwrite: Boolean) {}
	fun zipFiles(context: Any?, files: List<String>, dest: String) {}
}
