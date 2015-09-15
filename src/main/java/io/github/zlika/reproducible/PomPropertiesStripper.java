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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Strips non-reproducible data from Maven pom properties files.
 * This stripper removes all comment lines (as some of them can contain date/time).
 */
final class PomPropertiesStripper implements Stripper
{
    @Override
    public void strip(InputStream is, OutputStream os) throws IOException
    {
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        reader.lines().filter(s -> !s.startsWith("#"))
                        .forEach(s -> 
                        {
                            try
                            {
                                writer.write(s);
                                writer.newLine();
                            }
                            catch (IOException e)
                            {
                            }
                        });
        writer.flush();
    }
}