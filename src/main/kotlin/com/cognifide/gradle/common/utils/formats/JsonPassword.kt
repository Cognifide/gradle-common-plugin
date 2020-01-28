package com.cognifide.gradle.common.utils.formats

import com.cognifide.gradle.common.utils.Formats
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class JsonPassword : JsonSerializer<String>() {

    override fun serialize(value: String?, generator: JsonGenerator, serializers: SerializerProvider) {
        generator.writeObject(value?.let { Formats.toPassword(value) })
    }
}
