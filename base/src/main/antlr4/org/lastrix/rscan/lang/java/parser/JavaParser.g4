parser grammar JavaParser;

options { tokenVocab = JavaLexer; language = Java; }

@header
{
package org.lastrix.rscan.lang.java.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import org.lastrix.rscan.model.*;
import org.lastrix.rscan.model.tokens.RScanToken;
import org.lastrix.rscan.vfs.*;
import org.lastrix.rscan.model.operation.*;
import org.lastrix.rscan.model.operation.raw.*;
import org.lastrix.rscan.model.operation.std.*;
import org.lastrix.rscan.lang.java.meta.*;
import org.lastrix.rscan.lang.java.parser.model.operation.*;

import java.util.*;
}

//////////////////////////////////////// Main Entry Points /////////////////////////////////////////////////////////////
startJava returns [@NotNull RLangOp result]
    :   EOF { $result = opEmptyLang(); }
    |   compilationUnit { $result = opLang($compilationUnit.start, $compilationUnit.stop, Collections.singletonList($compilationUnit.result)); }
        EOF
    ;

//////////////////////////////////////// Secondary Entry Points ////////////////////////////////////////////////////////
startModuleDeclBody returns [ROp result]
    :   moduleDeclBody      { $result = $moduleDeclBody.result; }
        EOF
    ;

startClassBody returns [ROp result]
    :   classBody           { $result = $classBody.result; }
        EOF
    ;

startEnumBody returns [ROp result]
    :   enumBody            { $result = $enumBody.result; }
        EOF
    ;

startInterfaceBody returns [ROp result]
    :   interfaceBody       { $result = $interfaceBody.result; }
        EOF
    ;

startAnnotationTypeBody returns [ROp result]
    :   annotationTypeBody  { $result = $annotationTypeBody.result; }
        EOF
    ;

startConstructorBody returns [ROp result]
    :   constructorBody     { $result = $constructorBody.result; }
        EOF
    ;

startElementValueArrayInitializer returns [ROp result]
    :   elementValueArrayInitializer { $result = $elementValueArrayInitializer.result; }
        EOF
    ;

startArrayInitializer returns [ROp result]
    :   arrayInitializer    { $result = $arrayInitializer.result; }
        EOF
    ;

startBlock returns [ROp result]
    :   block               { $result = $block.result; }
        EOF
    ;

startSwitchBlock returns [ROp result]
    :   switchBlock         { $result = $switchBlock.result; }
        EOF
    ;

//////////////////////////////////////// Actual grammar ////////////////////////////////////////////////////////////////
compilationUnit returns [ROp result]
    :   ordinaryCompilationUnit { $result = $ordinaryCompilationUnit.result; }
    |   modularCompilationUnit  { $result = $modularCompilationUnit.result; }
    ;

ordinaryCompilationUnit returns [ROp result]
locals [List<ROp> list]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), JavaOpType.RAW_DECL_PACKAGE, $list); }
    :   (
            (packageModifier { $list.add($packageModifier.result); })*
            PACKAGE qualifiedName { $list.add($qualifiedName.result); }
            SEMI
        )?
        (   importDecl { $list.add($importDecl.result); }
        |   SEMI
        )*
        (typeDecl { $list.add($typeDecl.result); })*
    ;

modularCompilationUnit returns [ROp result]
locals [List<ROp> list]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), RawOpType.RAW_DECL_MODULE, $list); }
    :   (   importDecl { $list.add($importDecl.result); }
        |   SEMI
        )*
        (annotation { $list.add($annotation.result); })*
        (OPEN { $list.add(opModifier($OPEN, JavaModifier.Open())); } )?
        MODULE qualifiedName  { $list.add($qualifiedName.result); }
        moduleDeclBody { $list.add($moduleDeclBody.result); }
    ;


packageModifier returns [ROp result]
    :   annotation { $result = $annotation.result; }
    ;

importDecl returns [ROp result]
locals [ boolean isStatic, List<ROp> list, List<RModifier> modifiers]
@init { $list = new ArrayList<>(); $modifiers = new ArrayList<>(); }
@after {
    Statement st= evalStatement($start, $stop);
    if ( !$modifiers.isEmpty() ) { $list.add(opModifiers(st, $modifiers)); }
    $result = opNode(st, RawOpType.RAW_IMPORT, $list);
}
    :   IMPORT
        (STATIC { $modifiers.add(JavaModifier.Static()); })?
        qualifiedName
        { $list.add($qualifiedName.result); }
        (DOT MUL { $modifiers.add(JavaModifier.Wildcard()); } )?
        SEMI
    ;

typeDecl returns [ ROp result ]
    :   classDecl     { $result = $classDecl.result; }
    |   interfaceDecl { $result = $interfaceDecl.result; }
    ;

moduleDeclBody returns [ ROp result ]
    :   FoldBlock { $result = opFoldBlock((RScanToken)$FoldBlock, "startModuleDeclBody"); }
    |   { List<ROp> list = new ArrayList<>(); }
        LBRACE
            (moduleDirective { list.add($moduleDirective.result); } SEMI)*
        RBRACE
        { $result = opNode(evalStatement($LBRACE, $RBRACE), StdOpType.MEMBERS, list); }
    ;

moduleDirective returns [ ROp result ]
locals [List<ROp> list, ROpType directiveType ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start, $stop), $directiveType, $list); }
    :   REQUIRES            { $directiveType = JavaOpType.RAW_DIRECTIVE_REQUIRES; }
        (requiresModifier   { $list.add($requiresModifier.result); })*
        qualifiedName { $list.add($qualifiedName.result); }
    |   (   EXPORTS         { $directiveType = JavaOpType.RAW_DIRECTIVE_EXPORTS; }
        |   OPENS           { $directiveType = JavaOpType.RAW_DIRECTIVE_OPENS; }
        )
        qualifiedName { $list.add($qualifiedName.result); }
        (TO nameList        { $list.add(opNode(JavaOpType.TO, $nameList.result)); })?
    |   USES                { $directiveType = JavaOpType.RAW_DIRECTIVE_USES; }
        qualifiedTypeName            { $list.add($qualifiedTypeName.result); }
    |   PROVIDES            { $directiveType = JavaOpType.RAW_DIRECTIVE_PROVIDES; }
        qualifiedTypeName            { $list.add($qualifiedTypeName.result); }
        WITH qualifiedTypeNameList   { $list.add(opNode(evalStatement($WITH, $qualifiedTypeNameList.stop), JavaOpType.WITH, $qualifiedTypeNameList.result));  }
    ;

requiresModifier returns [ ROp result ]
    :   TRANSITIVE { $result = opModifier($TRANSITIVE, JavaModifier.Transitive()); }
    |   STATIC     { $result = opModifier($STATIC, JavaModifier.Static()); }
    ;

//////////////////////////////////////// Classes and interfaces ////////////////////////////////////////////////////////
classDecl returns [ ROp result ]
    :   normalClassDecl { $result = $normalClassDecl.result; }
    |   enumDecl        { $result = $enumDecl.result; }
    ;

normalClassDecl returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_CLASS, $list);
}
    :   (classModifier       { $list.add($classModifier.result); })*
        CLASS name           { $list.add($name.result); }
        (typeParameters      { $list.addAll($typeParameters.result); })?
        (superClass          { $list.add($superClass.result); })?
        (superInterfaces     { $list.add($superInterfaces.result); })?
        classBody            { $list.add($classBody.result); }
    ;

classModifier returns [ROp result]
    :   annotation { $result = $annotation.result; }
    |   PUBLIC     { $result = opModifier($PUBLIC, JavaModifier.Public()); }
    |   PROTECTED  { $result = opModifier($PROTECTED, JavaModifier.Protected()); }
    |   PRIVATE    { $result = opModifier($PRIVATE, JavaModifier.Private()); }
    |   ABSTRACT   { $result = opModifier($ABSTRACT, JavaModifier.Abstract()); }
    |   STATIC     { $result = opModifier($STATIC, JavaModifier.Static()); }
    |   FINAL      { $result = opModifier($FINAL, JavaModifier.Final()); }
    |   STRICTFP   { $result = opModifier($STRICTFP, JavaModifier.Strictfp()); }
    ;

superClass returns [ ROp result ]
    :   EXTENDS classType
        { $result = opNode(StdOpType.EXTENDS, opNode(RawOpType.RAW_TYPE, $classType.result)); }
    ;

superInterfaces returns [ ROp result ]
    :   IMPLEMENTS classTypeList
        { $result = opNode(StdOpType.IMPLEMENTS, $classTypeList.result);}
    ;

classBody returns [ ROp result ]
    :   FoldBlock { $result = opFoldBlock((RScanToken)$FoldBlock, "startClassBody" ); }
    |   { List<ROp> list = new ArrayList<>(); }
        LBRACE
            (classBodyDecl { list.add($classBodyDecl.result); })*
        RBRACE
        { $result = opNode(evalStatement($LBRACE, $RBRACE), StdOpType.MEMBERS, list); }
    ;

classBodyDecl returns [ ROp result ]
    :   classMemberDecl      { $result = $classMemberDecl.result; }
    |   instanceInitializer  { $result = $instanceInitializer.result; }
    |   staticInitializer    { $result = $staticInitializer.result; }
    |   constructorDecl      { $result = $constructorDecl.result; }
    |   SEMI                 { $result = opNone($SEMI); }
    ;

classMemberDecl returns [ ROp result ]
    :   classDecl       { $result = $classDecl.result; }
    |   interfaceDecl   { $result = $interfaceDecl.result; }
    |   fieldDecl SEMI  { $result = $fieldDecl.result; }
    |   methodDecl      { $result = $methodDecl.result; }
    ;

fieldDecl returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_FIELD, $list); }
    :   (fieldModifier  { $list.add($fieldModifier.result); } )*
        unannType       { $list.add($unannType.result); }
        variableDeclList[$list]
    ;

fieldModifier returns [ROp result]
    :   annotation     { $result = $annotation.result; }
    |   PUBLIC         { $result = opModifier($PUBLIC, JavaModifier.Public()); }
    |   PROTECTED      { $result = opModifier($PROTECTED, JavaModifier.Protected()); }
    |   PRIVATE        { $result = opModifier($PRIVATE, JavaModifier.Private()); }
    |   STATIC         { $result = opModifier($STATIC, JavaModifier.Static()); }
    |   FINAL          { $result = opModifier($FINAL, JavaModifier.Final()); }
    |   TRANSIENT      { $result = opModifier($TRANSIENT, JavaModifier.Transient()); }
    |   VOLATILE       { $result = opModifier($VOLATILE, JavaModifier.Volatile()); }
    ;

variableDeclList [ List<ROp> list ]
    :   variableDecl        { $list.add($variableDecl.result); }
        (COMMA variableDecl { $list.add($variableDecl.result); } )*
    ;

variableDecl returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), RawOpType.RAW_DECL_VARIABLE, $list); }
    :   name                        { $list.add($name.result); }
        (   arrayDims               { $list.add($arrayDims.result); }
        )?
        (   ASSIGN
            variableInitializer     { $list.add(opNode(StdOpType.INIT, $variableInitializer.result)); }
        )?
    ;

instanceInitializer returns [ ROp result ]
    :   block            { $result = opNode(JavaOpType.INITIALIZER, $block.result); }
    ;

staticInitializer returns [ ROp result ]
    :   STATIC block      { $result = opNode(JavaOpType.STATIC_INITIALIZER, $block.result); }
    ;

enumDecl returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_ENUM, $list);
}
    :   (classModifier   { $list.add($classModifier.result); })*
        ENUM
        name             { $list.add($name.result); }
        (superInterfaces { $list.add($superInterfaces.result); })?
        enumBody         { $list.add($enumBody.result); }
    ;

enumBody returns [ ROp result ]
    :   FoldBlock { $result = opFoldBlock((RScanToken)$FoldBlock, "startEnumBody"); }
    |   { List<ROp> list = new ArrayList<>(); }
        LBRACE
            (enumConstantList[list])? COMMA?
            (enumBodyDeclarations[list])?
        RBRACE
        { $result = opNode(evalStatement($LBRACE, $RBRACE), StdOpType.MEMBERS, list); }
    ;

enumConstantList[List<ROp> list]
    :   enumConstant        { $list.add($enumConstant.result); }
        (COMMA enumConstant { $list.add($enumConstant.result); })*
    ;

enumConstant returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), RawOpType.RAW_DECL_ENUM_MEMBER, $list); }
    :   (enumConstantModifier { $list.add($enumConstantModifier.result); })*
        identifier            { $list.add($identifier.result); }
        (   { List<ROp> args = Collections.emptyList(); }
            LPAREN
                (argumentList { args = $argumentList.result; })?
            RPAREN
            { $list.add(opNode(evalStatement($LPAREN,$RPAREN), StdOpType.ARGUMENTS, args)); }
        )?
        (classBody            { $list.add($classBody.result); })?
    ;

enumConstantModifier returns [ ROp result ]
    :   annotation { $result = $annotation.result; }
    ;

enumBodyDeclarations[List<ROp> list]
    :   SEMI
        (classBodyDecl { $list.add($classBodyDecl.result); })*
    ;
    
interfaceDecl returns [ ROp result ]
    :   normalInterfaceDecl { $result = $normalInterfaceDecl.result; }
    |   annotationTypeDecl  { $result = $annotationTypeDecl.result; }
    ;
    
normalInterfaceDecl returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_INTERFACE, $list);
}
    :   (interfaceModifier { $list.add($interfaceModifier.result); })*
        INTERFACE
        name               { $list.add($name.result); }
        (typeParameters    { $list.addAll($typeParameters.result); })?
        (EXTENDS classTypeList { $list.add(opNode(StdOpType.EXTENDS, $classTypeList.result)); })?
        interfaceBody      { $list.add($interfaceBody.result); }
    ; 
    
interfaceModifier returns [ ROp result ]
    :   annotation    { $result = $annotation.result; }
    |   PUBLIC        { $result = opModifier($PUBLIC, JavaModifier.Public()); }
    |   PROTECTED     { $result = opModifier($PROTECTED, JavaModifier.Protected()); }
    |   PRIVATE       { $result = opModifier($PRIVATE, JavaModifier.Private()); }
    |   ABSTRACT      { $result = opModifier($ABSTRACT, JavaModifier.Abstract()); }
    |   STATIC        { $result = opModifier($STATIC, JavaModifier.Static()); }
    |   STRICTFP      { $result = opModifier($STRICTFP, JavaModifier.Strictfp()); }
    ;  

interfaceBody returns [ ROp result ]
    :   FoldBlock  { $result = opFoldBlock((RScanToken)$FoldBlock, "startInterfaceBody"); }
    |   { List<ROp> list = new ArrayList<>(); }
        LBRACE
            (interfaceMemberDecl { list.add($interfaceMemberDecl.result); })*
        RBRACE
        { $result = opNode(evalStatement($LBRACE, $RBRACE), StdOpType.MEMBERS, list); }
    ;
    
interfaceMemberDecl returns [ ROp result ]
    :   constantDecl SEMI   { $result = $constantDecl.result; }
    |   classDecl           { $result = $classDecl.result; }
    |   interfaceDecl       { $result = $interfaceDecl.result; }
    |   interfaceMethodDecl { $result = $interfaceMethodDecl.result; }
    |   SEMI                { $result = opNone($SEMI); }
    ; 
    
constantDecl returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_FIELD, $list);
}
    :   (constantModifier { $list.add($constantModifier.result); })*
        unannType         { $list.add($unannType.result); }
        variableDecl      { $list.add($variableDecl.result); }
    ;     

constantModifier returns [ ROp result ]
    :   annotation  { $result = $annotation.result; }
    |   PUBLIC      { $result = opModifier($PUBLIC, JavaModifier.Public()); }
    |   STATIC      { $result = opModifier($STATIC, JavaModifier.Static()); }
    |   FINAL       { $result = opModifier($FINAL, JavaModifier.Final()); }
    ;   
    
annotationTypeDecl returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_ANNOTATION, $list);
}
    :   (interfaceModifier    { $list.add($interfaceModifier.result); })*
        AT INTERFACE
        name                  { $list.add($name.result); }
        annotationTypeBody    { $list.add($annotationTypeBody.result); }
    ; 
    
annotationTypeBody returns [ ROp result ]
    :   FoldBlock  { $result = opFoldBlock((RScanToken)$FoldBlock, "startAnnotationTypeBody"); }
    |   { List<ROp> list = new ArrayList<>(); }
        LBRACE
            (annotationTypeMemberDecl { list.add($annotationTypeMemberDecl.result); })*
        RBRACE
        { $result = opNode(evalStatement($LBRACE, $RBRACE), StdOpType.MEMBERS, list); }
    ;

annotationTypeMemberDecl returns [ ROp result ]
    :   (   annotationTypeElementDecl { $result = $annotationTypeElementDecl.result; }
        |   constantDecl              { $result = $constantDecl.result; }
        )
        SEMI
    |   classDecl                     { $result = $classDecl.result; }
    |   interfaceDecl                 { $result = $interfaceDecl.result; }
    |   SEMI                          { $result = opNone($SEMI); }
    ;    

annotationTypeElementDecl returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), JavaOpType.RAW_DECL_ANNOTATION_ELEMENT, $list);
}
    :   (annotationTypeElementModifier { $list.add($annotationTypeElementModifier.result); })*
        unannType                      { $list.add($unannType.result); }
        name                           { $list.add($name.result); }
        LPAREN RPAREN
        (arrayDims                     { $list.add($arrayDims.result); } )?
        (DEFAULT elementValue          { $list.add(opNode(StdOpType.DEFAULT, $elementValue.result)); })?
    ;

annotationTypeElementModifier returns [ ROp result ]
    :   annotation  { $result = $annotation.result; }
    |   PUBLIC      { $result = opModifier($PUBLIC, JavaModifier.Public()); }
    |   ABSTRACT    { $result = opModifier($ABSTRACT, JavaModifier.Abstract()); }
    ;    

//////////////////////////////////////// Methods & lambdas /////////////////////////////////////////////////////////////
methodDecl returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_METHOD, $list);
}
    :   (   methodModifier      { $list.add($methodModifier.result); }
        )*
        methodHeader[$list]
        (   block               { $list.add($block.result); }
        |   SEMI
        )
    ;

methodModifier returns [ ROp result ]
    :   annotation     { $result = $annotation.result; }
    |   PUBLIC         { $result = opModifier($PUBLIC, JavaModifier.Public()); }
    |   PROTECTED      { $result = opModifier($PROTECTED, JavaModifier.Protected()); }
    |   PRIVATE        { $result = opModifier($PRIVATE, JavaModifier.Private()); }
    |   ABSTRACT       { $result = opModifier($ABSTRACT, JavaModifier.Abstract()); }
    |   STATIC         { $result = opModifier($STATIC, JavaModifier.Static()); }
    |   FINAL          { $result = opModifier($FINAL, JavaModifier.Final()); }
    |   SYNCHRONIZED   { $result = opModifier($SYNCHRONIZED, JavaModifier.Synchronized()); }
    |   NATIVE         { $result = opModifier($NATIVE, JavaModifier.Native()); }
    |   STRICTFP       { $result = opModifier($STRICTFP, JavaModifier.Strictfp()); }
    ;

methodHeader[ List<ROp> list ]
    :   (   typeParameters    { $list.addAll($typeParameters.result); }
            (annotation       { $list.add($annotation.result); })*
        )?
        (   VOID              { $list.add(opNode(StdOpType.TYPE, opUnresolvedId($VOID))); }
        |   unannType         { $list.add($unannType.result); }
        )
        name                  { $list.add($name.result); }
        LPAREN
            (   receivedParameter
                              { $list.add($receivedParameter.result); }
                COMMA
            )?
            formalParameterList[$list]?
        RPAREN
        (arrayDims            { $list.add($arrayDims.result); })?
        (throwsClause         { $list.add($throwsClause.result); })?
    ;

receivedParameter returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), JavaOpType.RAW_DECL_RECEIVED_PARAMETER, $list);
}
    :   (annotation     { $list.add($annotation.result); })*
        unannType       { $list.add($unannType.result); }
        (   identifier DOT THIS
                        {
                            $list.add($identifier.result);
                            $list.add(opUnresolvedId($THIS));
                        }
        |   THIS        { $list.add(opName(opUnresolvedId($THIS))); }
        )
    ;

formalParameterList[ List<ROp> list ]
    :   formalParameter            { $list.add($formalParameter.result); }
        (COMMA formalParameter     { $list.add($formalParameter.result); })*
        (COMMA formalParameterRest { $list.add($formalParameterRest.result); })?
    |   formalParameterRest        { $list.add($formalParameterRest.result); }
    ;

formalParameter returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_PARAMETER, $list);
}
    :   (variableModifier { $list.add($variableModifier.result); })*
        unannType         { $list.add($unannType.result); }
        name              { $list.add($name.result); }
        (arrayDims        { $list.add($arrayDims.result); })?
    ;

formalParameterRest returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_PARAMETER, $list);
}
    :   (variableModifier { $list.add($variableModifier.result); } )*
        unannType         { $list.add($unannType.result); }
        (annotation       { $list.add($annotation.result); })*
        ELLIPSIS          { $list.add(opModifier($ELLIPSIS, JavaModifier.VarArgs())); }
        identifier        { $list.add($identifier.result); }
    ;

variableModifier returns [ ROp result ]
    :   annotation  { $result = $annotation.result; }
    |   FINAL       { $result = opModifier($FINAL, JavaModifier.Final()); }
    ;

throwsClause returns [ ROp result ]
    :   THROWS exceptionTypeList
        { $result = opNode(JavaOpType.THROWS, $exceptionTypeList.result); }
    ;

exceptionTypeList returns [ List<ROp> result ]
@init { $result = new ArrayList<>(); }
    :   exceptionType        { $result.add($exceptionType.result); }
        (COMMA exceptionType { $result.add($exceptionType.result); })*
    ;

exceptionType returns [ ROp result ]
@after { $result = opNode(RawOpType.RAW_TYPE, $result); }
    :   classType     { $result = $classType.result; }
    ;

constructorDecl returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_CONSTRUCTOR, $list);
}
    :   (constructorModifier  { $list.add($constructorModifier.result); })*
        (typeParameters       { $list.addAll($typeParameters.result); })?
        name                  { $list.add($name.result); }
        LPAREN
            (   receivedParameter COMMA
                              { $list.add($receivedParameter.result); }
            )?
            formalParameterList[$list]?
        RPAREN
        (throwsClause         { $list.add($throwsClause.result); })?
        constructorBody       { $list.add($constructorBody.result); }
    ;

constructorModifier returns [ ROp result ]
    :   annotation   { $result = $annotation.result; }
    |   PUBLIC       { $result = opModifier($PUBLIC, JavaModifier.Public()); }
    |   PROTECTED    { $result = opModifier($PROTECTED, JavaModifier.Protected()); }
    |   PRIVATE      { $result = opModifier($PRIVATE, JavaModifier.Private()); }
    ;

constructorBody returns [ ROp result ]
    :   FoldBlock  { $result = opFoldBlock((RScanToken)$FoldBlock, "startConstructorBody"); }
    |   { List<ROp> list = new ArrayList<>(); }
        LBRACE
            (explicitConstructorInvocation SEMI { list.add($explicitConstructorInvocation.result); })?
            (blockStmts  { list.addAll($blockStmts.result); })?
        RBRACE
        { $result = opNode(evalStatement($LBRACE, $RBRACE), StdOpType.BLOCK, list); }
    ;

explicitConstructorInvocation returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), JavaOpType.RAW_EXPLICIT_CTOR_INVOKE, $list);
}
    :   (   typeArguments  { $list.add($typeArguments.result); }
        )?
        tos=(THIS | SUPER) { $list.add(opUnresolvedId($tos)); }
        call               { $list.add($call.result); }

    |   atom               { $list.add($atom.result); }
        DOT
        (   typeArguments  { $list.add($typeArguments.result); }
        )?
        SUPER              { $list.add(opUnresolvedId($SUPER)); }
        call               { $list.add($call.result); }
    ;
    
interfaceMethodDecl returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_METHOD, $list);
}
    :   (   interfaceMethodModifier     { $list.add($interfaceMethodModifier.result); }
        )*
        methodHeader[$list]
        (   block                       { $list.add($block.result); }
        |   SEMI
        )
    ;   
    
interfaceMethodModifier returns [ ROp result ]
    :   annotation    { $result = $annotation.result; }
    |   PUBLIC        { $result = opModifier($PUBLIC, JavaModifier.Public()); }
    |   PRIVATE       { $result = opModifier($PRIVATE, JavaModifier.Private()); }
    |   ABSTRACT      { $result = opModifier($ABSTRACT, JavaModifier.Abstract()); }
    |   DEFAULT       { $result = opModifier($DEFAULT, JavaModifier.Default()); }
    |   STATIC        { $result = opModifier($STATIC, JavaModifier.Static()); }
    |   STRICTFP      { $result = opModifier($STRICTFP, JavaModifier.Strictfp()); }
    ;     

lambdaExpr returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_LAMBDA, $list);
}
    :   lambdaParameters[$list]
        ARROW
        (   expression  { $list.add($expression.result); }
        |   block       { $list.add($block.result); }
        )
    ;

lambdaParameters[ List<ROp> list ]
    :   LPAREN lambdaParameterList[$list]? RPAREN
    |   name    { $list.add(opNode(RawOpType.RAW_DECL_PARAMETER, $name.result)); }
    ;

lambdaParameterList[ List<ROp> list ]
    :   lambdaParameter            { $list.add($lambdaParameter.result); }
        (COMMA lambdaParameter     { $list.add($lambdaParameter.result); })*
        (COMMA formalParameterRest { $list.add($formalParameterRest.result); })?
    |   formalParameterRest        { $list.add($formalParameterRest.result); }
    |   name                       { $list.add(opNode(RawOpType.RAW_DECL_PARAMETER, $name.result)); }
        (COMMA name                { $list.add(opNode(RawOpType.RAW_DECL_PARAMETER, $name.result)); } )*
    ;

lambdaParameter returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_PARAMETER, $list);
}
    :   (variableModifier { $list.add($variableModifier.result); })*
        (   unannType     { $list.add($unannType.result);  }
        |   VAR
        )
        name              { $list.add($name.result); }
        (arrayDims        { $list.add($arrayDims.result); })?
    ;

//////////////////////////////////////// Annotations ///////////////////////////////////////////////////////////////////
annotation returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_ANNOTATION, $list);
}
    :   AT qualifiedTypeName                  { $list.add($qualifiedTypeName.result); }
        (   LPAREN
            (   elementValuePairList[$list]
            |   elementValue         { $list.add(opNode(RawOpType.RAW_DECL_PARAMETER, opNode(StdOpType.VALUE, $elementValue.result))); }
            )?
            RPAREN
        )?
    ;
    
elementValuePairList[ List<ROp> list ]
    :   elementValuePair        { $list.add($elementValuePair.result); }
        (COMMA elementValuePair { $list.add($elementValuePair.result); })*
    ;    
    
elementValuePair returns [ ROp result ]
    :   name ASSIGN elementValue
        {
            $result = opNode(RawOpType.RAW_DECL_PARAMETER, Arrays.asList(
                    $name.result,
                    opNode(StdOpType.VALUE, $elementValue.result)
            ));
        }
    ;    

elementValue returns [ ROp result ]
    :   conditionalExpr              { $result = $conditionalExpr.result; }
    |   elementValueArrayInitializer { $result = $elementValueArrayInitializer.result; }
    |   annotation                   { $result = $annotation.result; }
    ;   

elementValueList [List<ROp> list]
    :   elementValue        { $list.add($elementValue.result); }
        (COMMA elementValue { $list.add($elementValue.result); })*
    ;    
    
elementValueArrayInitializer returns [ ROp result ]
    :   FoldBlock         { $result = opFoldBlock((RScanToken)$FoldBlock, "startElementValueArrayInitializer"); }
    |   { List<ROp> list = new ArrayList<>(); }
        LBRACE
            elementValueList[list]?
            (COMMA { list.add(opNone($COMMA)); })?
        RBRACE
        { $result = opNode(evalStatement($LBRACE, $RBRACE), RawOpType.RAW_ARRAY_VALUE, list); }
    ;
    
//////////////////////////////////////// Initializers //////////////////////////////////////////////////////////////////
variableInitializer returns [ ROp result ]
    :   expression         { $result = $expression.result; }
    |   arrayInitializer   { $result = $arrayInitializer.result; }
    ;

variableInitializerList [ List<ROp> list ]
    :   variableInitializer        { $list.add($variableInitializer.result); }
        (COMMA variableInitializer { $list.add($variableInitializer.result); })*
    ; 
    
arrayInitializer returns [ ROp result ]
    :   FoldBlock  { $result = opFoldBlock((RScanToken)$FoldBlock, "startArrayInitializer"); }
    |   { List<ROp> list = new ArrayList<>(); }
        LBRACE
            variableInitializerList[list]?
            (COMMA { list.add(opNone($COMMA)); })?
        RBRACE
        { $result = opNode(evalStatement($LBRACE, $RBRACE), RawOpType.RAW_ARRAY_VALUE, list); }
    ;

//////////////////////////////////////// Statements ////////////////////////////////////////////////////////////////////
block returns [ ROp result ]
    :   FoldBlock           { $result = opFoldBlock((RScanToken)$FoldBlock, "startBlock"); }
    |   LBRACE              { List<ROp> list = Collections.emptyList(); }
            (   blockStmts  { list = $blockStmts.result; }
            )?
        RBRACE              { $result = opNode(evalStatement($LBRACE, $RBRACE), StdOpType.BLOCK, list); }
    ;

blockStmts returns [ List<ROp> result ]
@init { $result = new ArrayList<>(); }
    :   (blockStmt { $result.add($blockStmt.result); })+
    ;

blockStmt returns [ ROp result ]
    :   localVarDeclStmt  { $result = $localVarDeclStmt.result; }
    |   classDecl         { $result = $classDecl.result; }
    |   statement         { $result = $statement.result; }
    ;

localVarDeclStmt returns [ ROp result ]
    :   localVarDecl SEMI { $result = $localVarDecl.result; }
    ;

localVarDecl returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_LOCAL, $list);
}
    :   (variableModifier  { $list.add($variableModifier.result); })*
        (   VAR
        |   unannType      { $list.add($unannType.result); }
        )
        variableDeclList[$list]
    ;

statement returns [ ROp result ]
    :   statementWithoutTrailingSubstatement { $result = $statementWithoutTrailingSubstatement.result; }
    |   labeledStmt                          { $result = $labeledStmt.result; }
    |   ifStmt                               { $result = $ifStmt.result; }
    |   whileStmt                            { $result = $whileStmt.result; }
    |   forStmt                              { $result = $forStmt.result; }
    ;

labeledStmt returns [ ROp result ]
    :   name
        COLON statement
        {
            $result = opNode(RawOpType.RAW_DECL_LABEL, Arrays.asList(
                $name.result,
                $statement.result
            ));
        }
    ;

ifStmt returns [ ROp result ]
locals [ List<ROp> conditional, ROp unconditional]
@init { $conditional = new ArrayList<>(); }
@after { $result = opIfStmt($start, $stop, $conditional, $unconditional); }
    :   ifStmtCondBlock       { $conditional.add($ifStmtCondBlock.result); }
        (ELSE ifStmtCondBlock { $conditional.add($ifStmtCondBlock.result); })*
        (ELSE statement       { $unconditional = blockWrap($statement.result);})?
    ;

ifStmtCondBlock returns [ @NotNull ROp result]
locals [ ROp condition, ROp body ]
@after { $result = opConditionalBlock($start,$stop,ConditionalType.IF_ITEM, $condition, $body); }
    :   IF
        LPAREN
            expression     { $condition = opCondition($expression.result); }
        RPAREN
            statement      { $body = blockWrap($statement.result); }
    ;

whileStmt returns [ @NotNull ROp result]
    :   WHILE
        LPAREN expression RPAREN
            statement
        { $result = opConditionalBlock(
                        $WHILE,
                        $statement.stop,
                        ConditionalType.WHILE,
                        opCondition($expression.result),
                        blockWrap($statement.result) );
        }
    ;

forStmt returns [ @NotNull ROp result]
    :   basicForStmt     { $result = $basicForStmt.result; }
    |   enhancedForStmt  { $result = $enhancedForStmt.result; }
    ;

basicForStmt returns [ @NotNull ROp result]
    :   FOR
            forCondition
            statement
        { $result = opConditionalBlock(
                        $FOR,
                        $statement.stop,
                        ConditionalType.FOR,
                        $forCondition.result,
                        blockWrap($statement.result) );
        }
    ;

forCondition returns [ @NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opCondition($list);
}
    :   LPAREN
            (forInit           { $list.add(opNode(StdOpType.FOR_INIT, $forInit.result)); })?
            SEMI
            (expression        { $list.add(opNode(StdOpType.FOR_CONDITION, $expression.result)); })?
            SEMI
            (statementExprList { $list.add(opNode(StdOpType.FOR_UPDATE, $statementExprList.result)); })?
        RPAREN
    ;

forInit returns [ @NotNull ROp result]
    :   statementExprList   { $result = $statementExprList.result; }
    |   localVarDecl        { $result = $localVarDecl.result; }
    ;

enhancedForStmt returns [ ROp result ]
    :   FOR
            enhancedForCondition
            statement
        { $result = opConditionalBlock(
                        $FOR,
                        $statement.stop,
                        ConditionalType.FOR,
                        $enhancedForCondition.result,
                        blockWrap($statement.result) );
        }
    ;

enhancedForCondition returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opCondition($list);
}
    :   LPAREN
            (variableModifier       { $list.add($variableModifier.result); })*
            (   VAR
            |   unannType           { $list.add($unannType.result); }
            )
            name                    { $list.add($name.result); }
            (arrayDims              { $list.add($arrayDims.result); })?
            COLON
            expression              { $list.add(opNode(StdOpType.EXPR, $expression.result)); }
        RPAREN
    ;

statementWithoutTrailingSubstatement returns [ @NotNull ROp result]
    :   SEMI                { $result = opNone($SEMI); }
    |   block               { $result = $block.result; }
    |   (   statementExpr   { $result = $statementExpr.result; }
        |   assertStmt      { $result = $assertStmt.result; }
        |   doStmt          { $result = $doStmt.result; }
        |   breakStmt       { $result = $breakStmt.result; }
        |   continueStmt    { $result = $continueStmt.result; }
        |   returnStmt      { $result = $returnStmt.result; }
        |   throwStmt       { $result = $throwStmt.result; }
        ) SEMI
    |   switchStmt          { $result = $switchStmt.result; }
    |   synchronizedStmt    { $result = $synchronizedStmt.result; }
    |   tryStmt             { $result = $tryStmt.result; }
    ;

statementExprList returns [ @NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), StdOpType.LIST, $list);
}
    :   statementExpr        { $list.add($statementExpr.result); }
        (COMMA statementExpr { $list.add($statementExpr.result); })*
    ;
    
statementExpr returns [ @NotNull ROp result]
    :   assignment                  { $result = $assignment.result; }
    |   preIncExpr                  { $result = $preIncExpr.result; }
    |   preDecExpr                  { $result = $preDecExpr.result; }
    |   postfixExpr                 { $result = $postfixExpr.result; }
    |   methodInvocation            { $result = $methodInvocation.result; }
    |   classInstanceCreationExpr   { $result = $classInstanceCreationExpr.result; }
    ;

assertStmt returns [ @NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), StdOpType.ASSERT, $list);
}
    :   ASSERT condition=expression { $list.add(opCondition($condition.result)); }
        (COLON msg=expression { $list.add($msg.result); } )?
    ;
    
switchStmt returns [ @NotNull ROp result]
    :   SWITCH
        LPAREN expression RPAREN
        switchBlock
        { $result = opSwitch($SWITCH, $switchBlock.stop, opCondition($expression.result), $switchBlock.result); }
    ;    

switchBlock returns [ @NotNull ROp result]
    :   FoldBlock  { $result = opFoldBlock((RScanToken)$FoldBlock, "startSwitchBlock"); }
    |   { List<ROp> list = new ArrayList<>(); }
        LBRACE
            (switchBlockItem { list.add($switchBlockItem.result); })*
        RBRACE
        { $result = opBlock($LBRACE, $RBRACE, list); }
    ;

switchBlockItem returns [ @NotNull ROp result]
    :   { ROp condition = null; }
        CASE
        (   constantExpr { condition = $constantExpr.result; }
        //|   identifier { condition = $identifier.result; }
        )
        COLON
        (   blockStmts
            { $result = opCaseItem($CASE, $blockStmts.stop, opCondition(condition), $blockStmts.result); }
        |   { $result = opCaseItem($CASE, $COLON, opCondition(condition), null); }
        )
    |   DEFAULT COLON
        (   blockStmts
            { $result = opDefaultCaseItem($DEFAULT, $blockStmts.stop, $blockStmts.result); }
        |   { $result = opDefaultCaseItem($DEFAULT, $COLON, null); }
        )
    ;

doStmt returns [ @NotNull ROp result]
    :   DO statement WHILE
        LPAREN expression RPAREN
        { $result = opConditionalBlock(
                                $DO,
                                $RPAREN,
                                ConditionalType.DO_WHILE,
                                opCondition($expression.result),
                                blockWrap($statement.result) );
        }
    ;

breakStmt returns [ @NotNull ROp result]
    :   BREAK
        (   identifier
            { $result = opNode(evalStatement($BREAK,$identifier.stop), StdOpType.BREAK, opNode(StdOpType.LABEL, $identifier.result)); }
        |   { $result = opBreak($BREAK); }
        )
    ;

continueStmt returns [ @NotNull ROp result]
    :   CONTINUE
        (   identifier
            { $result = opNode(evalStatement($CONTINUE,$identifier.stop), StdOpType.CONTINUE, opNode(StdOpType.LABEL, $identifier.result)); }
        |   { $result = opContinue($CONTINUE); }
        )
    ;

returnStmt returns [ @NotNull ROp result]
    :   RETURN
        (   expression
            { $result = opReturn($RETURN, $expression.stop, $expression.result); }
        |   { $result = opReturn($RETURN); }
        )
    ;

synchronizedStmt returns [ @NotNull ROp result]
    :   SYNCHRONIZED
        LPAREN expression RPAREN
        block
        { $result = opConditionalBlock(
                                $SYNCHRONIZED,
                                $block.stop,
                                ConditionalType.SYNCHRONIZED,
                                opCondition($expression.result),
                                $block.result );
        }
    ;

throwStmt returns [ @NotNull ROp result]
    :   THROW expression
        { $result = opThrow($THROW, $expression.stop, $expression.result); }
    ;

tryStmt returns [ @NotNull ROp result]
locals [ List<ROp> list, ROpType tryOpType ]
@init { $list = new ArrayList<>(); }
@after {
    $result = opNode(evalStatement($start, $stop), $tryOpType, $list);
}
    :   { $tryOpType = StdOpType.TRY; }
        TRY block                   { $list.add($block.result); }
        (   (catchClause            { $list.add($catchClause.result); })+
        |   (catchClause            { $list.add($catchClause.result); })*
            FINALLY block           { $list.add(opNode(StdOpType.FINALLY, $block.result)); }
        )
    |   { $tryOpType = StdOpType.TRY_RESOURCE; }
        TRY
        LPAREN
            resourceList            { $list.add(opCondition($resourceList.result)); }
        RPAREN
        block                       { $list.add($block.result); }
        (catchClause                { $list.add($catchClause.result); })*
        (   FINALLY block           { $list.add(opNode(StdOpType.FINALLY, $block.result)); }
        )?
    ;

catchClause returns [ @NotNull ROp result]
    :   CATCH LPAREN condition=catchFormalParameter RPAREN
        block
        { $result = opConditionalBlock($CATCH, $block.stop, ConditionalType.CATCH, opCondition($condition.result), $block.result); }
    ;

catchFormalParameter returns [ @NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), JavaOpType.RAW_DECL_CATCH_PARAMETER, $list); }
    :   (variableModifier { $list.add($variableModifier.result); })*
        catchType         { $list.addAll($catchType.result); }
        name              { $list.add($name.result); }
        (arrayDims        { $list.add($arrayDims.result); } )?
    ;

catchType returns [ @NotNull List<ROp> result]
@init { $result = new ArrayList<>(); }
    :   unannClassType   { $result.add($unannClassType.result); }
        (BITOR classType { $result.add($classType.result); })*
    ;

resourceList returns [ @NotNull List<ROp> result]
@init { $result = new ArrayList<>(); }
    :   resource        { $result.add($resource.result); }
        (SEMI resource  { $result.add($resource.result); } )*
        SEMI?
    ;

resource returns [ @NotNull ROp result]
locals [List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start, $stop), JavaOpType.RESOURCE, $list); }
    :   (variableModifier       { $list.add($variableModifier.result); })*
        (   VAR
        |   unannType           { $list.add($unannType.result); }
        )
        name                    { $list.add($name.result); }
        ASSIGN expression       { $list.add(opNode(StdOpType.INIT, $expression.result)); }
    |   variableAccess  { $list.add($variableAccess.result); }
    ;

variableAccess returns [ @NotNull ROp result]
    :   qualifiedIdentifier { $result = opChain($qualifiedIdentifier.result); }
    |   fieldAccess         { $result = $fieldAccess.result; }
    ;

//////////////////////////////////////// Expressions ///////////////////////////////////////////////////////////////////
atom returns [ @NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = $list.size() == 1 ? $list.get(0) : opChain($list); }
    :   atomFirst[$list]
        (   DOT unqualifiedClassInstanceCreationExpr
            { $list.add($unqualifiedClassInstanceCreationExpr.result); }

        |   DOT typeArguments identifier call
            {
                $list.add(opNode(
                    JavaOpType.RAW_METHOD_ACCESS,
                    Arrays.asList($typeArguments.result, $identifier.result)));
                $list.add($call.result);
            }

        |   call
            { $list.add($call.result); }

        |   DOT identifier
            { $list.add($identifier.result); }

        |   LBRACK expression RBRACK
            { $list.add(opArrayAccessor($LBRACK, $RBRACK, $expression.result)); }

        |   COLONCOLON
            { List<ROp> mrList = new ArrayList<>(); Token stop = null; }
            (typeArguments  { mrList.add($typeArguments.result); })?
            (   identifier  { mrList.add($identifier.result); stop = $identifier.stop; }
            |   NEW         { mrList.add(opUnresolvedId($NEW)); stop = $NEW; }
            )               { $list.add(opNode(evalStatement($COLONCOLON, stop), JavaOpType.RAW_METHOD_REFERENCE, mrList)); }
        )*
    ;

atomFirst [ @NotNull List<ROp> list]
    :   literal                                 { $list.add($literal.result); }
    |   arrayCreationExpr                       { $list.add($arrayCreationExpr.result); }
    |   classLiteral                            { $list.add($classLiteral.result); }

    |   (   qualifiedTypeName DOT                        { $list.add($qualifiedTypeName.result); }
        )?
        (   THIS                                { $list.add(opUnresolvedId($THIS)); }
        |   SUPER                               { $list.add(opUnresolvedId($SUPER)); }
        )

    |   LPAREN expression RPAREN                { $list.add(opNode(StdOpType.PARENTHESIZED, $expression.result)); }
    |   unqualifiedClassInstanceCreationExpr    { $list.add($unqualifiedClassInstanceCreationExpr.result); }
//    |   methodName
    |   identifier                              { $list.add($identifier.result); }
    |   referenceType                           { $list.add($referenceType.result); }
    ;

methodInvocation returns [ ROp result ]
    :   atom call
        { $result = opChainOrAppend($atom.result, $call.result); }
    ;

call returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = Collections.emptyList(); }
@after { $result = opNode(evalStatement($start,$stop), RawOpType.RAW_CALL, $list); }
    :   LPAREN
            (argumentList { $list = $argumentList.result; })?
        RPAREN
    ;

argumentList returns [ List<ROp> result ]
@init { $result = new ArrayList<>(); }
    :   expression        { $result.add($expression.result); }
        (COMMA expression { $result.add($expression.result); })*
    ;
    
classInstanceCreationExpr returns [ ROp result ]
    :   (   atom DOT unqualifiedClassInstanceCreationExpr   { $result = opNode(JavaOpType.RAW_NEW_QUALIFIED, List.of($atom.result, $unqualifiedClassInstanceCreationExpr.result)); }
        |   unqualifiedClassInstanceCreationExpr            { $result = $unqualifiedClassInstanceCreationExpr.result; }
        )
    ;

unqualifiedClassInstanceCreationExpr returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), RawOpType.RAW_NEW, $list); }
    :   NEW (typeArguments                 { $list.add($typeArguments.result); })?
        classOrInterfaceTypeToInstantiate  { $list.add($classOrInterfaceTypeToInstantiate.result); }
        call                               { $list.add($call.result); }
        (classBody                         { $list.add($classBody.result); })?
    ;

classOrInterfaceTypeToInstantiate returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), JavaOpType.RAW_TYPE_INSTANTIATION, $list); }
    :   (annotation      { $list.add($annotation.result); })* identifier { $list.add($identifier.result); }
        (DOT (annotation { $list.add($annotation.result); })* identifier { $list.add($identifier.result); })*
        (typeArgumentsOrDiamond { $list.add($typeArgumentsOrDiamond.result); })?
    ;

typeArgumentsOrDiamond returns [ ROp result ]
    :   typeArguments   { $result = $typeArguments.result; }
    |   LT GT           { $result = opNode(evalStatement($LT, $GT), StdOpType.TYPE_ARGUMENTS, Collections.emptyList() ); }
    ;

fieldAccess returns [ ROp result ]
    :   atom DOT identifier
        { $result = opChainOrAppend($atom.result, $identifier.result); }
    ;

arrayAccess returns [ ROp result ]
    :   atom
        LBRACK expression RBRACK
        { $result = opChainOrAppend($atom.result, opArrayAccessor($LBRACK, $RBRACK, $expression.result)); }
    ;

arrayCreationExpr returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), JavaOpType.RAW_ARRAY_INSTANTIATION, $list); }
    :   NEW
        (   primitiveType   { $list.add($primitiveType.result); }
        |   classType       { $list.add($classType.result); }
        )
        (   (arrayDimExpr   { $list.add($arrayDimExpr.result); })+
            (arrayDims      { $list.add($arrayDims.result); })?
        |   arrayDims       { $list.add($arrayDims.result); }
            arrayInitializer{ $list.add($arrayInitializer.result); }
        )
    ;

arrayDimExpr returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), JavaOpType.RAW_ARRAY_DIM_EXPR, $list); }
    :   (annotation { $list.add($annotation.result); } )*
        LBRACK expression RBRACK
        { $list.add($expression.result); }
    ;

expression returns [ ROp result ]
    :   lambdaExpr        { $result = $lambdaExpr.result; }
    |   conditionalExpr   { $result = $conditionalExpr.result; }
    |   assignment        { $result = $assignment.result; }
    ;

assignment returns [ ROp result ]
    :   leftHandSide operator=assignmentOperator expression
        { $result = opAssign($leftHandSide.result, $operator.result, $expression.result); }
    ;

leftHandSide returns [ ROp result ]
    :   qualifiedIdentifier  { $result = opChain($qualifiedIdentifier.result); }
    |   fieldAccess          { $result = $fieldAccess.result; }
    |   arrayAccess          { $result = $arrayAccess.result; }
    ;

assignmentOperator returns [@NotNull  AssignType result ]
    :   ASSIGN             { $result = AssignType.DEFAULT; }
    |   MUL_ASSIGN         { $result = AssignType.MUL; }
    |   DIV_ASSIGN         { $result = AssignType.DIV; }
    |   MOD_ASSIGN         { $result = AssignType.MOD; }
    |   ADD_ASSIGN         { $result = AssignType.ADD; }
    |   SUB_ASSIGN         { $result = AssignType.SUB; }
    |   LSHIFT_ASSIGN      { $result = AssignType.SHL; }
    |   RSHIFT_ASSIGN      { $result = AssignType.SHR; }
    |   URSHIFT_ASSIGN     { $result = AssignType.SHR_LOGICAL; }
    |   AND_ASSIGN         { $result = AssignType.BW_AND; }
    |   XOR_ASSIGN         { $result = AssignType.BW_XOR; }
    |   OR_ASSIGN          { $result = AssignType.BW_OR; }
    ;

conditionalExpr returns [ ROp result ]
    :   orExpr { $result = $orExpr.result; }
        (   QUESTION trueExpr=expression
            COLON
            { ROp falseExpr = null; }
            (   conditionalExpr { falseExpr = $conditionalExpr.result; }
            |   lambdaExpr      { falseExpr = $lambdaExpr.result; }
            )
            { $result = opTernary($result, $trueExpr.result, falseExpr); }
        )?
    ;

orExpr returns [ ROp result ]
locals [List<ROp> list]
@after { if ( $list != null) $result = opBinary(BinaryType.OR, $list); }
    :   andExpr { $result = $andExpr.result; }
        (
            {
                $list = new ArrayList<>();
                $list.add($result);
            }
            (OR andExpr { $list.add($andExpr.result); })+
        )?
    ;

andExpr returns [ ROp result ]
locals [List<ROp> list]
@after { if ( $list != null) $result = opBinary(BinaryType.AND, $list); }
    :   bitOrExpr  { $result = $bitOrExpr.result; }
        (
            {
                $list = new ArrayList<>();
                $list.add($result);
            }
            (AND bitOrExpr { $list.add($bitOrExpr.result); })+
        )?
    ;

bitOrExpr returns [ ROp result ]
locals [List<ROp> list]
@after { if ( $list != null) $result = opBinary(BinaryType.BW_OR, $list); }
    :   xorExpr { $result = $xorExpr.result; }
        (
            {
                $list = new ArrayList<>();
                $list.add($result);
            }
            (BITOR xorExpr { $list.add($xorExpr.result); } )+
        )?
    ;

xorExpr returns [ ROp result ]
locals [List<ROp> list]
@after { if ( $list != null) $result = opBinary(BinaryType.BW_XOR, $list); }
    :   bitAndExpr { $result = $bitAndExpr.result; }
        (
            {
                $list = new ArrayList<>();
                $list.add($result);
            }
            (CARET bitAndExpr { $list.add($bitAndExpr.result); } )+
        )?
    ;

bitAndExpr returns [ ROp result ]
locals [List<ROp> list]
@after { if ( $list != null) $result = opBinary(BinaryType.BW_AND, $list); }
    :   eqExpr  { $result = $eqExpr.result; }
        (
            {
                $list = new ArrayList<>();
                $list.add($result);
            }
            (BITAND eqExpr { $list.add($eqExpr.result); })+
        )?
    ;

eqExpr returns [ ROp result ]
    :   relExpr { $result = $relExpr.result; }
        (
            eqOperator relExpr
            { $result = opBinary($eqOperator.result, $result, $relExpr.result); }
        )*
    ;

eqOperator returns [BinaryType result]
    :   EQUAL      { $result = BinaryType.EQ; }
    |   NOTEQUAL   { $result = BinaryType.NEQ; }
    ;

relExpr returns [ ROp result ]
    :   shiftExpr { $result = $shiftExpr.result; }
        (
            relOperator shiftExpr
            { $result = opBinary($relOperator.result, $result, $shiftExpr.result); }
        )*
    ;

relOperator returns [BinaryType result]
    :   LT           { $result = BinaryType.LT; }
    |   GT           { $result = BinaryType.GT; }
    |   LE           { $result = BinaryType.LE; }
    |   GE           { $result = BinaryType.GE; }
    |   INSTANCEOF   { $result = BinaryType.INSTANCEOF; }
    ;

shiftExpr returns [ ROp result ]
    :   addExpr { $result = $addExpr.result; }
        (
            shiftOperator addExpr
            { $result = opBinary($shiftOperator.result, $result, $addExpr.result); }
        )*
    ;

shiftOperator returns [BinaryType result]
    :   {checkNoWs(2)}?
        LT LT            { $result = BinaryType.SHL; }
    |   {checkNoWs(2)}?
        GT GT            { $result = BinaryType.SHR; }
    |   {checkNoWs(3)}?
        GT GT GT         { $result = BinaryType.SHR_LOGICAL; }
    ;

addExpr returns [ ROp result ]
    :   multExpr { $result = $multExpr.result; }
        (
            addOperator multExpr
            { $result = opBinary($addOperator.result, $result, $multExpr.result); }
        )*
    ;

addOperator returns [BinaryType result]
    :   ADD  { $result = BinaryType.ADD; }
    |   SUB  { $result = BinaryType.SUB; }
    ;

multExpr returns [ ROp result ]
    :   unaryExpr { $result = $unaryExpr.result; }
        (
            multOperator unaryExpr
            { $result = opBinary($multOperator.result, $result, $unaryExpr.result); }
        )*
    ;

multOperator returns [BinaryType result]
    :   MUL  { $result = BinaryType.MUL; }
    |   DIV  { $result = BinaryType.DIV; }
    |   MOD  { $result = BinaryType.MOD; }
    ;

unaryExpr returns [ ROp result ]
    :   unaryExprNotPlusMinus   { $result = $unaryExprNotPlusMinus.result; } // to allow integers consume its unary sign
    |   preIncExpr              { $result = $preIncExpr.result; }
    |   preDecExpr              { $result = $preDecExpr.result; }
    |   opToken= ( ADD | SUB )  { UnaryType unaryType = "+".equals($opToken.text) ? UnaryType.ADD : UnaryType.SUB; }
        unaryExpr               { $result = opUnary($opToken, $unaryExpr.stop, unaryType, $unaryExpr.result); }
    ;

preIncExpr returns [ ROp result ]
    :   INC unaryExpr { $result = opUnary($INC, $unaryExpr.stop, UnaryType.INC, $unaryExpr.result); }
    ;

preDecExpr returns [ ROp result ]
    :   DEC unaryExpr { $result = opUnary($DEC, $unaryExpr.stop, UnaryType.DEC, $unaryExpr.result); }
    ;

unaryExprNotPlusMinus returns [ ROp result ]
    :   postfixExpr              { $result = $postfixExpr.result; }
    |   opToken = (TILDE | BANG) { UnaryType unaryType = "~".equals($opToken.text) ? UnaryType.BW_NOT : UnaryType.NOT; }
        unaryExpr                { $result = opUnary($opToken, $unaryExpr.stop, unaryType, $unaryExpr.result); }
    |   castExpr                 { $result = $castExpr.result; }
    ;

postfixExpr returns [ ROp result ]
    :   atom { $result = $atom.result; }
        (
            postfixOperator
            { $result = opUnary($atom.start, $postfixOperator.stop, $postfixOperator.result, $atom.result); }
        )?
    ;

postfixOperator returns [ UnaryType result ]
    :   INC { $result = UnaryType.INC_POSTFIX; }
    |   DEC { $result = UnaryType.DEC_POSTFIX; }
    ;

castExpr returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), JavaOpType.RAW_TYPE_CAST, $list); }
    :   LPAREN
        (   referenceType            { $list.add($referenceType.result); }
            (BITAND classType        { $list.add(opNode(RawOpType.RAW_TYPE, $classType.result)); })*
        |   primitiveType            { $list.add($primitiveType.result); }
        )
        RPAREN
        (   unaryExpr                { $list.add($unaryExpr.result); }
        |   lambdaExpr               { $list.add($lambdaExpr.result); }
        )
    ;

constantExpr returns [ ROp result ]
    :   expression  { $result = $expression.result; }
    ;

//////////////////////////////////////// Types /////////////////////////////////////////////////////////////////////////
primitiveType returns [ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(StdOpType.TYPE, $list); }
    :   (annotation { $list.add($annotation.result); })*
        unannPrimitiveType { $list.add($unannPrimitiveType.result); }
    ;

referenceType returns [ ROp result ]
    :   primitiveType arrayDims
        { $result = opNode(RawOpType.RAW_ARRAY_TYPE, Arrays.asList(rawTypeWrap($primitiveType.result), $arrayDims.result)); }
    |   cType=classType
        (   arrayDims
            { $result = opNode(RawOpType.RAW_ARRAY_TYPE, Arrays.asList(rawTypeWrap($cType.result), $arrayDims.result)); }
        |   { $result = rawTypeWrap($cType.result); }
        )
    ;

classType returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), RawOpType.RAW_TYPE_REFERENCE, $list); }
    :   (annotation      { $list.add($annotation.result); })*
        identifier   { $list.add($identifier.result); }
        (typeArguments   { $list.add($typeArguments.result); })?
        (   DOT
            (annotation    { $list.add($annotation.result); } )*
            identifier { $list.add($identifier.result); }
            (typeArguments { $list.add($typeArguments.result); })?
        )*
    ;

classTypeList returns [ List<ROp> result ]
@init { $result = new ArrayList<>(); }
    :   classType        { $result.add(opNode(RawOpType.RAW_TYPE, $classType.result)); }
        (COMMA classType { $result.add(opNode(RawOpType.RAW_TYPE, $classType.result)); })*
    ;

arrayDims returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(StdOpType.ARRAY_DIMS, $list); }
    :   (
            { List<ROp> items = new ArrayList<>(); }
            (annotation { items.add($annotation.result); } )*
            LBRACK RBRACK
            { $list.add(opNode(evalStatement($LBRACK, $RBRACK), StdOpType.ARRAY_DIM, items)); }
        )+
    ;

typeParameters returns [ List<ROp> result ]
@init { $result = new ArrayList<>(); }
    :   LT
            typeParameter        { $result.add($typeParameter.result); }
            (COMMA typeParameter { $result.add($typeParameter.result); })*
        GT
    ;

typeParameter returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_TYPE_PARAMETER, $list); }
    :   (annotation    { $list.add($annotation.result); })*
        name           { $list.add($name.result); }
        (   EXTENDS
            { List<ROp> items = new ArrayList<>(); }
            classType        { items.add($classType.result); }
            (BITAND classType { items.add($classType.result); })*
            { $list.add(opNode(StdOpType.EXTENDS, items)); }
        )?
    ;

typeArguments returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start, $stop), StdOpType.TYPE_ARGUMENTS, $list ); }
    :   LT
            typeArgument        { $list.add($typeArgument.result); }
            (COMMA typeArgument { $list.add($typeArgument.result); })*
        GT
    ;

typeArgument returns [ ROp result ]
@after { $result = opNode(RawOpType.RAW_TYPE, $result); }
    :   referenceType { $result = $referenceType.result; }
    |   wildcard      { $result = $wildcard.result; }
    ;

wildcard returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start, $stop), JavaOpType.TYPE_WILDCARD, $list ); }
    :   (annotation { $list.add($annotation.result); })* QUESTION
        (   EXTENDS referenceType  { $list.add(opNode(StdOpType.EXTENDS, $referenceType.result)); }
        |   SUPER referenceType    { $list.add(opNode(StdOpType.SUPER, $referenceType.result)); }
        )?
    ;

unannType returns [ ROp result ]
@after { $result = rawTypeWrap($result); }
    :   unannPrimitiveType  { $result = $unannPrimitiveType.result; }
    |   unannReferenceType  { $result = $unannReferenceType.result; }
    ;

unannPrimitiveType returns [ ROp result ]
    :   BYTE      { $result = opUnresolvedId($BYTE); }
    |   SHORT     { $result = opUnresolvedId($SHORT); }
    |   INT       { $result = opUnresolvedId($INT); }
    |   LONG      { $result = opUnresolvedId($LONG); }
    |   CHAR      { $result = opUnresolvedId($CHAR); }
    |   FLOAT     { $result = opUnresolvedId($FLOAT); }
    |   DOUBLE    { $result = opUnresolvedId($DOUBLE); }
    |   BOOLEAN   { $result = opUnresolvedId($BOOLEAN); }
    ;

unannReferenceType returns [ ROp result ]
    :   unannPrimitiveType arrayDims
        { $result = opNode(RawOpType.RAW_TYPE, Arrays.asList(rawTypeWrap($unannPrimitiveType.result), $arrayDims.result)); }
    |   cType=unannClassType
        (   arrayDims
            { $result = opNode(RawOpType.RAW_TYPE, Arrays.asList(rawTypeWrap($cType.result), $arrayDims.result)); }
        |   { $result = $cType.result; }
        )
    ;

unannClassType returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), RawOpType.RAW_TYPE_REFERENCE, $list); }
    :   identifier   { $list.add($identifier.result); }
        (typeArguments   { $list.add($typeArguments.result); })?
        (   DOT
            (annotation    { $list.add($annotation.result); } )*
            identifier { $list.add($identifier.result); }
            (typeArguments { $list.add($typeArguments.result); })?
        )*
    ;

//////////////////////////////////////// Types /////////////////////////////////////////////////////////////////////////
literal returns [ ROp result ]
    :   SUB? integerLiteral[$SUB != null]        { $result = $integerLiteral.result; }
    |   SUB? floatingPointLiteral[$SUB != null]  { $result = $floatingPointLiteral.result; }
    |   BooleanLiteral        { $result = booleanLiteral($BooleanLiteral); }
    |   CharacterLiteral      { $result = characterLiteral($CharacterLiteral); }
    |   StringLiteral         { $result = stringLiteral($StringLiteral); }
    |   NullLiteral           { $result = nullLiteral($NullLiteral); }
    ;

integerLiteral[boolean negate] returns [ ROp result ]
    :   DecimalIntegerLiteral   { $result = intLiteral($negate, $DecimalIntegerLiteral, 10); }
    |   HexIntegerLiteral       { $result = intLiteral($negate, $HexIntegerLiteral, 16); }
    |   OctalIntegerLiteral     { $result = intLiteral($negate, $OctalIntegerLiteral, 8); }
    |   BinaryIntegerLiteral    { $result = intLiteral($negate, $BinaryIntegerLiteral, 2); }
    ;

floatingPointLiteral[boolean negate] returns [ ROp result ]
    :   DecimalFloatingPointLiteral      { $result = floatLiteral($negate, $DecimalFloatingPointLiteral); }
    |   HexadecimalFloatingPointLiteral  { $result = floatHexLiteral($negate, $HexadecimalFloatingPointLiteral); }
    ;

classLiteral returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start, $stop), JavaOpType.RAW_CLASS_REFERENCE, $list); }
    :   (   qualifiedTypeName             { $list.add($qualifiedTypeName.result); }
            (literalArrayDims    { $list.add($literalArrayDims.result); })?
        |   unannPrimitiveType   { $list.add($unannPrimitiveType.result); }
            (literalArrayDims    { $list.add($literalArrayDims.result); })?
        |   VOID                 { $list.add(opUnresolvedId($VOID)); }
        )
        DOT CLASS { $list.add(opUnresolvedId($CLASS)); }
    ;

literalArrayDims returns [ ROp result ]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(StdOpType.ARRAY_DIMS, $list); }
    :   (
            LBRACK RBRACK
            { $list.add(opNode(evalStatement($LBRACK, $RBRACK), StdOpType.ARRAY_DIM, Collections.emptyList())); }
        )+
    ;

//////////////////////////////////////// Misc /////////////////////////////////////////////////////////////////////////
nameList returns [List<ROp> result]
@init { $result = new ArrayList<>(); }
    :   qualifiedName         { $result.add($qualifiedName.result); }
        (COMMA qualifiedName  { $result.add($qualifiedName.result); })*
    ;

qualifiedName returns [ ROp result ]
    :   qualifiedIdentifier { $result = opName($qualifiedIdentifier.result.size() == 1 ?$qualifiedIdentifier.result.get(0) : opChain($qualifiedIdentifier.result) ); }
    ;

name returns [ ROp result ]
    :   identifier  { $result = opName($identifier.result); }
    ;

qualifiedTypeName returns [ ROp result ]
    :   qualifiedIdentifier { $result = opNode(RawOpType.RAW_TYPE, opNode(RawOpType.RAW_TYPE_REFERENCE, $qualifiedIdentifier.result)); }
    ;

qualifiedTypeNameList returns [ List<ROp> result ]
@init { $result = new ArrayList<>(); }
    :   qualifiedTypeName         { $result.add($qualifiedTypeName.result); }
        (COMMA qualifiedTypeName  { $result.add($qualifiedTypeName.result); })*
    ;

qualifiedIdentifier returns [List<ROp> result]
@init { $result = new ArrayList<>(); }
    :   identifier { $result.add($identifier.result); }
        (DOT identifier { $result.add($identifier.result); } )*
    ;

identifier returns [ROp result]
@after { $result = opUnresolvedId($start); }
    :   Identifier
    |   TO
    |   MODULE
    |   OPEN
    |   WITH
    |   PROVIDES
    |   USES
    |   OPENS
    |   REQUIRES
    |   EXPORTS
    |   VAR
    |   TRANSITIVE
;
