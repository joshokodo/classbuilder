package com.vertafore.test.utilities.classgenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ClassBuilder {

    private final String PACKAGE_TEMPLATE = "package %s;\n\n";
    private final String IMPORT_STATEMENT_TEMPLATE = "import %s%s;\n";
    private final String CLASS_TEMPLATE = "public class %s {\n\n%s\n}";
    private final String FIELD_TEMPLATE = "\t%s %s%s%s %s = %s;\n";
    private final String METHOD_TEMPLATE = "\t%s %s%s %s(%s){\n\t\t%s%s\n\t}\n\n";

    private List<String> imports = new ArrayList<>();
    private List<String> fields = new ArrayList<>();
    private List<String> methods = new ArrayList<>();

    private String className;
    private String packagePath;

    public ClassBuilder(String packagePath, String className) {
        setPackagePath(packagePath);
        setClassName(className);
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
    }

    public void removeMethod(String method) {
        methods.remove(method);
    }

    public void removeField(String field) {
        fields.remove(field);
    }

    public void removeImport(String importStatement) {
        imports.remove(importStatement);
    }

    public void clearImports() {
        imports.clear();
    }

    public void clearMethods() {
        methods.clear();
    }

    public void clearFields() {
        fields.clear();
    }

    public void clearAll() {
        clearFields();
        clearImports();
        clearMethods();
        className = null;
        packagePath = null;
    }

    public void generateClassAt(String path) {
        String fullPath = path + "/" + className + ".java";
        generateBaseDirectory(path);
        generateBaseFile(fullPath);
        writeUsingBufferedWriter(fullPath, generateFileContent());
    }

    public String generateFileContent() {
        StringBuilder result = new StringBuilder();
        result.append(String.format(PACKAGE_TEMPLATE, packagePath));
        result.append(generateStringFromList(imports));
        result.append(
                String.format(
                        CLASS_TEMPLATE,
                        className,
                        generateStringFromList(fields) + generateStringFromList(methods)));
        return result.toString();
    }

    public void addMethod(
            String accessLevel,
            boolean isStatic,
            String returnType,
            String methodName,
            String methodArguments,
            String beforeReturnStatement,
            String methodDefinition) {
        methods.add(
                String.format(
                        METHOD_TEMPLATE,
                        accessLevel,
                        isStatic ? "static " : "",
                        returnType,
                        methodName,
                        methodArguments,
                        beforeReturnStatement,
                        methodDefinition));
    }

    public void addField(
            String accessLevel,
            boolean isStatic,
            boolean isFinal,
            String dataType,
            String fieldName,
            String dataValue) {
        fields.add(
                String.format(
                        FIELD_TEMPLATE,
                        accessLevel,
                        isStatic ? "static " : "",
                        isFinal ? "final " : "",
                        dataType,
                        fieldName,
                        dataValue));
    }

    public void addImport(boolean isStatic, String path) {
        String currentImports = generateStringFromList(imports);
        if (!currentImports.contains(path)) {
            imports.add(String.format(IMPORT_STATEMENT_TEMPLATE, isStatic ? "static " : "", path));
        }
    }

    public void addPublicStaticMethod(
            String returnType,
            String methodName,
            String methodArguments,
            String beforeReturnStatement,
            String methodDefinition) {
        addMethod(
                "public",
                true,
                returnType,
                methodName,
                methodArguments,
                beforeReturnStatement,
                methodDefinition);
    }

    public void addPrivateStaticFinalStringField(String constantName, String constantValue) {
        addField("private", true, true, "String", constantName, constantValue);
    }

    public void addImportStatement(String importPath) {
        addImport(false, importPath);
    }

    public void addStaticImportStatement(String importPath) {
        addImport(true, importPath);
    }

    public void addArrayOfImportStatements(String[] imports) {
        Arrays.stream(imports).forEach(this::addImportStatement);
    }

    public void addCollectionOfStaticImportStatements(Collection<String> collection) {
        collection.forEach(this::addStaticImportStatement);
    }

    public void addArrayOfStaticImportStatements(String[] imports) {
        Arrays.stream(imports).forEach(this::addStaticImportStatement);
    }

    public void addCollectionOfImportStatements(Collection<String> collection) {
        collection.forEach(this::addImportStatement);
    }

    public void printFileContent() {
        System.out.println(generateFileContent());
    }

    public void generateBaseDirectory(String basePath) {
        File file = new File(basePath); // initialize File object and passing new path as argument
        try {
            file.mkdir();
        } catch (Exception e) {
            e.printStackTrace(); // prints exception if any
        }
    }

    public void generateBaseFile(String basePath) {
        File file = new File(basePath); // initialize File object and passing path as argument
        try {
            file.createNewFile(); // makes a new file
        } catch (Exception e) {
            e.printStackTrace(); // prints exception if any
        }
    }

    public void writeUsingBufferedWriter(String path, String fileContent) {

        File file = new File(path);
        FileWriter fr = null;
        BufferedWriter br = null;
        String dataWithNewLine = fileContent + System.getProperty("line.separator");
        try {
            fr = new FileWriter(file);
            br = new BufferedWriter(fr);
            br.write(dataWithNewLine);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String generateStringFromList(List<String> list) {
        StringBuilder result = new StringBuilder();
        list.forEach(result::append);
        return result.append("\n").toString();
    }

    public static String capitalizeStringArrToString(String[] arr) {
        StringBuilder result = new StringBuilder();
        Arrays.stream(arr)
                .forEach(
                        word -> {
                            result.append(capitalizeString(word));
                        });
        return result.toString();
    }

    public static String capitalizeString(String str) {
        if (str.isEmpty()) {
            return str;
        } else if (str.length() == 1) {
            return Character.toUpperCase(str.charAt(0)) + "";
        } else if (str.length() >= 2) {
            return Character.toUpperCase(str.charAt(0)) + str.substring(1);
        } else {
            throw new IllegalArgumentException("could not capitalize string " + str);
        }
    }
}
