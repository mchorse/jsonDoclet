package mchorse.utils.doclets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Doclet that processes JavaDocs' data to one JSON file.
 *
 * Loosely based off this project: https://github.com/tantaman/jsonDoclet
 */
public class JsonDoclet
{
    public static boolean start(RootDoc root)
    {
        JsonObject object = new JsonObject();

        object.add("classes", compileClasses(root));
        object.add("packages", compilePackages(root));
        writeToFile(object);

        return true;
    }

    private static JsonElement compileClasses(RootDoc root)
    {
        JsonArray array = new JsonArray();

        for (ClassDoc classDoc : root.classes())
        {
            if (classDoc.methods().length > 0)
            {
                JsonObject clazz = new JsonObject();

                compileClass(clazz, classDoc);

                array.add(clazz);
            }
        }

        return array;
    }

    private static JsonElement compilePackages(RootDoc root)
    {
        JsonArray packages = new JsonArray();

        for (PackageDoc packageDoc : root.specifiedPackages())
        {
            JsonObject packageObject = new JsonObject();

            packageObject.addProperty("name", packageDoc.name());

            if (!packageDoc.commentText().trim().isEmpty())
            {
                packageObject.addProperty("doc", packageDoc.commentText());
            }

            packages.add(packageObject);
        }

        return packages;
    }

    private static void compileClass(JsonObject clazz, ClassDoc classDoc)
    {
        clazz.addProperty("name", classDoc.qualifiedTypeName());
        clazz.addProperty("doc", classDoc.commentText());
        clazz.add("methods", compileMethods(classDoc.methods()));
    }

    private static JsonElement compileMethods(MethodDoc[] methods)
    {
        JsonArray array = new JsonArray();

        for (MethodDoc methodDoc : methods)
        {
            JsonObject method = new JsonObject();

            compileMethod(method, methodDoc);

            array.add(method);
        }

        return array;
    }

    private static void compileMethod(JsonObject method, MethodDoc methodDoc)
    {
        method.addProperty("name", methodDoc.name());
        method.addProperty("doc", methodDoc.commentText());
        method.add("returns", compileReturn(methodDoc));
        method.add("arguments", compileArguments(methodDoc));
    }

    private static JsonElement compileReturn(MethodDoc methodDoc)
    {
        Tag[] returnTag = methodDoc.tags("return");
        JsonObject returnObject = new JsonObject();

        returnObject.addProperty("type", methodDoc.returnType().qualifiedTypeName());

        if (returnTag.length > 0)
        {
            returnObject.addProperty("doc", returnTag[0].text());
        }

        return returnObject;
    }

    private static JsonElement compileArguments(MethodDoc methodDoc)
    {
        Map<String, String> argumentTags = compileArgumentComments(methodDoc);

        JsonArray array = new JsonArray();

        for (Parameter argumentDoc : methodDoc.parameters())
        {
            JsonObject argument = new JsonObject();

            compileArgument(argument, argumentDoc, argumentTags, methodDoc);

            array.add(argument);
        }

        return array;
    }

    private static Map<String, String> compileArgumentComments(MethodDoc methodDoc)
    {
        Map<String, String> argumentTags = new HashMap<String, String>();

        for (Tag tag : methodDoc.tags("param"))
        {
            String text = tag.text();

            int space = text.indexOf(' ');
            String name = text.substring(0, space).trim();
            String comment = text.substring(space).trim();

            argumentTags.put(name, comment);
        }

        return argumentTags;
    }

    private static void compileArgument(JsonObject argument, Parameter argumentDoc, Map<String, String> argumentTags, MethodDoc methodDoc)
    {
        String comment = argumentTags.get(argumentDoc.name());

        argument.addProperty("name", argumentDoc.name());
        argument.addProperty("type", argumentDoc.type().qualifiedTypeName());

        if (comment != null)
        {
            argument.addProperty("doc", comment);
        }
    }

    private static void writeToFile(JsonElement array)
    {
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File("./docs.json")));

            writer.write(jsonToPretty(array));
            writer.close();
        }
        catch (Exception e)
        {}
    }

    public static String jsonToPretty(JsonElement element)
    {
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        jsonWriter.setIndent("    ");
        gson.toJson(element, jsonWriter);

        return writer.toString();
    }
}