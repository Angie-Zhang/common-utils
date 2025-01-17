package org.opensearch.commons.alerting.model

import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.io.stream.Writeable
import org.opensearch.common.xcontent.XContentParserUtils
import org.opensearch.commons.alerting.util.instant
import org.opensearch.commons.alerting.util.optionalTimeField
import org.opensearch.core.xcontent.ToXContent
import org.opensearch.core.xcontent.ToXContentObject
import org.opensearch.core.xcontent.XContentBuilder
import org.opensearch.core.xcontent.XContentParser
import java.io.IOException
import java.time.Instant

/**
 * When an alert triggered, the trigger's actions will be executed.
 * Action execution result records action throttle result and is a part of Alert.
 */
data class ActionExecutionResult(
    val actionId: String,
    val lastExecutionTime: Instant?,
    val throttledCount: Int = 0
) : Writeable, ToXContentObject {

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        sin.readString(), // actionId
        sin.readOptionalInstant(), // lastExecutionTime
        sin.readInt() // throttledCount
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        return builder.startObject()
            .field(ACTION_ID_FIELD, actionId)
            .optionalTimeField(LAST_EXECUTION_TIME_FIELD, lastExecutionTime)
            .field(THROTTLED_COUNT_FIELD, throttledCount)
            .endObject()
    }

    @Throws(IOException::class)
    override fun writeTo(out: StreamOutput) {
        out.writeString(actionId)
        out.writeOptionalInstant(lastExecutionTime)
        out.writeInt(throttledCount)
    }

    companion object {
        const val ACTION_ID_FIELD = "action_id"
        const val LAST_EXECUTION_TIME_FIELD = "last_execution_time"
        const val THROTTLED_COUNT_FIELD = "throttled_count"

        @JvmStatic
        @Throws(IOException::class)
        fun parse(xcp: XContentParser): ActionExecutionResult {
            lateinit var actionId: String
            var throttledCount: Int = 0
            var lastExecutionTime: Instant? = null

            XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, xcp.currentToken(), xcp)
            while (xcp.nextToken() != XContentParser.Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                xcp.nextToken()
                when (fieldName) {
                    ACTION_ID_FIELD -> actionId = xcp.text()
                    THROTTLED_COUNT_FIELD -> throttledCount = xcp.intValue()
                    LAST_EXECUTION_TIME_FIELD -> lastExecutionTime = xcp.instant()

                    else -> {
                        throw IllegalStateException("Unexpected field: $fieldName, while parsing action")
                    }
                }
            }

            requireNotNull(actionId) { "Must set action id" }
            return ActionExecutionResult(actionId, lastExecutionTime, throttledCount)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readFrom(sin: StreamInput): ActionExecutionResult {
            return ActionExecutionResult(sin)
        }
    }
}
