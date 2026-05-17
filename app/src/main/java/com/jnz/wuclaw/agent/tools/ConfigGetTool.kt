package com.jnz.wuclaw.agent.tools

/**
 * OmniClaw Source Reference:
 * - ../xomniclaw/src/gateway/(all)
 *
 * OmniClaw adaptation: explicit config read tool.
 */


import com.jnz.wuclaw.gateway.methods.ConfigMethods
import com.jnz.wuclaw.providers.FunctionDefinition
import com.jnz.wuclaw.providers.ParametersSchema
import com.jnz.wuclaw.providers.PropertySchema
import com.jnz.wuclaw.providers.ToolDefinition

/**
 * Read value from /sdcard/.xomniclaw/xomniclaw.json by path.
 */
class ConfigGetTool(
    private val configMethods: ConfigMethods
) : Tool {
    override val name = "config_get"
    override val description = "Read a configuration value from xomniclaw.json by dot path"

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "path" to PropertySchema("string", "Dot path, e.g. channels.feishu.appId")
                    ),
                    required = listOf("path")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val path = args["path"] as? String
            ?: return ToolResult.error("Missing required parameter: path")

        val result = configMethods.configGet(mapOf("path" to path))
        return if (result.success) {
            ToolResult.success(result.config?.toString() ?: "null")
        } else {
            ToolResult.error(result.error ?: "Failed to read config")
        }
    }
}
