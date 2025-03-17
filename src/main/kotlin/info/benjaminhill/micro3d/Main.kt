package info.benjaminhill.micro3d

import kotlinx.coroutines.delay
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.time.Duration.Companion.seconds

val exportDir: Path = Path.of("./output").also { it.createDirectories() }

suspend fun main() {
    EasyCamera().use { camera ->
        val image = camera.captureMat()
        camera.captureToFile(exportDir.resolve("DELETEME.png").toString())
    }
    System.exit(0)



    EasyPort.connect().use { port ->
        delay(5.seconds)
        println("Starting commands")
        val app = MicroscopeCamera(GCode(port))
        println("Starting command loop")
        while (true) {
            print("> ")
            val cmd = readlnOrNull() ?: continue
            when (cmd) {
                "exit" -> break
                "focus" -> app.focus()
                "grid" -> app.captureGrid()
                "stack" -> app.captureStack()
                else -> {
                    // repeated letters for longer moves
                    cmd.forEach { c ->
                        app.invokeUsingName(c)
                    }
                }
            }
        }
    }
}
