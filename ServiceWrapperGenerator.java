package com.company.test.utilities.classgenerator;

import static com.company.test.utilities.classgenerator.ClassBuilder.capitalizeStringArrToString;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.CaseFormat;
import com.company.core.util.JsonHelper;
import groovyjarjarcommonscli.MissingArgumentException;
import io.restassured.response.Response;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.serenitybdd.rest.SerenityRest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServiceWrapperGenerator {

    private static final String SWAGGER_API_URL =
            "https://company.com/%s/v2/api-docs?group=%s-service";

    private final String BASE_PACKAGE_PATH = "com.company.test.tasks.servicewrappers.%s";

    private final String[] DEFAULT_IMPORTS = {
            "net.serenitybdd.screenplay.Performable", "net.serenitybdd.screenplay.Task"
    };

    private final String[] DEFAULT_STATIC_IMPORTS = {
            "com.company.test.abilities.CallProductApi.as", "net.serenitybdd.rest.SerenityRest.rest"
    };

    private final String CLASS_NAME_TEMPLATE = "Use%sServiceTo";
    private final String METHOD_DEFINITION_TEMPLATE =
            "return Task.where(\n\t\t\"{0} %s\", \n\t\t\tactor -> {\n\t\t\t\t%s\n\t\t\t}\n\t\t);";

    private final String GET_FILE_MIME_TYPE =
            "String mime = URLConnection.guessContentTypeFromName(file.getName());";

    private final String BEFORE_RETURN_STATEMENT = "%s";

    private final String REST_CALL_METHOD_CHAIN_TEMPLATE =
            "rest().with().%s%s%s(as(actor).toEndpoint(%s));";
    private final String CONTENT_TYPE_TEMPLATE = "contentType(\"%s\").";
    private final String FORM_DATA_TEMPLATE_ = "multiPart(\"%s\", %s %s).";
    private final String PATH_PARAM_TEMPLATE = "pathParam(\"%s\", %s).";
    private final String QUERY_PARAM_TEMPLATE = "queryParam(\"%s\", %s).";
    private final String BODY_TEMPLATE = "body(body).";

    private final String DEFAULT_BASE_PATH =
            "./src/main/java/com/company/test/tasks/servicewrappers/%s";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parameter {
        public String name;
        public String in;
        public String required;
        public String type;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiCallMethod {
        public String restVerb;
        public String summary;
        public String consumes;
        public String endpointName;
        public List<Parameter> parameters;
        public String methodName;
        public String methodArguments;
        public String restParamMethodChain;
        public String operationId;
        public String controller;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Path {
        public String endpoint;
        public List<ApiCallMethod> apiCallMethods;
    }

    public static void main(String[] args) throws ParseException, MissingArgumentException {
        if (args.length == 0) {
            throw new MissingArgumentException("must provide service arguments when running this tasks");
        }
        for (String service : args) {
            Response response = SerenityRest.get(String.format(SWAGGER_API_URL, service, service));
            new ServiceWrapperGenerator().generateServiceWrapperClass(response.getBody().asString());
        }
    }

    public void generateServiceWrapperClass(String json) throws ParseException {
        JSONObject swaggerJson = (JSONObject) new JSONParser().parse(json);

        List<Path> paths = convertPathsMapToPathsList((Map) swaggerJson.get("paths"));
        String servicePath = (String) swaggerJson.get("basePath");

        String className = String.format(CLASS_NAME_TEMPLATE, generateClassName(servicePath));
        String packageName = generatePackageName(servicePath);
        String packagePath = String.format(BASE_PACKAGE_PATH, packageName);

        ClassBuilder classBuilder = new ClassBuilder(packagePath, className);

        classBuilder.addArrayOfImportStatements(DEFAULT_IMPORTS);
        classBuilder.addArrayOfStaticImportStatements(DEFAULT_STATIC_IMPORTS);

        paths.forEach(
                p -> {
                    p.apiCallMethods.forEach(
                            ac -> {
                                classBuilder.addPrivateStaticFinalStringField(ac.endpointName, p.endpoint);

                                String restCallMethodChain =
                                        String.format(
                                                REST_CALL_METHOD_CHAIN_TEMPLATE,
                                                String.format(CONTENT_TYPE_TEMPLATE, ac.consumes),
                                                ac.restParamMethodChain,
                                                ac.restVerb,
                                                ac.endpointName);

                                String beforeReturnStatement = generateBeforeReturnStatementCode(ac);

                                classBuilder.addPublicStaticMethod(
                                        "Performable",
                                        ac.methodName,
                                        ac.methodArguments,
                                        beforeReturnStatement,
                                        String.format(METHOD_DEFINITION_TEMPLATE, ac.summary, restCallMethodChain));
                                classBuilder.addCollectionOfImportStatements(generateImportPaths(ac.parameters));
                            });
                });
        String path =
                Paths.get(String.format(DEFAULT_BASE_PATH, packageName))
                        .toAbsolutePath()
                        .normalize()
                        .toString();
        classBuilder.generateClassAt(path);
    }

    private List<Path> convertPathsMapToPathsList(Map paths) {
        List<Path> results = new ArrayList<>();
        paths
                .keySet()
                .forEach(
                        p -> {
                            Path nextPath = new Path();
                            nextPath.endpoint = generateEndpointConstantValue((String) p);
                            nextPath.apiCallMethods = convertApiCallsMapToApiCallsList((Map) paths.get(p));
                            results.add(nextPath);
                        });
        return results;
    }

    private List<ApiCallMethod> convertApiCallsMapToApiCallsList(Map apiCalls) {
        List<ApiCallMethod> results = new ArrayList<>();
        apiCalls
                .keySet()
                .forEach(
                        ac -> {
                            Map call = (Map) apiCalls.get(ac);
                            ApiCallMethod nextApiCallMethod = new ApiCallMethod();

                            nextApiCallMethod.restVerb = (String) ac;

                            nextApiCallMethod.operationId = (String) call.get("operationId");

                            nextApiCallMethod.controller = (String) ((JSONArray) call.get("tags")).get(0);

                            nextApiCallMethod.summary = (String) call.get("summary");

                            nextApiCallMethod.endpointName =
                                    generateEndpointConstantName(nextApiCallMethod.operationId);

                            nextApiCallMethod.methodName =
                                    generateMethodName(nextApiCallMethod.operationId, nextApiCallMethod.controller);

                            nextApiCallMethod.consumes = (String) ((JSONArray) call.get("consumes")).get(0);

                            nextApiCallMethod.parameters =
                                    JsonHelper.deserializeJsonAsList(
                                            call.get("parameters").toString(), new TypeReference<>() {});

                            nextApiCallMethod.methodArguments =
                                    generateMethodArguments(nextApiCallMethod.parameters);

                            nextApiCallMethod.restParamMethodChain =
                                    generateRestParamsMethodChain(nextApiCallMethod.parameters);

                            results.add(nextApiCallMethod);
                        });
        return results;
    }

    public String generatePackageName(String path) {
        return path.replaceAll("[^A-z]+", "").toLowerCase();
    }

    private List<String> generateImportPaths(List<Parameter> params) {
        List<String> result = new ArrayList<>();
        params
                .stream()
                .filter(p -> p.type != null)
                .forEach(
                        p -> {
                            if (p.type.equalsIgnoreCase("file")) {
                                result.add("java.io.File");
                                result.add("java.net.URLConnection");
                            }
                        });
        return result;
    }

    private String generateEndpointConstantValue(String path) {
        if (path.contains("filter")) {
            int filterIndex = path.lastIndexOf("{");
            path = path.substring(0, filterIndex);
        }
        return "\"" + path.replaceAll("^.*\\{entityId}/", "").replaceAll("\\{\\?.*", "") + "\"";
    }

    private String generateEndpointConstantName(String operationId) {
        StringBuilder result = new StringBuilder();
        String[] words = operationId.split("(?=\\p{Upper})");

        for (int i = 0; i < words.length; i++) {
            result.append(words[i].toUpperCase() + (i != words.length - 1 ? "_" : ""));
        }

        // find way to replace these with regex above that will not split the all caps Verbs
        return result
                .toString()
                .replaceAll("P_U_T", "PUT")
                .replaceAll("P_O_S_T", "POST")
                .replaceAll("G_E_T", "GET")
                .replaceAll("P_A_T_C_H", "PATCH")
                .replaceAll("D_E_L_E_T_E", "DELETE");
    }

    private String generateMethodName(String operationId, String controllerNameFromSwagger) {
        String controllerName =
                controllerNameFromSwagger.substring(0, controllerNameFromSwagger.length() - 15);
        String controllerNameInCamelCase =
                CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, controllerName);

        if (!operationId.toLowerCase().contains(controllerNameInCamelCase.toLowerCase())) {
            // change casing of first letter of operationId (get --> Get)
            operationId = operationId.substring(0, 1).toUpperCase() + operationId.substring(1);
            // add back in the controller to the beginning of the method
            operationId = controllerNameInCamelCase + operationId;
        }

        return operationId
                .replaceAll("GET", "Get")
                .replaceAll("PUT", "Put")
                .replaceAll("POST", "Post")
                .replaceAll("DELETE", "Delete")
                .replaceAll("PATCH", "Patch")
                .replaceAll("_.$", "");
    }

    private String generateMethodArguments(List<Parameter> params) {
        StringBuilder result = new StringBuilder();
        params.forEach(
                param -> {
                    if (param.name.matches("^(?!productId$|entityId$|tenantId$).*")) {
                        if (param.type != null && param.type.equalsIgnoreCase("file")) {
                            result.append("File file, ");
                        } else if (param.in.equalsIgnoreCase("body")) {
                            result.append("Object body, ");
                        } else {
                            result.append("String " + param.name + ", ");
                        }
                    }
                });
        return result.toString().replaceAll(", $", "").replaceAll("\"", "");
    }

    private String generateClassName(String path) {
        String[] arr = path.toLowerCase().replaceAll("/", "").trim().split("[^A-z]+");
        return capitalizeStringArrToString(arr);
    }

    private String generateRestParamsMethodChain(List<Parameter> params) {
        StringBuilder result = new StringBuilder();
        params
                .stream()
                .filter(p -> p.name.matches("^(?!productId$|entityId$|tenantId$).*"))
                .forEach(
                        p -> {
                            // if body param, add body param method
                            if (p.in.equalsIgnoreCase("body")) {
                                result.append(BODY_TEMPLATE);
                            }
                            // handle queryParam
                            else if (p.in.equalsIgnoreCase("query")) {
                                result.append(String.format(QUERY_PARAM_TEMPLATE, p.name, p.name));
                            }
                            // handle formData
                            else if (p.in.equalsIgnoreCase("formData")) {
                                if (p.type.equalsIgnoreCase("file")) {
                                    result.append(String.format(FORM_DATA_TEMPLATE_, p.name, p.name, ", mime"));
                                } else {
                                    result.append(String.format(FORM_DATA_TEMPLATE_, p.name, p.name, ""));
                                }

                            }
                            // else treat param as a path param
                            else {
                                result.append(String.format(PATH_PARAM_TEMPLATE, p.name, p.name));
                            }
                        });
        return result.toString();
    }

    private String generateBeforeReturnStatementCode(ApiCallMethod apiCallMethod) {
        String result = "";
        if (apiCallMethod.consumes.contains("multipart")) {
            result = String.format(BEFORE_RETURN_STATEMENT, GET_FILE_MIME_TYPE);
        }
        return result;
    }
}
