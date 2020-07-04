package io.jpsison.ship_processor

import com.google.auto.common.MoreTypes
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asTypeName
import io.jpsison.ship_processor.extensions.javaToKotlinType
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

class DTOProcessor(private val context: Context, annotationValue: AnnotationValue) {

    val fields: List<Field> by lazy {
        process()
    }

    val model = TO_TYPE.visit(annotationValue)
    val modelElement = MoreTypes.asElement(model)
    val declaredType = MoreTypes.asDeclared(modelElement.asType())
    val name = ClassName.bestGuess(declaredType.toString())

    private fun process(): List<Field> {

        val members = context.environment.elementUtils.getAllMembers(modelElement as TypeElement)
            .filter { it.kind == ElementKind.FIELD }
            .filter { it is VariableElement }
            .map { it as VariableElement }
            .toSet()

        val fields = members.map {
            val member = context.environment.typeUtils.asMemberOf(declaredType, it)
            Field(member, member.asTypeName().javaToKotlinType(), member.kind, it.simpleName.toString())
        }
        return fields.filter {
            it.simpleName != "CONTENTS_FILE_DESCRIPTOR" &&
            it.simpleName != "PARCELABLE_WRITE_RETURN_VALUE" &&
            it.simpleName != "CREATOR"
        }
    }
}
