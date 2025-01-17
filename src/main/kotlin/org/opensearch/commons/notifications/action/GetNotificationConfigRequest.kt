/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.commons.notifications.action

import org.opensearch.action.ActionRequest
import org.opensearch.action.ActionRequestValidationException
import org.opensearch.action.ValidateActions
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.io.stream.Writeable
import org.opensearch.common.xcontent.XContentParserUtils
import org.opensearch.commons.notifications.NotificationConstants.CONFIG_ID_LIST_TAG
import org.opensearch.commons.notifications.NotificationConstants.DEFAULT_MAX_ITEMS
import org.opensearch.commons.notifications.NotificationConstants.FILTER_PARAM_LIST_TAG
import org.opensearch.commons.notifications.NotificationConstants.FROM_INDEX_TAG
import org.opensearch.commons.notifications.NotificationConstants.MAX_ITEMS_TAG
import org.opensearch.commons.notifications.NotificationConstants.SORT_FIELD_TAG
import org.opensearch.commons.notifications.NotificationConstants.SORT_ORDER_TAG
import org.opensearch.commons.utils.STRING_READER
import org.opensearch.commons.utils.STRING_WRITER
import org.opensearch.commons.utils.enumReader
import org.opensearch.commons.utils.fieldIfNotNull
import org.opensearch.commons.utils.logger
import org.opensearch.commons.utils.stringList
import org.opensearch.core.xcontent.ToXContent
import org.opensearch.core.xcontent.ToXContentObject
import org.opensearch.core.xcontent.XContentBuilder
import org.opensearch.core.xcontent.XContentParser
import org.opensearch.search.sort.SortOrder
import java.io.IOException

/**
 * Action Request for getting notification configuration.
 */
class GetNotificationConfigRequest : ActionRequest, ToXContentObject {
    val configIds: Set<String>
    val fromIndex: Int
    val maxItems: Int
    val sortField: String?
    val sortOrder: SortOrder?
    val filterParams: Map<String, String>

    companion object {
        private val log by logger(GetNotificationConfigRequest::class.java)

        /**
         * reader to create instance of class from writable.
         */
        val reader = Writeable.Reader { GetNotificationConfigRequest(it) }

        /**
         * Creator used in REST communication.
         * @param parser XContentParser to deserialize data from.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun parse(parser: XContentParser): GetNotificationConfigRequest {
            var configIdList: Set<String> = setOf()
            var fromIndex = 0
            var maxItems = DEFAULT_MAX_ITEMS
            var sortField: String? = null
            var sortOrder: SortOrder? = null
            var filterParams: Map<String, String> = mapOf()

            XContentParserUtils.ensureExpectedToken(
                XContentParser.Token.START_OBJECT,
                parser.currentToken(),
                parser
            )
            while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                val fieldName = parser.currentName()
                parser.nextToken()
                when (fieldName) {
                    CONFIG_ID_LIST_TAG -> configIdList = parser.stringList().toSet()
                    FROM_INDEX_TAG -> fromIndex = parser.intValue()
                    MAX_ITEMS_TAG -> maxItems = parser.intValue()
                    SORT_FIELD_TAG -> sortField = parser.textOrNull()
                    SORT_ORDER_TAG -> sortOrder = SortOrder.fromString(parser.text())
                    FILTER_PARAM_LIST_TAG -> filterParams = parser.mapStrings()
                    else -> {
                        parser.skipChildren()
                        log.info("Unexpected field: $fieldName, while parsing GetNotificationConfigRequest")
                    }
                }
            }
            return GetNotificationConfigRequest(configIdList, fromIndex, maxItems, sortField, sortOrder, filterParams)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun toXContent(builder: XContentBuilder?, params: ToXContent.Params?): XContentBuilder {
        return builder!!.startObject()
            .field(CONFIG_ID_LIST_TAG, configIds)
            .field(FROM_INDEX_TAG, fromIndex)
            .field(MAX_ITEMS_TAG, maxItems)
            .fieldIfNotNull(SORT_FIELD_TAG, sortField)
            .fieldIfNotNull(SORT_ORDER_TAG, sortOrder)
            .field(FILTER_PARAM_LIST_TAG, filterParams)
            .endObject()
    }

    /**
     * constructor for creating the class
     * @param configIds the ids of the notification configuration (other parameters are not relevant if ids are present)
     * @param fromIndex the starting index for paginated response
     * @param maxItems the maximum number of items to return for paginated response
     * @param sortField the sort field if response has many items
     * @param sortOrder the sort order if response has many items
     * @param filterParams the filter parameters
     */
    constructor(
        configIds: Set<String> = setOf(),
        fromIndex: Int = 0,
        maxItems: Int = DEFAULT_MAX_ITEMS,
        sortField: String? = null,
        sortOrder: SortOrder? = null,
        filterParams: Map<String, String> = mapOf()
    ) {
        this.configIds = configIds
        this.fromIndex = fromIndex
        this.maxItems = maxItems
        this.sortField = sortField
        this.sortOrder = sortOrder
        this.filterParams = filterParams
    }

    /**
     * {@inheritDoc}
     */
    @Throws(IOException::class)
    constructor(input: StreamInput) : super(input) {
        configIds = input.readStringList().toSet()
        fromIndex = input.readInt()
        maxItems = input.readInt()
        sortField = input.readOptionalString()
        sortOrder = input.readOptionalWriteable(enumReader(SortOrder::class.java))
        filterParams = input.readMap(STRING_READER, STRING_READER)
    }

    /**
     * {@inheritDoc}
     */
    @Throws(IOException::class)
    override fun writeTo(output: StreamOutput) {
        super.writeTo(output)
        output.writeStringCollection(configIds)
        output.writeInt(fromIndex)
        output.writeInt(maxItems)
        output.writeOptionalString(sortField)
        output.writeOptionalWriteable(sortOrder)
        output.writeMap(filterParams, STRING_WRITER, STRING_WRITER)
    }

    /**
     * {@inheritDoc}
     */
    override fun validate(): ActionRequestValidationException? {
        var validationException: ActionRequestValidationException? = null
        if (fromIndex < 0) {
            validationException = ValidateActions.addValidationError("fromIndex is -ve", validationException)
        }
        if (maxItems <= 0) {
            validationException = ValidateActions.addValidationError("maxItems is not +ve", validationException)
        }
        return validationException
    }
}
