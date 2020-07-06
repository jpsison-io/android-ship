package io.jpsison.ship_processor.processors

import com.google.common.collect.ImmutableSet
import com.squareup.kotlinpoet.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.jpsison.ship_processor.DTOProcessor
import java.io.File
import java.io.IOException
import javax.tools.DocumentationTool
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

open class ShipProcessor : AbstractProcessor() {

    protected lateinit var messager: Messager
    protected lateinit var filer: Filer
    protected lateinit var processor: ProcessingEnvironment
    protected lateinit var elementUtils: Elements

    private val androidPackage = "android.content"
    private val libraryPackage = "io.jpsison.ship_annotation"

    private val kClass = ClassName.bestGuess("kotlin.reflect.KClass")
    private val kLazy = ClassName.bestGuess("kotlin.Lazy")
    private val kAny = ClassName.bestGuess("kotlin.Any")
    private val kSuppress = ClassName.bestGuess("kotlin.Suppress")

    private val kCargo = ClassName.bestGuess("${libraryPackage}.Cargo")
    private val kParcel = ClassName.bestGuess("${libraryPackage}.Parcel")

    private val kActivity = ClassName.bestGuess("android.app.Activity")
    val kFragment = ClassName.bestGuess("androidx.fragment.app.Fragment")
    val kKeep = ClassName.bestGuess("androidx.annotation.Keep")
    val kIntent = ClassName.bestGuess("$androidPackage.Intent")
    val kContext = ClassName.bestGuess("$androidPackage.Context")
    val kBundle = ClassName.bestGuess("android.os.Bundle")

    var argName = "navargs"

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        filer = processingEnv.filer
        createCargoInterface()
        createCargoExtension()
    }

    override fun process(
        set: Set<TypeElement?>,
        roundEnv: RoundEnvironment
    ): Boolean {
        return true
    }

    private fun createCargoInterface() {
        val file = FileSpec.builder(libraryPackage, kCargo.simpleName)
        val cargo = TypeSpec.interfaceBuilder("Cargo")
            .addModifiers(KModifier.PUBLIC)
            .build()

        try {
            file.addType(cargo)
                .build()
                .writeTo(filer)
        } catch (e: IOException) {
            if (!e.localizedMessage.contains("Attempt to reopen a file for path", true)) {
                e.printStackTrace()
            }
        }
    }

    private fun createCargoExtension() {
        val lineType = LambdaTypeName.get(returnType = kAny)
        val cargoType = kClass.parameterizedBy(kParcel)

        val file = FileSpec.builder(libraryPackage, "CargoExtension")
            .addAliasedImport(kActivity, kActivity.simpleName)
            .addAliasedImport(kFragment, kFragment.simpleName)
            .addType(TypeSpec.classBuilder("CargoLazy")
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("cargo", cargoType)
                        .addParameter("line", lineType)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("cargo", cargoType)
                        .initializer("cargo")
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("line", lineType)
                        .initializer("line")
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                )
                .addTypeVariable(TypeVariableName("Parcel: Cargo"))
                .addSuperinterface(kLazy.parameterizedBy(kParcel))
                .addAnnotation(AnnotationSpec.builder(kSuppress)
                    .addMember("\"UNCHECKED_CAST\"")
                    .build())
                .addProperty(
                    PropertySpec.builder("cached", kParcel.copy(nullable = true))
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("null")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("value", kParcel, KModifier.OVERRIDE)
                        .getter(FunSpec.getterBuilder()
                            .addCode("""
                            |val args = cached
                            |if (args == null) {
                            |    val caller = line.invoke()
                            |    return if (caller is Activity) {
                            |        cargo.constructors.first().call(caller.intent.extras)
                            |    } else if (caller is Fragment) {
                            |        cargo.constructors.first().call(caller.arguments)
                            |    } else {
                            |        throw IllegalStateException("Cargo must be applied to an Activity or Fragment only")
                            |    }
                            |}
                            |return args
                            |""".trimMargin())
                            .build())
                        .build()
                )
                .addFunction(
                    FunSpec.builder("isInitialized")
                        .addModifiers(KModifier.OVERRIDE)
                        .addStatement("return cached != null")
                        .build()
                )
                .build())
            .addFunction(FunSpec.builder("cargo")
                .receiver(kActivity)
                .addModifiers(KModifier.INLINE)
                .addTypeVariable(TypeVariableName("reified Parcel: Cargo"))
                .addStatement("return  CargoLazy(Parcel::class) { this }")
                .build())
            .addFunction(FunSpec.builder("cargo")
                .receiver(kFragment)
                .addModifiers(KModifier.INLINE)
                .addTypeVariable(TypeVariableName("reified Parcel: Cargo"))
                .addStatement("return  CargoLazy(Parcel::class) { this }")
                .build())
            .build()
        try {
            file.writeTo(filer)
        } catch (e: IOException) {
            if (!e.localizedMessage.contains("Attempt to reopen a file for path", true)) {
                e.printStackTrace()
            }
        }
    }

    protected fun createArgClass(
        packageName: String,
        className: String,
        dtoProcessor: DTOProcessor
    ) {
        val argsClass = ClassName(packageName, "${className}Args")
        val argsFile = FileSpec.builder(packageName, argsClass.simpleName)
        val buildArgs = initArgContent(argsClass, dtoProcessor)
            .superclass(kCargo)
            .addAnnotation(kKeep)

        dtoProcessor.fields.forEach {
            val getter = PropertySpec.builder(it.simpleName, it.name.copy(nullable = true))
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return cargo?.${it.simpleName}")
                        .build()
                ).build()
            buildArgs.addProperty(getter)
        }

        argsFile.addType(buildArgs.build())
            .build()
            .writeTo(filer)
    }

    private fun initArgContent(
        builderClass: ClassName,
        dtoProcessor: DTOProcessor
    ): TypeSpec.Builder {

        val buildFile = TypeSpec.classBuilder(builderClass.simpleName)
            .addFunction(
                FunSpec.constructorBuilder()
                    .addParameter("bundle", kBundle.copy(true))
                    .addStatement("bundle ?: return")
                    .addStatement("intent = Intent().apply { putExtras(bundle) }")
                    .addStatement("cargo = intent?.getParcelableExtra(\"$argName\")")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("navargs", ClassName.bestGuess("kotlin.String"))
                    .initializer("\"$argName\"")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )

        var declaration = PropertySpec.builder("intent", kIntent.copy(nullable = true))
            .mutable()
            .addModifiers(KModifier.PRIVATE)
            .initializer("null")
            .build()
        buildFile.addProperty(declaration)

        declaration = PropertySpec.builder("bundle", kBundle.copy(nullable = true))
            .mutable()
            .addModifiers(KModifier.PRIVATE)
            .initializer("null")
            .build()
        buildFile.addProperty(declaration)

        declaration = PropertySpec.builder("cargo", dtoProcessor.name.copy(nullable = true))
            .mutable()
            .addModifiers(KModifier.PRIVATE)
            .initializer("null")
            .build()
        buildFile.addProperty(declaration)
        return buildFile
    }

    override fun getSupportedAnnotationTypes(): Set<String?>? {
        return ImmutableSet.of(
            ActivityShipProcessor::class.java.canonicalName,
            FragmentShipProcessor::class.java.canonicalName
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion? {
        return SourceVersion.latestSupported()
    }
}
