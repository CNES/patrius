/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history creation 21/09/2012
 */
package fr.cnes.sirius.patrius.signalpropagation.ionosphere;

/**
 * Container for the USK data read from the NEWUSK file. This data is used
 * in ionospheric correction computation with the Bent model.
 * The inner attributes are not well protected relative to encapsulation,
 * but since this class is only a helper class with a short lifetime,
 * it's not important.
 * 
 * @concurrency immutable
 * 
 * @see USKProvider
 * 
 * @author trapiert
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
@SuppressWarnings("PMD.MethodReturnsInternalArray")
final class USKData {

    /** iuf data */
    private final int[] iufIn;
    /** ium data */
    private final double[][] ufIn;
    /** uf data */
    private final int[] iumIn;
    /** um data */
    private final double[][] umIn;

    /**
     * Constructor for container.
     * 
     * @param iuf
     *        iuf data
     * @param ium
     *        ium data
     * @param uf
     *        uf data
     * @param um
     *        um data
     */
    public USKData(final int[] iuf, final int[] ium,
        final double[][] uf, final double[][] um) {
        this.iufIn = iuf;
        this.iumIn = ium;
        this.ufIn = uf;
        this.umIn = um;
    }

    /**
     * @return iuf
     */
    public int[] getIuf() {
        return this.iufIn;
    }

    /**
     * @return uf
     */
    public double[][] getUf() {
        return this.ufIn;
    }

    /**
     * @return ium
     */
    public int[] getIum() {
        return this.iumIn;
    }

    /**
     * @return um
     */
    public double[][] getUm() {
        return this.umIn;
    }
}
