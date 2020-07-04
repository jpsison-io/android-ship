package io.jpsison.ship_processor

import com.squareup.kotlinpoet.TypeName
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

data class Field(
    val member: TypeMirror,
    val name: TypeName,
    val type: TypeKind,
    val simpleName: String
)
