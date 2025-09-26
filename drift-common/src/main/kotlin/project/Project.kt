/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package project

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.Exception


/******************************************************************************
 * DRIFT PROJECT HELPER CLASS
 ******************************************************************************/



/**
 * Drift Project Main Data Object
 *
 * Provide all information about this Drift distribution.
 */
data object Project


@Serializable
data class ProjectConfig(
    val name: String = "Unnamed Project",
    val structure: ProjectStructure = ProjectStructure(
        "./src",
        "main"))

@Serializable
data class ProjectStructure(
    val root: String,
    val entry: String)


class DriftProjectLoadingException(
    override val message: String) : Exception(message)


fun loadConfig(dir: File) : ProjectConfig {
    val configFile = File(dir, "drift.json")

    if (!configFile.exists()) {
        throw DriftProjectLoadingException(
            "Config file does not exist: ${configFile.absolutePath}")
    }

    val json = configFile.readText()

    return Json.decodeFromString(ProjectConfig.serializer(), json)
}