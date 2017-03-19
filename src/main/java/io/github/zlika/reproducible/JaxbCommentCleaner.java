/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.zlika.reproducible;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Fixes ObjectFactory java files generated by the JAXB xjc tool.
 * xjc (before JAXB 2.2.11) generates ObjectFactory.java files where the methods
 * are put in a non-predictable order (cf.https://java.net/jira/browse/JAXB-598).
 * This class sorts the methods so that the ObjectFactory.java file produced is always the same.
 */
final class JaxbCommentCleaner implements Stripper
{
    private static final String END_OF_METHOD = "    }";
    private final Charset charset;

    /**
     * Constructor.
     * @param charset the charset of the Java files to be processed.
     */
    public JaxbCommentCleaner(Charset charset)
    {
        this.charset = charset;
    }
    
    @Override
    public void strip(File in, File out) throws IOException
    {
        final String inContent = new String(Files.readAllBytes(in.toPath()), charset);
        // If this is not an ObjectFactory file generated by xjc,
        // just copy the file unchanged
        if (!checkIsXjcFile(inContent))
        {
            Files.copy(in.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        
        final StringBuilder builder = new StringBuilder();
        builder.append(removeComments(inContent));
        Files.write(out.toPath(), builder.toString().getBytes(charset));
    }

    private String removeComments(String code) {
        StringBuilder newCode = new StringBuilder();
        try (StringReader sr = new StringReader(code)) {
            boolean inBlockComment = false;
            boolean inLineComment = false;
            boolean out = true;

            int prev = sr.read();
            int cur;
            for(cur = sr.read(); cur != -1; cur = sr.read()) {
                if(inBlockComment) {
                    if (prev == '*' && cur == '/') {
                        inBlockComment = false;
                        out = false;
                    }
                } else if (inLineComment) {
                    if (cur == '\r') { // start untested block
                        sr.mark(1);
                        int next = sr.read();
                        if (next != '\n') {
                            sr.reset();
                        }
                        inLineComment = false;
                        out = false; // end untested block
                    } else if (cur == '\n') {
                        inLineComment = false;
                        out = false;
                    }
                } else {
                    if (prev == '/' && cur == '*') {
                        sr.mark(1); // start untested block
                        int next = sr.read();
                        if (next != '*') {
                            inBlockComment = true; // tested line (without rest of block)
                        }
                        sr.reset(); // end untested block
                    } else if (prev == '/' && cur == '/') {
                        inLineComment = true;
                    } else if (out){
                        newCode.append((char)prev);
                    } else {
                        out = true;
                    }
                }
                prev = cur;
            }
            if (prev != -1 && out && !inLineComment) {
                newCode.append((char)prev);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return newCode.toString();
    }

    private boolean checkIsXjcFile(String content)
    {
        return content.contains("JavaTM Architecture for XML Binding");
    }
}