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
 * VERSION:4.9:FA:FA-3144:10/05/2022:[PATRIUS] Classe TempDirectory en double 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2366:27/05/2020:Le scope de la dependance a JUnit devrait être « test » 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.tools.validationTool;

import java.io.File;

/**
 * <p>
 * This class offers a temporary directory (complete name) to write results in output files
 * 
 * @author clauded
 * 
 * @version $Id: TempDirectory.java 17578 2017-05-10 12:20:20Z bignon $
 * 
 * @since 2.1
 * 
 */
public final class TemporaryDirectory {

    /**
     * default private constructor
     */
    private TemporaryDirectory() {
    }

    /**
     * 
     * Static method to recover the temporary directory.
     * In maven environment writes in "sMavenDir".
     * In other environment, in "java.io.tmpdir" with possibly "additionnalDirectory".
     * 
     * @param sMavenDir key of maven directory (optional)
     * @param additionnalDirectory additional directory (optional)
     * @return the temporary directory
     * 
     * @since 2.1
     */
    public static String getTemporaryDirectory(final String sMavenDir, final String additionnalDirectory) {
        final String mavenDir;
        final String tempDirectory;

        if (sMavenDir != null) {
            mavenDir = System.getProperty(sMavenDir);
        } else {
            mavenDir = null;
        }

        if (mavenDir != null) {
            // We assume we are in a mavenized context
            // The output is to be written to the directory
            // pointed by pdb.misc.results
            // if (additionnalDirectory != null) {
            // tempDirectory = mavenDir + File.separator + additionnalDirectory;
            // } else {
            tempDirectory = mavenDir;
            // }
        } else {
            // We assume we are not in a mavenized context
            // We use a default output directory
            final String tmpDir = System.getProperty("java.io.tmpdir");
            if (additionnalDirectory != null) {
                tempDirectory = tmpDir + File.separator + additionnalDirectory;
            } else {
                tempDirectory = tmpDir;
            }
        }
        return tempDirectory;
    }
}
