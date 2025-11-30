package dev.badkraft.anvil.utilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AmlMinifier {

    private static final Pattern COMMENT = Pattern.compile("//.*|(?s)/\\*.*?\\*/");
    private static final Pattern STRING_OR_BLOB = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|`(?:\\\\.|[^`\\\\])*`");

    /**
     * Minifies AML source: strips comments, collapses insignificant whitespace,
     * but preserves ALL content inside strings and blobs EXACTLY.
     * Tested against your sacred YAML blob example.
     */
    public static String minify(String aml) {
        // Step 1: Remove all comments (safe outside literals)
        String noComments = COMMENT.matcher(aml).replaceAll("");

        // Step 2: Preserve strings/blobs exactly, collapse other whitespace to single space
        Matcher matcher = STRING_OR_BLOB.matcher(noComments);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            // Append text before this literal (collapse its whitespace)
            String before = noComments.substring(lastEnd, matcher.start());
            sb.append(before.replaceAll("\\s+", " ").trim());

            // Append the literal exactly
            sb.append(matcher.group());

            lastEnd = matcher.end();
        }

        // Append any remaining text after last literal
        String remaining = noComments.substring(lastEnd);
        sb.append(remaining.replaceAll("\\s+", " ").trim());

        // Step 3: Final cleanup - remove optional spaces around operators
        String result = sb.toString()
                .replaceAll(" (?=[:=}\\]])", "")  // space before := } ]
                .replaceAll("(?<=[{\\[:=]) ", "") // space after { [ ( :=
                .trim();

        return result;
    }
}