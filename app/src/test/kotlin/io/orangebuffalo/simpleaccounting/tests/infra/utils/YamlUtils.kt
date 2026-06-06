package io.orangebuffalo.simpleaccounting.tests.infra.utils

import tools.jackson.databind.DeserializationFeature
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule

fun yamlObjectMapper() : YAMLMapper = YAMLMapper.builder()
    .addModule(kotlinModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()
