package io.github.tbib.nav3generator.compiler

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.*

class NavProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val navGenerateSymbols = resolver.getSymbolsWithAnnotation("io.github.tbib.nav3generator.annotations.NavGenerate")
            .filterIsInstance<KSClassDeclaration>()

        val navDestinationSymbols = resolver.getSymbolsWithAnnotation("io.github.tbib.nav3generator.annotations.NavDestination")
            .filterIsInstance<KSFunctionDeclaration>()

        val navInterface = navGenerateSymbols.firstOrNull() ?: return emptyList()
        val destinations = navDestinationSymbols.toList()

        if (destinations.isEmpty()) return emptyList()

        // Scan for all top-level functions to find correct wrapper packages
        val allFunctions = mutableMapOf<String, ClassName>()
        resolver.getAllFiles().forEach { file ->
            collectFunctions(file.declarations, allFunctions)
        }

        generateAllInOne(navInterface, destinations, allFunctions)

        return emptyList()
    }

    private fun collectFunctions(declarations: Sequence<KSDeclaration>, map: MutableMap<String, ClassName>) {
        declarations.forEach { decl ->
            if (decl is KSFunctionDeclaration) {
                map[decl.simpleName.asString()] = ClassName(decl.packageName.asString(), decl.simpleName.asString())
            } else if (decl is KSClassDeclaration) {
                collectFunctions(decl.declarations, map)
            }
        }
    }

    private fun KSType.isFunctionTypeCustom(): Boolean {
        val decl = declaration
        if (decl !is KSClassDeclaration) return false
        val qName = decl.qualifiedName?.asString() ?: ""
        return qName.startsWith("kotlin.Function") || qName.startsWith("kotlin.coroutines.SuspendFunction")
    }

    private fun generateAllInOne(
        navInterface: KSClassDeclaration, 
        destinations: List<KSFunctionDeclaration>,
        allFunctions: Map<String, ClassName>
    ) {
        val packageName = navInterface.packageName.asString()
        val interfaceName = navInterface.simpleName.asString()
        val generatedName = "${interfaceName}Destinations"
        val fileName = generatedName

        val fileBuilder = FileSpec.builder(packageName, fileName)
        
        fileBuilder.addImport("androidx.navigation3.runtime", "NavEntry", "NavKey", "rememberNavBackStack", "NavBackStack")
        fileBuilder.addImport("androidx.navigation3.ui", "NavDisplay")
        fileBuilder.addImport("kotlinx.serialization", "Serializable")
        fileBuilder.addImport("kotlinx.serialization.modules", "SerializersModule", "polymorphic", "subclass")
        fileBuilder.addImport("androidx.compose.runtime", "Composable", "remember", "staticCompositionLocalOf", "ProvidableCompositionLocal")
        fileBuilder.addImport("androidx.savedstate.serialization", "SavedStateConfiguration")

        val routesClassName = navInterface.toClassName()
        val navKeyClassName = ClassName("androidx.navigation3.runtime", "NavKey")
        val generatedInterfaceType = ClassName(packageName, generatedName)

        // 1. Destinations Sealed Interface
        val generatedInterface = TypeSpec.interfaceBuilder(generatedName)
            .addModifiers(KModifier.SEALED)
            .addSuperinterface(routesClassName)

        val groupSpecs = mutableMapOf<String, TypeSpec.Builder>()
        
        destinations.forEach { dest ->
            val annotation = dest.annotations.find { it.shortName.asString() == "NavDestination" }
            val customName = annotation?.arguments?.find { it.name?.asString() == "name" }?.value as? String
            val groupName = annotation?.arguments?.find { it.name?.asString() == "group" }?.value as? String
            val routeName = if (!customName.isNullOrBlank()) customName else dest.simpleName.asString().removeSuffix("Screen")

            val routeParams = dest.parameters.filter { !it.type.resolve().isFunctionTypeCustom() }
            
            val routeSpec = if (routeParams.isEmpty()) {
                TypeSpec.objectBuilder(routeName).addModifiers(KModifier.PUBLIC)
            } else {
                val classBuilder = TypeSpec.classBuilder(routeName).addModifiers(KModifier.PUBLIC, KModifier.DATA)
                val constructor = FunSpec.constructorBuilder()
                routeParams.forEach { param ->
                    val pName = param.name?.asString() ?: ""
                    val pType = param.type.toTypeName()
                    constructor.addParameter(pName, pType)
                    classBuilder.addProperty(PropertySpec.builder(pName, pType).initializer("%L", pName).build())
                }
                classBuilder.primaryConstructor(constructor.build())
                classBuilder
            }

            routeSpec.addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
            routeSpec.addSuperinterface(generatedInterfaceType)
            routeSpec.addSuperinterface(navKeyClassName)

            if (!groupName.isNullOrBlank()) {
                groupSpecs.getOrPut(groupName) { 
                    TypeSpec.objectBuilder(groupName).addModifiers(KModifier.PUBLIC) 
                }.addType(routeSpec.build())
            } else {
                generatedInterface.addType(routeSpec.build())
            }

            // 2. Actions and Local for each Screen (if needed)
            val callbackParams = dest.parameters.filter { it.type.resolve().isFunctionTypeCustom() }
            if (callbackParams.isNotEmpty()) {
                val actionsClassName = "${interfaceName}${routeName}Actions"
                val actionsClass = TypeSpec.classBuilder(actionsClassName).addModifiers(KModifier.PUBLIC, KModifier.DATA)
                val actionsConstructor = FunSpec.constructorBuilder()
                callbackParams.forEach { param ->
                    val name = param.name?.asString() ?: ""
                    val type = param.type.toTypeName()
                    actionsConstructor.addParameter(name, type)
                    actionsClass.addProperty(PropertySpec.builder(name, type).initializer(name).build())
                }
                actionsClass.primaryConstructor(actionsConstructor.build())
                fileBuilder.addType(actionsClass.build())

                fileBuilder.addProperty(PropertySpec.builder("Local${actionsClassName}", 
                    ClassName("androidx.compose.runtime", "ProvidableCompositionLocal").parameterizedBy(ClassName(packageName, actionsClassName)))
                    .addModifiers(KModifier.PUBLIC)
                    .initializer("staticCompositionLocalOf { error(\"No $actionsClassName provided\") }")
                    .build())
            }
        }
        groupSpecs.values.forEach { generatedInterface.addType(it.build()) }

        // Companion Object for Serializers
        val companionBuilder = TypeSpec.companionObjectBuilder()
        companionBuilder.addProperty(PropertySpec.builder("${interfaceName}Serializers", ClassName("kotlinx.serialization.modules", "SerializersModule"))
            .addModifiers(KModifier.PUBLIC)
            .getter(FunSpec.getterBuilder()
                .addCode(buildCodeBlock {
                    beginControlFlow("return SerializersModule")
                    beginControlFlow("polymorphic(NavKey::class)")
                    destinations.forEach { dest ->
                         val annotation = dest.annotations.find { it.shortName.asString() == "NavDestination" }
                         val customName = annotation?.arguments?.find { it.name?.asString() == "name" }?.value as? String
                         val groupName = annotation?.arguments?.find { it.name?.asString() == "group" }?.value as? String
                         val routeName = if (!customName.isNullOrBlank()) customName else dest.simpleName.asString().removeSuffix("Screen")
                         val fullRoutePath = if (!groupName.isNullOrBlank()) "$groupName.$routeName" else routeName
                         addStatement("subclass(%L.%L::class, %L.%L.serializer())", generatedName, fullRoutePath, generatedName, fullRoutePath)
                    }
                    endControlFlow().endControlFlow()
                }).build())
            .build())
        generatedInterface.addType(companionBuilder.build())
        fileBuilder.addType(generatedInterface.build())

        // 3. Helper
        fileBuilder.addFunction(FunSpec.builder("remember${interfaceName}BackStack")
            .addModifiers(KModifier.PUBLIC).addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
            .addParameter("initialKey", routesClassName)
            .returns(ClassName("androidx.navigation3.runtime", "NavBackStack").parameterizedBy(routesClassName))
            .addCode(buildCodeBlock {
                beginControlFlow("val config = remember")
                beginControlFlow("SavedStateConfiguration")
                addStatement("serializersModule = ${generatedName}.${interfaceName}Serializers")
                endControlFlow().endControlFlow()
                addStatement("@Suppress(\"UNCHECKED_CAST\")")
                addStatement("return rememberNavBackStack(config, initialKey) as NavBackStack<%T>", routesClassName)
            }).build())

        // 4. routesEntryProvider
        val entryProviderFun = FunSpec.builder("${interfaceName.replaceFirstChar { it.lowercase() }}EntryProvider")
            .addModifiers(KModifier.PUBLIC).addParameter("key", routesClassName)
            .returns(ClassName("androidx.navigation3.runtime", "NavEntry").parameterizedBy(routesClassName))
            .beginControlFlow("return when (key)")
            
        destinations.forEach { dest ->
            val annotation = dest.annotations.find { it.shortName.asString() == "NavDestination" }
            val customName = annotation?.arguments?.find { it.name?.asString() == "name" }?.value as? String
            val groupName = annotation?.arguments?.find { it.name?.asString() == "group" }?.value as? String
            val wrapperName = annotation?.arguments?.find { it.name?.asString() == "wrapper" }?.value as? String
            val routeName = if (!customName.isNullOrBlank()) customName else dest.simpleName.asString().removeSuffix("Screen")
            val fullRoutePath = if (!groupName.isNullOrBlank()) "$groupName.$routeName" else routeName

            entryProviderFun.beginControlFlow("is %L.%L ->", generatedName, fullRoutePath)
            entryProviderFun.beginControlFlow("NavEntry(key)")
            
            if (!wrapperName.isNullOrBlank()) {
                val wrapperClassName = allFunctions[wrapperName]
                if (wrapperClassName != null) {
                    entryProviderFun.beginControlFlow("%T", wrapperClassName)
                } else {
                    entryProviderFun.beginControlFlow("%L", wrapperName)
                }
            }

            val args = mutableListOf<String>()
            val actionsClassName = "${interfaceName}${routeName}Actions"

            dest.parameters.forEach { param ->
                val name = param.name?.asString() ?: ""
                if (param.type.resolve().isFunctionTypeCustom()) {
                    args.add("$name = Local${actionsClassName}.current.$name")
                } else {
                    args.add("$name = key.$name")
                }
            }
            
            val screenClassName = ClassName(dest.packageName.asString(), dest.simpleName.asString())
            entryProviderFun.addStatement("%T(%L)", screenClassName, args.joinToString(", "))
            
            if (!wrapperName.isNullOrBlank()) entryProviderFun.endControlFlow()
            entryProviderFun.endControlFlow().endControlFlow()
        }
        entryProviderFun.addStatement("else -> error(\"Unknown NavKey ${'$'}key\")").endControlFlow()
        fileBuilder.addFunction(entryProviderFun.build())

        fileBuilder.build().writeTo(codeGenerator, true)
    }
}
