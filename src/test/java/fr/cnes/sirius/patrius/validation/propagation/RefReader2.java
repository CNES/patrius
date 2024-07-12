/**
 * 
 * Copyright 2011-2022 CNES
 *
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:266:07/05/2015:add various centered analytical models
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

class RefReader2 {

    SortedMap<Double, double[]> map = new TreeMap<>();

    RefReader2() {
    }

    SortedSet<Double> getDates() {
        final SortedSet<Double> set = new TreeSet<>();
        set.addAll(this.map.keySet());
        return set;
    }

    double[] getValue(final double t) {
        return this.map.get(t);
    }

    public void readData(final String filename) throws URISyntaxException, IOException {

        // Load file
        final String path = ParameterModelReader.class.getClassLoader().getResource(filename).toURI().getPath();
        final File file = new File(path);

        final FileInputStream fis = new FileInputStream(file);
        final BufferedReader bf = new BufferedReader(new InputStreamReader(fis));

        // Skip header
        for (int i = 0; i < 12; i++) {
            bf.readLine();
        }

        for (String line = bf.readLine(); line != null; line = bf.readLine()) {
            final String[] lineString = line.split(" ");

            final double t = Double.parseDouble(lineString[0]);
            final double[] v = { Double.parseDouble(lineString[1]), Double.parseDouble(lineString[2]),
                Double.parseDouble(lineString[3]),
                Double.parseDouble(lineString[4]), Double.parseDouble(lineString[5]),
                Double.parseDouble(lineString[6]) };

            this.map.put(t, v);
        }

        bf.close();
    }
}
