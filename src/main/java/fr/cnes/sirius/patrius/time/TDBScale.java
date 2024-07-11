/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2682:18/05/2021: Echelle de temps TDB (diff. PATRIUS - SPICE) 
 * VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;


/**
 * Barycentric Dynamic Time.
 * <p>
 * Time used to take account of time dilation when calculating orbits of planets, asteroids, comets and interplanetary
 * spacecraft in the Solar system. It was based on a Dynamical time scale but was not well defined and not rigorously
 * correct as a relativistic time scale. It was subsequently deprecated in favour of Barycentric Coordinate Time (TCB),
 * but at the 2006 General Assembly of the International Astronomical Union TDB was rehabilitated by making it a
 * specific fixed linear transformation of TCB.
 * </p>
 * <p>
 * By convention, TDB = TT + 0.001658 sin(g) + 0.000014 sin(2g)seconds where g = 357.53 + 0.9856003 (JD - 2451545)
 * degrees.
 * </p>
 * 
 * @author Aude Privat
 */
public class TDBScale implements TimeScale {
    
    static {
        // Default model
        setModel(new TDBDefaultModel());
    };

    /** TDB underlying model. */
    private static TDBModel model;

    /**
     * Package private constructor for the factory.
     */
    TDBScale() {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        return model.offsetFromTAI(date);
    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        final AbsoluteDate reference = new AbsoluteDate(date, time, TimeScalesFactory.getTAI());
        double offset = 0;
        for (int i = 0; i < 3; i++) {
            offset = -this.offsetFromTAI(reference.shiftedBy(offset));
        }
        return offset;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "TDB";
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * Set the TDB model.
     * @param modelIn the TDB model to set
     */
    public static void setModel(final TDBModel modelIn) {
        model = modelIn;
    }
}
