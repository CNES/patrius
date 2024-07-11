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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2095:15/05/2019:[PATRIUS] preparation au deploiement sur les depots centraux maven
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.tools.parallel;

/**
 * ResultTwo.
 * 
 * @author cardosop
 * 
 * @version $Id: ResultTwo.java 17915 2017-09-11 12:35:44Z bignon $
 * 
 */
public class ResultTwo implements ParallelResult {

    private final double razult;

    public ResultTwo(final double r) {
        this.razult = r;
    }

    @Override
    public double[][] getDataAsArray() {
        final double[][] rez = { { this.razult } };
        return rez;
    }

    public double getRazult() {
        return this.razult;
    }

    @Override
    public boolean resultEquals(final ParallelResult other) {
        if (other == null) {
            return false;
        }
        boolean rez = false;
        if (this.getClass().isInstance(other)) {
            final double otherRez = ((ResultTwo) other).getRazult();
            rez = (this.razult == otherRez);
        }
        return rez;
    }

}
