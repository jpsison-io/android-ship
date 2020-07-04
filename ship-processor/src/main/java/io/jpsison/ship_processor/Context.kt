package io.jpsison.ship_processor

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

data class Context(val environment: ProcessingEnvironment, val element: Element)
