import dev.kikugie.stonecutter.data.ParsedVersion
import org.gradle.api.JavaVersion

class JavaVersionConverter {
    val parsedVersion: ParsedVersion
    constructor(parsedVersion: ParsedVersion){
        this.parsedVersion = parsedVersion
    }
    
    fun getJavaVersion(): JavaVersion {
        return when {
            parsedVersion > "1.21.11" -> JavaVersion.VERSION_25
            parsedVersion >= "1.20.6" -> JavaVersion.VERSION_21
            parsedVersion >= "1.18" -> JavaVersion.VERSION_17
            parsedVersion >= "1.17" -> JavaVersion.VERSION_16
            else -> JavaVersion.VERSION_1_8
        }
    }
    
    fun getJavaVersionString(): String {
        return when {
            parsedVersion > "1.21.11" -> "JAVA_25"
            parsedVersion >= "1.20.6" -> "JAVA_21"
            parsedVersion >= "1.18" -> "JAVA_17"
            parsedVersion >= "1.17" -> "JAVA_16"
            else -> "JAVA_18"
        }
    }
    
    fun getJavaVersionInt(): Int {
        return when {
            parsedVersion > "1.21.11" -> 25
            parsedVersion >= "1.20.6" -> 21
            parsedVersion >= "1.18" -> 17
            parsedVersion >= "1.17" -> 16
            else -> 18
        }
    }
}