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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2095:15/05/2019:[PATRIUS] preparation au deploiement sur les depots centraux maven
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.tools.parallel;

/**
 * ResultOne.
 * 
 * @author cardosop
 * 
 * @version $Id: ResultOne.java 17915 2017-09-11 12:35:44Z bignon $
 * 
 */
public class ResultOne implements ParallelResult {

    // Four fields
    /** */
    private final double one;
    /** */
    private final double two;
    /** */
    private final double three;
    /** */
    private final double four;

    public ResultOne(final double o, final double tw, final double th,
        final double f) {
        this.one = o;
        this.two = tw;
        this.three = th;
        this.four = f;
    }

    @Override
    public double[][] getDataAsArray() {
        final double[][] rez = { { this.one, this.two }, { this.three, this.four } };
        return rez;
    }

    @Override
    public boolean resultEquals(final ParallelResult other) {
        if (other == null) {
            return false;
        }
        boolean rez = false;
        if (this.getClass().isInstance(other)) {
            final double[][] doubs = other.getDataAsArray();
            rez =
                (doubs[0][0] == this.one && doubs[0][1] == this.two && doubs[1][0] == this.three && doubs[1][1] == this.four);
        }
        return rez;
    }

}
