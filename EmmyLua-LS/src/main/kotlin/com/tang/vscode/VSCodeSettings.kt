package com.tang.vscode

import com.google.gson.*
import com.tang.intellij.lua.IVSCodeSettings
import com.tang.intellij.lua.project.LuaSettings
import com.tang.vscode.diagnostics.DiagnosticsOptions
import com.tang.vscode.diagnostics.InspectionsLevel
import com.tang.vscode.formatter.FormattingOptions
import com.yevdo.jwildcard.JWildcard

private fun <T> listEquals(a: List<T>, b: List<T>): Boolean {
    if (a.size != b.size)
        return false
    for (i in 0 until a.size) {
        if (a[i] != b[i])
            return false
    }
    return true
}

class SettingsUpdateResult(
    val associationChanged: Boolean
)

private const val DEFAULT_ASSOCIATION = "*.lua"

object VSCodeSettings : IVSCodeSettings {

    private var settings: JsonObject? = null

    private var mySourceRoots = mutableListOf<String>()

    override val sourceRoots get() = mySourceRoots

    private var myCompletionCaseSensitive = false

    override val completionCaseSensitive get() = myCompletionCaseSensitive

    private var myShowCodeLens = false

    private val associations = mutableListOf(DEFAULT_ASSOCIATION)

    private val myFileExtensions = mutableListOf<String>()

    override val fileExtensions: List<String>
        get() = myFileExtensions

    override val showCodeLens get() = myShowCodeLens

    var clientType = "unknown"

    override val isVSCode get() = clientType == "vsc"

    override fun matchFile(name: String): Boolean {
        return associations.any { JWildcard.matches(it, name) }
    }

    fun update(settings: JsonObject): SettingsUpdateResult {
        this.settings = settings

        //files.associations
        val associationChanged = updateAssociations()

        //case sensitive
        val caseSensitive = path("emmylua.completion.caseSensitive")
        if (caseSensitive != null) {
            myCompletionCaseSensitive = caseSensitive.asBoolean
        }

        // source roots
        updateSourceRoots()

        // show codeLens
        myShowCodeLens = path("emmylua.codeLens")?.asBoolean == true

        path("emmylua.hint.paramHint")?.asBoolean?.let {
            LuaSettings.instance.paramHint = it
        }

        path("emmylua.hint.localHint")?.asBoolean?.let {
            LuaSettings.instance.localHint = it
        }

        path("emmylua.hint.varargHint")?.asBoolean?.let {
            LuaSettings.instance.varargHint = it
        }

        path("emmylua.hint.overrideHint")?.asBoolean?.let {
            LuaSettings.instance.overrideHint = it
        }

        path("emmylua.constructorNames")?.asString?.let {
            LuaSettings.instance.constructorNames = it.split(";").filter { it.isNotEmpty() }.toTypedArray()
        }

        path("emmylua.requireLikeFunctions")?.asString?.let {
            LuaSettings.instance.requireLikeFunctions = it.split(";").filter { it.isNotEmpty() }.toTypedArray()
        }


        path("emmylua.typeSafety.anyTypeCanAssignToAnyDefineType")?.asBoolean?.let {
            DiagnosticsOptions.anyTypeCanAssignToAnyDefineType = it
        }
        path("emmylua.typeSafety.defineAnyTypeCanBeAssignedByAnyVariable")?.asBoolean?.let {
            DiagnosticsOptions.defineAnyTypeCanBeAssignedByAnyVariable = it
        }
        path("emmylua.typeSafety.defineTypeCanReceiveNilType")?.asBoolean?.let {
            DiagnosticsOptions.defineTypeCanReceiveNilType = it
        }

        path("emmylua.inspections.parameterValidation")?.asString?.let {
            DiagnosticsOptions.parameterValidation = InspectionsLevel.valueOf(it)
        }
        path("emmylua.inspections.fieldValidation")?.asString?.let {
            DiagnosticsOptions.fieldValidation = InspectionsLevel.valueOf(it)
        }
        path("emmylua.inspections.undeclaredVariable")?.asString?.let {
            DiagnosticsOptions.undeclaredVariable = InspectionsLevel.valueOf(it)
        }
        path("emmylua.inspections.assignValidation")?.asString?.let {
            DiagnosticsOptions.assignValidation = InspectionsLevel.valueOf(it)
        }
        path("emmylua.inspections.deprecated")?.asBoolean?.let {
            DiagnosticsOptions.deprecated = InspectionsLevel.Warning
        }

        return SettingsUpdateResult(associationChanged)
    }

    private fun updateSourceRoots() {
        mySourceRoots.clear()
        val sourceRoots = path("emmylua.source.roots")
        if (sourceRoots is JsonArray) {
            sourceRoots.forEach {
                if (it is JsonPrimitive)
                    mySourceRoots.add(it.asString)
            }
        }
        mySourceRoots.sort()
    }

    private fun updateAssociations(): Boolean {
        val ass = path("files.associations")
        val oriAssociations = ArrayList(associations)
        associations.clear()
        associations.add(DEFAULT_ASSOCIATION)
        myFileExtensions.clear()
        myFileExtensions.add(".lua")
        if (ass is JsonObject) {
            for (entry in ass.entrySet()) {
                val lan = entry.value.asString
                if (lan.toLowerCase() == "lua") {
                    val wildcard = entry.key
                    associations.add(wildcard)

                    if (wildcard.startsWith("*.")) {
                        myFileExtensions.add(wildcard.substring(1))
                    }
                }
            }
        }
        associations.sort()
        return !listEquals(oriAssociations, associations)
    }

    private fun path(path: String): JsonElement? {
        val arr = path.split(".")
        var ret: JsonElement = settings ?: return null
        for (s in arr) {
            if (ret !is JsonObject || ret is JsonNull)
                return null
            ret = ret.get(s) ?: return null
        }
        return ret
    }
}