package io.jpsison.ship_processor.processors

import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.common.collect.ImmutableSet
import com.squareup.kotlinpoet.*
import io.jpsison.ship_annotation.FragmentShip
import io.jpsison.ship_processor.Context
import io.jpsison.ship_processor.DTOProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class FragmentShipProcessor : ShipProcessor() {

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        messager = processingEnv.messager
        elementUtils = processingEnv.elementUtils
        filer = processingEnv.filer
        processor = processingEnv
    }

    override fun process(
        set: Set<TypeElement?>,
        roundEnv: RoundEnvironment
    ): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(FragmentShip::class.java)
        for (element in elements) {
            if (element.kind !== ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.")
                return true
            }
            val typeElement = element as TypeElement
            val className = typeElement.simpleName.toString()
            val packageName = elementUtils.getPackageOf(typeElement).qualifiedName.toString()

            val annotationMirror =
                MoreElements.getAnnotationMirror(element, FragmentShip::class.java).orNull()
            val entity = AnnotationMirrors.getAnnotationValue(annotationMirror, "cargo")
            val myContext = Context(processor, element)
            val dtoProcessor =
                DTOProcessor(myContext, entity)
            argName = "$argName$className"

            createBuilderClass(packageName, className, typeElement, dtoProcessor)
            createArgClass(packageName, className, dtoProcessor)

        }
        return super.process(set, roundEnv)
    }

    private fun createBuilderClass(
        packageName: String,
        className: String,
        typeElement: TypeElement,
        dtoProcessor: DTOProcessor
    ) {

        val builderClass = ClassName(packageName, "${className}Builder")
        val builderFile = FileSpec.builder(packageName, builderClass.simpleName)
                .addAliasedImport(kBundle, "Bundle")
        val buildBuilder = initBuilderContent(builderClass, typeElement)


        var declaration = PropertySpec.builder("cargo", dtoProcessor.name)
            .mutable()
            .addModifiers(KModifier.PRIVATE)
            .initializer("${dtoProcessor.name}()")
            .build()
        buildBuilder.addProperty(declaration)

        dtoProcessor.fields.forEach {
            val name = it.simpleName.split("(?=\\p{Upper})").map {
                it.capitalize()
            }.joinToString("")

            declaration = PropertySpec.builder(it.simpleName, it.name.copy(nullable = true))
                .mutable()
                .addModifiers(KModifier.PRIVATE)
                .initializer("null")
                .build()
            buildBuilder.addProperty(declaration)

                // Setters
                val setter = FunSpec.builder("set$name")
                    .addParameter(it.simpleName, it.name)
                    .addStatement("return apply { this.${it.simpleName} = ${it.simpleName}}")
                    .build()
                buildBuilder.addFunction(setter)
        }

        val variables = dtoProcessor.fields.map {
            "${it.simpleName} ?: cargo.${it.simpleName}"
        }.joinToString(", ")

        buildBuilder.addFunction(
            FunSpec.builder("create")
                .addStatement("cargo = ${dtoProcessor.name}($variables)")
                .addCode("""
                |fragment.arguments = Bundle().apply { 
                |  putParcelable(navargs, cargo)
                |}
                |""".trimMargin())
                .addStatement("return fragment")
                .returns(kFragment)
                .build()
        )
        builderFile.addType(buildBuilder.build())
            .build()
            .writeTo(filer)
    }

    private fun initBuilderContent(
        builderClass: ClassName,
        typeElement: TypeElement
    ): TypeSpec.Builder {
        return TypeSpec.classBuilder(builderClass.simpleName)
            .addProperty(
                PropertySpec.builder("fragment", kFragment)
                    .initializer("$typeElement()")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("navargs", ClassName.bestGuess("kotlin.String"))
                    .initializer("\"$argName\"")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
    }

    override fun getSupportedAnnotationTypes(): Set<String?>? {
        return ImmutableSet.of(FragmentShip::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion? {
        return SourceVersion.latestSupported()
    }
}
