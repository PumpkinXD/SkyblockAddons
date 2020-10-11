package codes.biscuit.skyblockaddons.core;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import com.google.gson.JsonObject;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translations {

    private static Pattern VARIABLE_PATTERN = Pattern.compile("%[A-Za-z]+%");

    public static String getMessage(String path, Object... variables) {
        try {
            // Split the member name from the rest of the path.
            int lastIndex = path.lastIndexOf('.');
            String memberName = path.substring(lastIndex + 1);
            String parentPath = path.substring(0, lastIndex);

            // Get the final json object.
            String[] pathSplit = parentPath.split(Pattern.quote("."));
            return getMessageInternal(memberName, pathSplit, variables);

        } catch (Throwable ex) {
            return path; // In case of fire...
        }
    }

    public static String getMessageInternal(String memberName, String[] path, Object... variables) {
        try {
            SkyblockAddons main = SkyblockAddons.getInstance();

            JsonObject jsonObject = main.getConfigValues().getLanguageConfig();
            for (String pathPart : path) {
                if (!pathPart.equals("")) {
                    jsonObject = jsonObject.getAsJsonObject(pathPart);
                }
            }

            // Get the string from the final json object.
            String text = jsonObject.get(memberName).getAsString();

            // Iterate through the string and replace any variables.
            Matcher matcher = VARIABLE_PATTERN.matcher(text);
            Deque<Object> variablesDeque = new ArrayDeque<>(Arrays.asList(variables));
            while (matcher.find()) {
                // No variables left... abort!
                if (variablesDeque.isEmpty()) {
                    break;
                }

                // Replace a variable and re-make the matcher.
                text = matcher.replaceFirst(variablesDeque.removeFirst().toString());
                matcher = VARIABLE_PATTERN.matcher(text);
            }

            // Handle RTL text...
            if ((main.getConfigValues().getLanguage() == Language.HEBREW || main.getConfigValues().getLanguage() == Language.ARABIC) &&
                    !Minecraft.getMinecraft().fontRendererObj.getBidiFlag()) {
                return bidiReorder(text);
            }
            return text;

        } catch (Throwable ex) {
            return memberName; // In case of fire...
        }
    }

    private static String bidiReorder(String text) {
        try {
            Bidi bidi = new Bidi((new ArabicShaping(ArabicShaping.LETTERS_SHAPE)).shape(text), Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
            bidi.setReorderingMode(Bidi.REORDER_DEFAULT);
            return bidi.writeReordered(Bidi.DO_MIRRORING);
        } catch (ArabicShapingException var3) {
            return text;
        }
    }
}
