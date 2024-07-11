/**
 * 
 * Copyright 2011-2022 CNES
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 
 * @history 05/03/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.propagation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RefReader {

    SortedMap<Double, Double> map = new TreeMap<Double, Double>();

    /** regERxp for real number */
    private static final String REAL = "[-]?\\d+\\.\\d+[eE]?-?\\d*+";

    /** regExp for line */
    private static final String LINE = "^\\s+(" + REAL + ")\\s+(" + REAL + ")";

    RefReader() {
    }

    SortedSet<Double> getDates() {
        final SortedSet<Double> set = new TreeSet<Double>();
        set.addAll(this.map.keySet());
        return set;
    }

    double getValue(final double t) {
        return this.map.get(t);
    }

    public void readData(final String filename) throws URISyntaxException, IOException {
        final String path = ParameterModelReader.class.getClassLoader().getResource(filename).toURI().getPath();
        final File file = new File(path);

        final FileInputStream fis = new FileInputStream(file);
        final BufferedReader bf = new BufferedReader(new InputStreamReader(fis));

        // # Partie polynomiale: A + B*t (m)
        // 0.0000000000000000 0.0011426867303076430
        final Pattern pattern = Pattern.compile(LINE);

        // skip header
        bf.readLine();

        for (String line = bf.readLine(); line != null; line = bf.readLine()) {
            final Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                throw new RuntimeException();
            }

            final double t = Double.parseDouble(matcher.group(1));
            final double v = Double.parseDouble(matcher.group(2));

            this.map.put(t, v);
        }
    }
}
