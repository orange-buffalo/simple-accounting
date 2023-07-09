package io.orangebuffalo.simpleaccounting.infra.utils

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

fun yamlObjectMapper() : YAMLMapper = YAMLMapper.builder().addModule(kotlinModule()).build()
