# Overview
Showcase of ClassBuilder utility I developed for my company to automate boiler plate type files. It was originally designed to generate essential files for our back-end automation and gain the attention of our head architect for its potential benefit not only to our automation repositories but to other boiler plate code that could be automated throughout the application. They decided on an established 3rd party code generating tool instead as this was still in its infancy, had much room for improvement/enhancement and was designed by a junior developer aka me, but I am proud to have designed and demonstrate its concept in use. This is purely to show some of my talents as a programmer but will consider making an full example project to show it in action.

# How it works
This was designed to generate "Service Wrapper" classes from our swagger api documented micro services in the form of the Screenplay pattern of Serenity BDD using Rest Assured. The ClassBuilder class is very generic in nature and can easily be expanded to generate a varity of different files in different programming languages, but in its current form is designed to generate java files in the user's desired shape and location. The ServiceWrapperGenerator is more specifically built to work with swagger documented data. It's responsible for executing the genration and translating the swagger data into files using an instance of ClassBuilder.

## ServiceWrapperGenerator.java
This class is designed to contain the main() method for execution and the main logic for orchestrating the translation of swagger data to actual java classes, leveraging the ClassBuilder class. This class is meant to be replaced with other similar classes to met the needs of the user e.g. a similar class designed to generate models based off swagger documentation. The main method is executed using gradle with the application plugin.

```
plugins{
  id 'application'
}

application{
    mainClassName = 'com.company.test.utilities.classgenerator.ServiceWrapperGenerator'
}

```
The user would run the gradle command ```gradle run --args="all"``` with the generator class expecting either the argument...

"all" which will generate the classes for all "services" declared in the generator class
```
private static final String[] SERVICES =
    {
        "serviceA",
        "serviceB",
        "serviceC"
    };
```

or one or many services the users has documented with swagger.

After the target services are determined, for each service, the generator class instantiates an instance of ClassBuilder with it's designated package declaration and classname.

```ClassBuilder classBuilder = new ClassBuilder(packagePath, className);```

The generator then uses a combination of simple JSONObjects, inner classes, parsing logic and the ClassBuilder to shape the swagger data into Rest Assured api calls while the ClassBuiler itself puts it all together and generates the file(s) in the desired location.


## ClassBuilder.java
This is a generic ClassBuilder java class that is specifically designed to generate java classes but can easily be expanded on to include other programming languages. You first Instantiate an instance of ClassBuilder with a designated package declaration and classname.

```ClassBuilder classBuilder = new ClassBuilder(packagePath, className);```

The ClassBuilder has an assortment of methods for adding import statements, fields and methods stored in Maps and formatted with Strings. It has both methods for adding detailed members such as methods with your choice of access level, return type, etc. as well as wrapper methods for common members such as private constant fields. You can also remove It also has a bit of logic to ensure no duplication for such things as import statements and variable names. You can also remove added members and import s

Example of custom tailored method:
```
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
```

Once you have added all desired import statements, variables and methods you simply call the generateClassAt() method, passing the desired path to generate the desired files.
``` generateClassAt(path)```

# Ideas for enhancement
- Make ClassBuilder a singleton.
- Make ClassBuilder a more generic parent class that children classes could extend to cover other types of files and other programming languages.
- Add more features to ClassBuilder for things like constructors and annotations.
- Make ClassBuilder more robust on how it manages import statements and members added.
- Make ServiceWrapperGenerator class a more generic parent class that is less specific to working with Swagger.
- Use a more robust method of execution than application plugin with gradle.
- Refactor generator inner classes to be actual classes and use a more robust method for parsing json.
