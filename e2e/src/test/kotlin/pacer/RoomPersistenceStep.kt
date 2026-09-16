package pacer

import com.thoughtworks.gauge.Step
import java.io.File
import java.util.concurrent.TimeUnit

class RoomPersistenceStep {
    @Step("プロセス再起動後もルームが取得できる契約が満たされている")
    fun プロセス再起動後もルームが取得できる契約が満たされている() {
        val webDir = File("../web").canonicalFile
        require(webDir.isDirectory) { "web directory not found: $webDir" }

        val process =
            ProcessBuilder("npm", "run", "test:persist")
                .directory(webDir)
                .redirectErrorStream(true)
                .start()

        val output = process.inputStream.bufferedReader().readText()
        val finished = process.waitFor(120, TimeUnit.SECONDS)
        check(finished) {
            process.destroyForcibly()
            "persistence contract test timed out"
        }
        check(process.exitValue() == 0) {
            "persistence contract failed (exit ${process.exitValue()}):\n$output"
        }
    }
}
